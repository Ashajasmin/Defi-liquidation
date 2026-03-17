#!/usr/bin/env python3
"""
TradingView Technical Indicators for ACTUS DeFi Liquidation Models
===================================================================
Computes RSI, MACD, Bollinger Bands, ATR, VWAP, Volume Profile
from CoinGecko OHLCV data to enrich behavioral model decisions.

NOTE: TradingView does not offer a public REST API. This script
      computes the same indicators locally using CoinGecko OHLCV data.
      For real-time TradingView data, use:
        - TradingView Charting Library (requires license)
        - ta-lib Python package (local computation)
        - CryptoCompare OHLCV as data source

Indicators computed:
  1. RSI (Relative Strength Index) - overbought/oversold (70/30)
  2. MACD (Moving Average Convergence Divergence) - trend + momentum
  3. Bollinger Bands - volatility envelope (+/-2s from 20-day SMA)
  4. ATR (Average True Range) - absolute volatility measurement
  5. VWAP (Volume Weighted Average Price) - fair value reference
  6. Annualized Volatility - from log returns (CollateralVelocityModel input)

Each indicator maps to ACTUS ReferenceIndex format for behavioral model consumption.

Usage:
    python tradingview_indicators.py --coin ethereum --days 120 --output indicators.json
    python tradingview_indicators.py --coin ethereum --days 120 --post-to http://localhost:8082
"""

import argparse
import json
import math
import os
import sys
import time
from datetime import datetime, timezone, timedelta
from typing import Dict, List, Tuple

try:
    import requests
except ImportError:
    print("ERROR: pip install requests")
    sys.exit(1)

# =========================================================================
# DATA SOURCE: CoinGecko OHLCV
# =========================================================================
def fetch_ohlcv(coin_id: str, days: int) -> List[Dict]:
    base_url = "https://api.coingecko.com/api/v3"
    api_key = os.environ.get("COINGECKO_API_KEY")
    if api_key:
        base_url = "https://pro-api.coingecko.com/api/v3"
    url = f"{base_url}/coins/{coin_id}/market_chart"
    params = {"vs_currency": "usd", "days": days, "interval": "daily"}
    headers = {"Accept": "application/json"}
    if api_key:
        headers["x-cg-pro-api-key"] = api_key
    resp = requests.get(url, params=params, headers=headers, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    prices = data.get("prices", [])
    volumes = data.get("total_volumes", [])
    records = []
    for i, (ts_ms, price) in enumerate(prices):
        dt = datetime.fromtimestamp(ts_ms / 1000, tz=timezone.utc)
        vol = volumes[i][1] if i < len(volumes) else 0
        records.append({"time": dt.strftime("%Y-%m-%dT%H:%M:%S"), "close": price, "volume": vol})
    return records


def fetch_ohlc_candles(coin_id: str, days: int) -> List[Dict]:
    base_url = "https://api.coingecko.com/api/v3"
    url = f"{base_url}/coins/{coin_id}/ohlc"
    params = {"vs_currency": "usd", "days": days}
    resp = requests.get(url, params=params, timeout=30)
    resp.raise_for_status()
    candles = resp.json()
    records = []
    for ts_ms, o, h, l, c in candles:
        dt = datetime.fromtimestamp(ts_ms / 1000, tz=timezone.utc)
        records.append({"time": dt.strftime("%Y-%m-%dT%H:%M:%S"), "open": o, "high": h, "low": l, "close": c})
    return records


# =========================================================================
# TECHNICAL INDICATORS
# =========================================================================
def compute_rsi(closes: List[float], period: int = 14) -> List[float]:
    if len(closes) < period + 1:
        return [50.0] * len(closes)
    deltas = [closes[i] - closes[i-1] for i in range(1, len(closes))]
    gains = [max(d, 0) for d in deltas]
    losses = [abs(min(d, 0)) for d in deltas]
    rsi_values = [50.0]
    avg_gain = sum(gains[:period]) / period
    avg_loss = sum(losses[:period]) / period
    for i in range(period):
        rsi_values.append(50.0)
    for i in range(period, len(deltas)):
        avg_gain = (avg_gain * (period - 1) + gains[i]) / period
        avg_loss = (avg_loss * (period - 1) + losses[i]) / period
        if avg_loss == 0:
            rsi = 100.0
        else:
            rs = avg_gain / avg_loss
            rsi = 100.0 - (100.0 / (1.0 + rs))
        rsi_values.append(round(rsi, 2))
    return rsi_values


def compute_macd(closes: List[float], fast: int = 12, slow: int = 26, signal: int = 9) -> Tuple[List[float], List[float], List[float]]:
    def ema(data, period):
        k = 2 / (period + 1)
        result = [data[0]]
        for i in range(1, len(data)):
            result.append(data[i] * k + result[-1] * (1 - k))
        return result
    ema_fast = ema(closes, fast)
    ema_slow = ema(closes, slow)
    macd_line = [round(f - s, 4) for f, s in zip(ema_fast, ema_slow)]
    signal_line = ema(macd_line, signal)
    histogram = [round(m - s, 4) for m, s in zip(macd_line, signal_line)]
    return macd_line, [round(s, 4) for s in signal_line], histogram


def compute_bollinger_bands(closes: List[float], period: int = 20, num_std: float = 2.0) -> Tuple[List[float], List[float], List[float]]:
    sma, upper, lower = [], [], []
    for i in range(len(closes)):
        if i < period - 1:
            sma.append(closes[i]); upper.append(closes[i]); lower.append(closes[i])
        else:
            window = closes[i - period + 1: i + 1]
            mean = sum(window) / period
            variance = sum((x - mean) ** 2 for x in window) / period
            std = math.sqrt(variance)
            sma.append(round(mean, 2)); upper.append(round(mean + num_std * std, 2)); lower.append(round(mean - num_std * std, 2))
    return sma, upper, lower


def compute_atr(candles: List[Dict], period: int = 14) -> List[float]:
    if not candles or "high" not in candles[0]:
        return [0.0] * len(candles)
    trs = [candles[0]["high"] - candles[0]["low"]]
    for i in range(1, len(candles)):
        h, l, pc = candles[i]["high"], candles[i]["low"], candles[i-1]["close"]
        trs.append(max(h - l, abs(h - pc), abs(l - pc)))
    k = 2 / (period + 1)
    atr = [trs[0]]
    for i in range(1, len(trs)):
        atr.append(round(trs[i] * k + atr[-1] * (1 - k), 2))
    return atr


def compute_vwap(records: List[Dict]) -> List[float]:
    cum_pv, cum_vol, vwap = 0, 0, []
    for rec in records:
        price, vol = rec["close"], rec.get("volume", 0)
        cum_pv += price * vol; cum_vol += vol
        vwap.append(round(cum_pv / cum_vol, 2) if cum_vol > 0 else price)
    return vwap


def compute_annualized_volatility(closes: List[float], window: int = 30) -> List[float]:
    log_returns = [0.0]
    for i in range(1, len(closes)):
        if closes[i-1] > 0 and closes[i] > 0:
            log_returns.append(math.log(closes[i] / closes[i-1]))
        else:
            log_returns.append(0.0)
    ann_vol = []
    for i in range(len(closes)):
        w = log_returns[1:i+1] if i > 0 and i < window else log_returns[max(1,i-window+1):i+1]
        if len(w) > 1:
            mean = sum(w) / len(w)
            var = sum((r - mean)**2 for r in w) / (len(w) - 1)
            vol = math.sqrt(var) * math.sqrt(365) * 100
        else:
            vol = 0
        ann_vol.append(round(vol, 2))
    return ann_vol


# =========================================================================
# ACTUS CONVERSION
# =========================================================================
def indicator_to_reference_index(rfid: str, moc: str, times: List[str], values: List[float]) -> Dict:
    return {"riskFactorID": rfid, "marketObjectCode": moc, "base": 1.0,
            "data": [{"time": t, "value": v} for t, v in zip(times, values)]}


# =========================================================================
# MAIN
# =========================================================================
def main():
    parser = argparse.ArgumentParser(description="TradingView Indicators -> ACTUS")
    parser.add_argument("--coin", type=str, default="ethereum")
    parser.add_argument("--days", type=int, default=120, help="Days (extra for warmup)")
    parser.add_argument("--output", type=str, help="Output JSON file")
    parser.add_argument("--post-to", type=str, help="POST to ACTUS risk service")
    args = parser.parse_args()

    print(f"Fetching {args.coin} data ({args.days} days)...")
    records = fetch_ohlcv(args.coin, args.days)
    print(f"  Got {len(records)} daily records")
    if len(records) < 30:
        print("ERROR: Need at least 30 data points"); sys.exit(1)
    times = [r["time"] for r in records]
    closes = [r["close"] for r in records]
    time.sleep(2.5)
    candles = fetch_ohlc_candles(args.coin, args.days)
    print(f"  Got {len(candles)} OHLC candles")

    print("\nComputing indicators...")
    rsi = compute_rsi(closes); print(f"  RSI(14): current={rsi[-1]:.1f}")
    macd_line, signal_line, histogram = compute_macd(closes); print(f"  MACD(12,26,9): line={macd_line[-1]:.2f}")
    bb_sma, bb_upper, bb_lower = compute_bollinger_bands(closes)
    bb_width = [(u - l) / s if s > 0 else 0 for s, u, l in zip(bb_sma, bb_upper, bb_lower)]
    atr = compute_atr(candles) if candles else [0.0] * len(closes)
    atr_times = [c["time"] for c in candles] if candles else times
    vwap = compute_vwap(records); print(f"  VWAP: ${vwap[-1]:.2f}")
    ann_vol = compute_annualized_volatility(closes); print(f"  Ann Vol(30d): {ann_vol[-1]:.1f}%")

    results = [
        indicator_to_reference_index("ETH_RSI_01", "ETH_RSI", times, rsi),
        indicator_to_reference_index("ETH_MACD_01", "ETH_MACD", times, macd_line),
        indicator_to_reference_index("ETH_MACD_SIGNAL_01", "ETH_MACD_SIGNAL", times, signal_line),
        indicator_to_reference_index("ETH_MACD_HIST_01", "ETH_MACD_HISTOGRAM", times, histogram),
        indicator_to_reference_index("ETH_BB_UPPER_01", "ETH_BB_UPPER", times, bb_upper),
        indicator_to_reference_index("ETH_BB_LOWER_01", "ETH_BB_LOWER", times, bb_lower),
        indicator_to_reference_index("ETH_BB_WIDTH_01", "ETH_BB_WIDTH", times, [round(w, 4) for w in bb_width]),
        indicator_to_reference_index("ETH_ATR_01", "ETH_ATR", atr_times, atr),
        indicator_to_reference_index("ETH_VWAP_01", "ETH_VWAP", times, vwap),
        indicator_to_reference_index("ETH_ANN_VOL_01", "ETH_ANNUALIZED_VOL", times, ann_vol),
    ]
    print(f"\nGenerated {len(results)} ACTUS ReferenceIndexes")

    if args.output:
        with open(args.output, "w") as f: json.dump(results, f, indent=2)
        print(f"Saved to {args.output}")
    if args.post_to:
        for idx in results:
            url = f"{args.post_to}/addReferenceIndex"
            resp = requests.post(url, json=idx, timeout=30)
            print(f"  Posted {idx['riskFactorID']}: {resp.status_code}")
    if not args.output and not args.post_to:
        print("\n" + "=" * 70)
        print(f"{'Indicator':<25} {'MOC':<25} {'Latest Value':<15}")
        print("-" * 70)
        for idx in results:
            latest = idx["data"][-1]["value"] if idx["data"] else "N/A"
            print(f"{idx['riskFactorID']:<25} {idx['marketObjectCode']:<25} {latest:<15}")


if __name__ == "__main__":
    main()
