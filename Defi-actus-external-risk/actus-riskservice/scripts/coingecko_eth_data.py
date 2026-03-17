#!/usr/bin/env python3
"""
CoinGecko API Integration for ACTUS DeFi Liquidation Models
============================================================
Fetches ETH, BTC, wstETH, USDC price data and converts to ACTUS ReferenceIndex format.

API: https://api.coingecko.com/api/v3 (Free tier: 30 calls/min, 10K/month)
Trusted by: Metamask, Coinbase, Etherscan (32M+ tokens, 200+ networks)

Usage:
    python coingecko_eth_data.py --days 90 --output actus_reference_indexes.json
    python coingecko_eth_data.py --days 90 --post-to http://localhost:8082

Environment:
    COINGECKO_API_KEY=<your_key>  (optional, for Pro tier: 500 calls/min)
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime, timezone
from typing import Dict, List, Optional

try:
    import requests
except ImportError:
    print("ERROR: pip install requests")
    sys.exit(1)

# =========================================================================
# CONFIGURATION
# =========================================================================
BASE_URL = "https://api.coingecko.com/api/v3"
PRO_URL = "https://pro-api.coingecko.com/api/v3"

# CoinGecko coin IDs
COINS = {
    "ETH_USD": "ethereum",
    "BTC_USD": "bitcoin",
    "WSTETH_USD": "wrapped-steth",
    "USDC_USD": "usd-coin",
}

# ACTUS ReferenceIndex MOC mapping
MOC_MAP = {
    "ETH_USD": "ETH_USD",
    "BTC_USD": "BTC_USD",
    "WSTETH_USD": "WSTETH_USD",
    "USDC_USD": "USDC_USD",
}

RATE_LIMIT_DELAY = 2.5  # seconds between calls (free tier safe)


# =========================================================================
# API FUNCTIONS
# =========================================================================
def get_headers() -> Dict:
    """Build headers with optional API key."""
    headers = {"Accept": "application/json"}
    api_key = os.environ.get("COINGECKO_API_KEY")
    if api_key:
        headers["x-cg-pro-api-key"] = api_key
    return headers


def get_base_url() -> str:
    """Use Pro URL if API key is set."""
    return PRO_URL if os.environ.get("COINGECKO_API_KEY") else BASE_URL


def fetch_market_chart(coin_id: str, days: int, vs_currency: str = "usd") -> Dict:
    """
    GET /coins/{id}/market_chart
    Returns OHLCV data with daily granularity.
    
    Response: {prices: [[timestamp_ms, price], ...], market_caps: [...], total_volumes: [...]}
    """
    url = f"{get_base_url()}/coins/{coin_id}/market_chart"
    params = {
        "vs_currency": vs_currency,
        "days": days,
        "interval": "daily",
    }
    resp = requests.get(url, params=params, headers=get_headers(), timeout=30)
    resp.raise_for_status()
    return resp.json()


def fetch_current_price(coin_ids: List[str], vs_currencies: str = "usd") -> Dict:
    """
    GET /simple/price
    Quick current price lookup.
    
    Example: /simple/price?ids=ethereum,bitcoin&vs_currencies=usd
    """
    url = f"{get_base_url()}/simple/price"
    params = {
        "ids": ",".join(coin_ids),
        "vs_currencies": vs_currencies,
        "include_24hr_change": "true",
    }
    resp = requests.get(url, params=params, headers=get_headers(), timeout=30)
    resp.raise_for_status()
    return resp.json()


def fetch_tickers(coin_id: str) -> Dict:
    """
    GET /coins/{id}/tickers
    Order book depth proxy (bid/ask spread, volume).
    Used for MARKET_DEPTH reference index estimation.
    """
    url = f"{get_base_url()}/coins/{coin_id}/tickers"
    params = {"include_exchange_logo": "false", "depth": "true"}
    resp = requests.get(url, params=params, headers=get_headers(), timeout=30)
    resp.raise_for_status()
    return resp.json()


# =========================================================================
# CONVERSION TO ACTUS FORMAT
# =========================================================================
def to_actus_reference_index(
    risk_factor_id: str,
    market_object_code: str,
    prices: List[List],  # [[timestamp_ms, price], ...]
) -> Dict:
    """Convert CoinGecko price array to ACTUS ReferenceIndex JSON."""
    data = []
    for ts_ms, price in prices:
        dt = datetime.fromtimestamp(ts_ms / 1000, tz=timezone.utc)
        data.append({
            "time": dt.strftime("%Y-%m-%dT%H:%M:%S"),
            "value": round(price, 2),
        })
    return {
        "riskFactorID": risk_factor_id,
        "marketObjectCode": market_object_code,
        "base": 1.0,
        "data": data,
    }


# =========================================================================
# MAIN
# =========================================================================
def main():
    parser = argparse.ArgumentParser(description="CoinGecko → ACTUS ReferenceIndex")
    parser.add_argument("--days", type=int, default=90, help="Days of history")
    parser.add_argument("--output", type=str, help="Output JSON file")
    parser.add_argument("--post-to", type=str, help="POST to ACTUS risk service URL")
    parser.add_argument("--coins", nargs="+", default=list(COINS.keys()),
                        help="Coin keys: ETH_USD BTC_USD WSTETH_USD USDC_USD")
    args = parser.parse_args()

    results = []

    for coin_key in args.coins:
        if coin_key not in COINS:
            print(f"WARNING: Unknown coin key '{coin_key}', skipping")
            continue

        coin_id = COINS[coin_key]
        moc = MOC_MAP[coin_key]
        rfid = f"{coin_key}_01"

        print(f"Fetching {coin_key} ({coin_id}) for {args.days} days...")
        try:
            chart = fetch_market_chart(coin_id, args.days)
            prices = chart.get("prices", [])
            if not prices:
                print(f"  WARNING: No price data for {coin_key}")
                continue

            ref_index = to_actus_reference_index(rfid, moc, prices)
            results.append(ref_index)

            first_price = prices[0][1]
            last_price = prices[-1][1]
            min_price = min(p[1] for p in prices)
            max_price = max(p[1] for p in prices)
            print(f"  OK: {len(prices)} data points")
            print(f"  Range: ${min_price:.2f} - ${max_price:.2f}")
            print(f"  Start: ${first_price:.2f}, End: ${last_price:.2f}")
            print(f"  Change: {((last_price/first_price)-1)*100:.1f}%")

        except requests.exceptions.HTTPError as e:
            print(f"  ERROR: {e}")
        except Exception as e:
            print(f"  ERROR: {e}")

        time.sleep(RATE_LIMIT_DELAY)

    # Output
    if args.output:
        with open(args.output, "w") as f:
            json.dump(results, f, indent=2)
        print(f"\nSaved {len(results)} reference indexes to {args.output}")

    if args.post_to:
        print(f"\nPosting {len(results)} indexes to {args.post_to}...")
        for idx in results:
            url = f"{args.post_to}/addReferenceIndex"
            resp = requests.post(url, json=idx, timeout=30)
            status = "OK" if resp.status_code == 200 else f"FAIL ({resp.status_code})"
            print(f"  {idx['riskFactorID']}: {status}")

    if not args.output and not args.post_to:
        print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
