#!/usr/bin/env python3
"""
DeFi Llama API Integration for ACTUS DeFi Liquidation Models
=============================================================
Fetches TVL, protocol utilization, pool data for CascadeProbabilityModel.

API: https://api.llama.fi (100% free, no API key required)
Documentation: https://defillama.com/docs/api

Used for:
  - POOL_AGG_LTV: Aggregate pool utilization/health (CascadeProbabilityModel)
  - MARKET_DEPTH proxy: TVL as liquidity indicator
  - Protocol monitoring: Aave, Compound, MakerDAO health

Usage:
    python defillama_data.py --protocol aave --output pool_data.json
    python defillama_data.py --all-lending --post-to http://localhost:8082
"""

import argparse
import json
import sys
import time
from datetime import datetime, timezone, timedelta
from typing import Dict, List, Optional

try:
    import requests
except ImportError:
    print("ERROR: pip install requests")
    sys.exit(1)

BASE_URL = "https://api.llama.fi"
YIELDS_URL = "https://yields.llama.fi"

# Major DeFi lending protocols for aggregate LTV estimation
LENDING_PROTOCOLS = ["aave", "compound-v3", "maker", "spark", "morpho"]


# =========================================================================
# API FUNCTIONS
# =========================================================================
def fetch_protocol_tvl(protocol: str) -> Dict:
    """
    GET /tvl/{protocol}
    Returns current TVL in USD.
    """
    resp = requests.get(f"{BASE_URL}/tvl/{protocol}", timeout=30)
    resp.raise_for_status()
    return {"protocol": protocol, "tvl_usd": resp.json()}


def fetch_protocol_detail(protocol: str) -> Dict:
    """
    GET /protocol/{protocol}
    Returns detailed protocol info with historical TVL.
    
    Response includes: tvl[], chainTvls{}, currentChainTvls{}, raises[], etc.
    """
    resp = requests.get(f"{BASE_URL}/protocol/{protocol}", timeout=30)
    resp.raise_for_status()
    return resp.json()


def fetch_historical_tvl(protocol: str, days: int = 90) -> List[Dict]:
    """Extract daily TVL history from protocol detail."""
    detail = fetch_protocol_detail(protocol)
    tvl_history = detail.get("tvl", [])
    
    cutoff = (datetime.now(timezone.utc) - timedelta(days=days)).timestamp()
    filtered = [
        {
            "time": datetime.fromtimestamp(p["date"], tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%S"),
            "tvl_usd": p["totalLiquidityUSD"],
        }
        for p in tvl_history
        if p["date"] >= cutoff
    ]
    return filtered


def fetch_yields_pools(chain: str = "Ethereum") -> List[Dict]:
    """
    GET https://yields.llama.fi/pools
    Returns yield/APY data for all pools.
    Filter by chain for Ethereum lending pools.
    
    Key fields: pool, chain, project, tvlUsd, apy, apyBase, apyReward,
                ilRisk, stablecoin, exposure, underlyingTokens
    """
    resp = requests.get(f"{YIELDS_URL}/pools", timeout=60)
    resp.raise_for_status()
    data = resp.json()
    
    pools = data.get("data", [])
    eth_pools = [p for p in pools if p.get("chain") == chain]
    return eth_pools


def fetch_stablecoins() -> List[Dict]:
    """
    GET /stablecoins
    Returns all stablecoins with market caps.
    Used for USDC/USDT peg monitoring context.
    """
    resp = requests.get(f"{BASE_URL.replace('api.llama.fi', 'stablecoins.llama.fi')}/stablecoins", timeout=30)
    resp.raise_for_status()
    return resp.json().get("peggedAssets", [])


def estimate_pool_agg_ltv(pools: List[Dict], protocol_filter: List[str] = None) -> float:
    """
    Estimate aggregate pool LTV from lending pool utilization.
    
    Logic: Sum of borrowed / Sum of supplied across major lending pools.
    This approximates the POOL_AGG_LTV index used by CascadeProbabilityModel.
    """
    if protocol_filter:
        pools = [p for p in pools if p.get("project") in protocol_filter]
    
    # Filter to lending pools with meaningful TVL
    lending_pools = [p for p in pools if p.get("tvlUsd", 0) > 1_000_000]
    
    total_tvl = sum(p.get("tvlUsd", 0) for p in lending_pools)
    
    # Utilization approximated from APY structure
    # Higher base APY relative to reward APY suggests higher utilization
    utilization_sum = 0
    weight_sum = 0
    for p in lending_pools:
        tvl = p.get("tvlUsd", 0)
        apy_base = p.get("apyBase", 0) or 0
        # Higher base APY -> higher utilization in lending markets
        # Rough mapping: 2% APY ~ 50% util, 5% ~ 70%, 10% ~ 85%
        if apy_base > 0:
            util_est = min(0.95, 0.30 + apy_base * 0.06)
        else:
            util_est = 0.50
        utilization_sum += util_est * tvl
        weight_sum += tvl
    
    return round(utilization_sum / weight_sum, 4) if weight_sum > 0 else 0.55


# =========================================================================
# ACTUS CONVERSION
# =========================================================================
def tvl_to_market_depth(tvl_history: List[Dict], scale_factor: float = 0.01) -> Dict:
    """
    Convert TVL history to MARKET_DEPTH reference index.
    Assumption: market depth ~ 1% of total lending TVL (order book proxy).
    """
    data = [
        {"time": p["time"], "value": round(p["tvl_usd"] * scale_factor / 1e6, 2)}
        for p in tvl_history
    ]
    return {
        "riskFactorID": "MARKET_DEPTH_01",
        "marketObjectCode": "MARKET_DEPTH",
        "base": 1.0,
        "data": data,
    }


def utilization_to_pool_ltv(tvl_history: List[Dict], base_util: float = 0.55) -> Dict:
    """
    Convert TVL history to POOL_AGG_LTV reference index.
    As TVL drops, utilization/LTV tends to rise (denominator shrinks, loans outstanding same).
    """
    if not tvl_history:
        return {"riskFactorID": "POOL_AGG_LTV_01", "marketObjectCode": "POOL_AGG_LTV",
                "base": 1.0, "data": []}
    
    max_tvl = max(p["tvl_usd"] for p in tvl_history)
    data = []
    for p in tvl_history:
        # As TVL drops, effective pool LTV rises
        tvl_ratio = p["tvl_usd"] / max_tvl if max_tvl > 0 else 1.0
        agg_ltv = base_util / tvl_ratio  # inverse relationship
        agg_ltv = min(max(agg_ltv, 0.40), 0.95)
        data.append({"time": p["time"], "value": round(agg_ltv, 4)})
    
    return {
        "riskFactorID": "POOL_AGG_LTV_01",
        "marketObjectCode": "POOL_AGG_LTV",
        "base": 1.0,
        "data": data,
    }


# =========================================================================
# MAIN
# =========================================================================
def main():
    parser = argparse.ArgumentParser(description="DeFi Llama -> ACTUS Reference Indexes")
    parser.add_argument("--protocol", type=str, default="aave", help="Protocol slug")
    parser.add_argument("--days", type=int, default=90, help="Days of history")
    parser.add_argument("--all-lending", action="store_true", help="Aggregate all lending")
    parser.add_argument("--output", type=str, help="Output JSON file")
    parser.add_argument("--post-to", type=str, help="POST to ACTUS risk service")
    args = parser.parse_args()

    results = []

    # Historical TVL -> POOL_AGG_LTV and MARKET_DEPTH
    print(f"Fetching {args.protocol} TVL history ({args.days} days)...")
    try:
        tvl_hist = fetch_historical_tvl(args.protocol, args.days)
        print(f"  Got {len(tvl_hist)} daily data points")
        
        if tvl_hist:
            first_tvl = tvl_hist[0]["tvl_usd"]
            last_tvl = tvl_hist[-1]["tvl_usd"]
            print(f"  TVL: ${first_tvl/1e9:.2f}B -> ${last_tvl/1e9:.2f}B")
        
        pool_ltv_idx = utilization_to_pool_ltv(tvl_hist)
        depth_idx = tvl_to_market_depth(tvl_hist)
        results.extend([pool_ltv_idx, depth_idx])
        
    except Exception as e:
        print(f"  ERROR: {e}")

    # Current pool utilization snapshot
    if args.all_lending:
        print("\nFetching Ethereum yield pools for aggregate utilization...")
        try:
            pools = fetch_yields_pools("Ethereum")
            agg_ltv = estimate_pool_agg_ltv(pools, LENDING_PROTOCOLS)
            print(f"  Aggregate pool utilization/LTV estimate: {agg_ltv}")
            print(f"  Pools analyzed: {len(pools)} Ethereum pools")
        except Exception as e:
            print(f"  ERROR: {e}")

    # Output
    if args.output:
        with open(args.output, "w") as f:
            json.dump(results, f, indent=2)
        print(f"\nSaved {len(results)} reference indexes to {args.output}")

    if args.post_to:
        for idx in results:
            url = f"{args.post_to}/addReferenceIndex"
            resp = requests.post(url, json=idx, timeout=30)
            print(f"  Posted {idx['riskFactorID']}: {resp.status_code}")

    if not args.output and not args.post_to:
        print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
