#!/usr/bin/env python3
"""
ACTUS DeFi Liquidation - Master Data Loader
=============================================
Orchestrates all API integrations to load live market data
into ACTUS risk service for DeFi liquidation behavioral models.

Loads:
  1. ETH/BTC/wstETH/USDC prices (CoinGecko)
  2. Gas prices (Etherscan / Infura)
  3. Pool LTV + Market Depth (DeFi Llama)
  4. ETF flows (farside.co.uk CSV or synthetic)
  5. Technical indicators RSI/MACD/BB/ATR/Vol (computed from prices)
  6. Invoice payment probability (synthetic or external)

Usage:
    # Load everything with synthetic fallbacks
    python load_all_data.py --days 90 --post-to http://localhost:8082

    # Load only prices (CoinGecko)
    python load_all_data.py --prices-only --days 90 --post-to http://localhost:8082

    # Save all to JSON for offline use
    python load_all_data.py --days 90 --output all_indexes.json

Environment variables (optional, for live API access):
    COINGECKO_API_KEY      - CoinGecko Pro (500 calls/min vs 30)
    ETHERSCAN_API_KEY      - Etherscan gas tracker
    INFURA_PROJECT_ID      - Infura RPC for EIP-1559 fee history
"""

import argparse
import json
import os
import sys
import subprocess

def run_script(script_name, args_list, description):
    print(f"\n{'='*60}")
    print(f"  {description}")
    print(f"{'='*60}")
    script_dir = os.path.dirname(os.path.abspath(__file__))
    script_path = os.path.join(script_dir, script_name)
    if not os.path.exists(script_path):
        print(f"  SKIP: {script_name} not found"); return False
    cmd = [sys.executable, script_path] + args_list
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        print(result.stdout)
        if result.stderr:
            print(f"  WARNINGS: {result.stderr[:500]}")
        return result.returncode == 0
    except subprocess.TimeoutExpired:
        print(f"  TIMEOUT: {script_name}"); return False
    except Exception as e:
        print(f"  ERROR: {e}"); return False


def main():
    parser = argparse.ArgumentParser(description="ACTUS DeFi - Master Data Loader")
    parser.add_argument("--days", type=int, default=90)
    parser.add_argument("--post-to", type=str, default="http://localhost:8082")
    parser.add_argument("--output", type=str, help="Save all to JSON")
    parser.add_argument("--prices-only", action="store_true")
    parser.add_argument("--skip-live", action="store_true", help="Use synthetic data only")
    args = parser.parse_args()

    target = args.post_to
    results = {"success": [], "failed": [], "skipped": []}

    print("ACTUS DeFi Liquidation - Master Data Loader")
    print(f"  Target: {target}")
    print(f"  Days: {args.days}")
    print(f"  Live APIs: {'disabled' if args.skip_live else 'enabled'}")

    # 1. CoinGecko prices
    ok = run_script("coingecko_eth_data.py",
        ["--days", str(args.days), "--post-to", target],
        "1/5 CoinGecko: ETH, BTC, wstETH, USDC prices")
    results["success" if ok else "failed"].append("CoinGecko prices")

    if args.prices_only:
        print("\n--prices-only: stopping after CoinGecko"); return

    # 2. Gas prices
    if not args.skip_live and os.environ.get("ETHERSCAN_API_KEY"):
        ok = run_script("etherscan_gas_data.py",
            ["--source", "etherscan", "--post-to", target],
            "2/5 Etherscan: Gas prices")
    elif not args.skip_live and os.environ.get("INFURA_PROJECT_ID"):
        ok = run_script("etherscan_gas_data.py",
            ["--source", "infura", "--blocks", "1000", "--post-to", target],
            "2/5 Infura: EIP-1559 fee history")
    else:
        print("\n  2/5 Gas: No API keys set, using simulation data defaults")
        ok = True
        results["skipped"].append("Gas (no API key)")
    if ok and "Gas" not in str(results["skipped"]):
        results["success"].append("Gas prices")

    # 3. DeFi Llama
    ok = run_script("defillama_data.py",
        ["--protocol", "aave", "--days", str(args.days), "--post-to", target],
        "3/5 DeFi Llama: Pool LTV + Market Depth (Aave TVL)")
    results["success" if ok else "failed"].append("DeFi Llama")

    # 4. ETF flows
    ok = run_script("etf_flow_data.py",
        ["--generate", "--days", str(args.days), "--post-to", target],
        "4/5 ETF Flows: Synthetic ETH ETF data (replace with farside.co.uk CSV)")
    results["success" if ok else "failed"].append("ETF flows")

    # 5. Technical indicators
    ok = run_script("tradingview_indicators.py",
        ["--coin", "ethereum", "--days", str(args.days + 30), "--post-to", target],
        "5/5 TradingView Indicators: RSI, MACD, BB, ATR, VWAP, Vol")
    results["success" if ok else "failed"].append("TradingView indicators")

    # Summary
    print("\n" + "=" * 60)
    print("  LOAD SUMMARY")
    print("=" * 60)
    print(f"  Success: {len(results['success'])} - {', '.join(results['success'])}")
    if results["failed"]:
        print(f"  Failed:  {len(results['failed'])} - {', '.join(results['failed'])}")
    if results["skipped"]:
        print(f"  Skipped: {len(results['skipped'])} - {', '.join(results['skipped'])}")
    print("=" * 60)


if __name__ == "__main__":
    main()
