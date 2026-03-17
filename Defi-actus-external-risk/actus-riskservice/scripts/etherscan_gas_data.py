#!/usr/bin/env python3
"""
Etherscan Gas Tracker + Infura/Alchemy EIP-1559 Integration
============================================================
Fetches real-time and historical gas prices for ACTUS GasOptimizationModel.

APIs:
  1. Etherscan Gas Tracker: https://api.etherscan.io/api (free, requires API key)
  2. Infura JSON-RPC: eth_gasPrice, eth_feeHistory (free tier: 100K req/day)
  3. Alchemy JSON-RPC: same endpoints (free tier: 300 CU/s)

Usage:
    python etherscan_gas_data.py --source etherscan --output gas_index.json
    python etherscan_gas_data.py --source infura --blocks 1000
    python etherscan_gas_data.py --post-to http://localhost:8082

Environment:
    ETHERSCAN_API_KEY=<your_key>
    INFURA_PROJECT_ID=<your_project_id>
    ALCHEMY_API_KEY=<your_key>
"""

import argparse
import json
import os
import sys
import time
from datetime import datetime, timezone, timedelta
from typing import Dict, List

try:
    import requests
except ImportError:
    print("ERROR: pip install requests")
    sys.exit(1)

# =========================================================================
# ETHERSCAN GAS TRACKER
# =========================================================================
def fetch_etherscan_gas() -> Dict:
    """
    GET https://api.etherscan.io/api?module=gastracker&action=gasoracle
    
    Returns: {SafeGasPrice, ProposeGasPrice, FastGasPrice, suggestBaseFee, gasUsedRatio}
    All values in Gwei.
    """
    api_key = os.environ.get("ETHERSCAN_API_KEY", "YourApiKeyToken")
    url = "https://api.etherscan.io/api"
    params = {
        "module": "gastracker",
        "action": "gasoracle",
        "apikey": api_key,
    }
    resp = requests.get(url, params=params, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    if data.get("status") != "1":
        raise ValueError(f"Etherscan error: {data.get('message')}")
    result = data["result"]
    return {
        "safe_gwei": float(result["SafeGasPrice"]),
        "proposed_gwei": float(result["ProposeGasPrice"]),
        "fast_gwei": float(result["FastGasPrice"]),
        "base_fee_gwei": float(result.get("suggestBaseFee", 0)),
        "gas_used_ratio": result.get("gasUsedRatio", ""),
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


def fetch_etherscan_gas_estimate(gas_price_gwei: float) -> Dict:
    """
    GET https://api.etherscan.io/api?module=gastracker&action=gasestimate&gasprice={gwei}000000000
    Returns estimated confirmation time in seconds.
    """
    api_key = os.environ.get("ETHERSCAN_API_KEY", "YourApiKeyToken")
    url = "https://api.etherscan.io/api"
    params = {
        "module": "gastracker",
        "action": "gasestimate",
        "gasprice": str(int(gas_price_gwei * 1e9)),
        "apikey": api_key,
    }
    resp = requests.get(url, params=params, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    return {"gasprice_gwei": gas_price_gwei, "est_seconds": int(data.get("result", 0))}


# =========================================================================
# INFURA / ALCHEMY JSON-RPC
# =========================================================================
def get_infura_url() -> str:
    project_id = os.environ.get("INFURA_PROJECT_ID", "YOUR_PROJECT_ID")
    return f"https://mainnet.infura.io/v3/{project_id}"


def get_alchemy_url() -> str:
    api_key = os.environ.get("ALCHEMY_API_KEY", "YOUR_API_KEY")
    return f"https://eth-mainnet.g.alchemy.com/v2/{api_key}"


def rpc_call(url: str, method: str, params: list = None) -> Dict:
    """Generic Ethereum JSON-RPC call."""
    payload = {
        "jsonrpc": "2.0",
        "method": method,
        "params": params or [],
        "id": 1,
    }
    resp = requests.post(url, json=payload, timeout=30)
    resp.raise_for_status()
    data = resp.json()
    if "error" in data:
        raise ValueError(f"RPC error: {data['error']}")
    return data["result"]


def fetch_current_gas_price(rpc_url: str) -> float:
    """eth_gasPrice -> current gas price in Gwei."""
    result = rpc_call(rpc_url, "eth_gasPrice")
    return int(result, 16) / 1e9


def fetch_fee_history(rpc_url: str, block_count: int = 100) -> Dict:
    """
    eth_feeHistory -> EIP-1559 base fee + priority fee history.
    
    Returns: {baseFeePerGas: [hex...], reward: [[hex...]], gasUsedRatio: [float...]}
    block_count: max 1024 per call
    """
    result = rpc_call(rpc_url, "eth_feeHistory", [
        hex(min(block_count, 1024)),
        "latest",
        [25, 50, 75]  # percentiles for priority fees
    ])
    return result


def fee_history_to_gwei(fee_history: Dict) -> List[Dict]:
    """Convert hex fee history to Gwei time series."""
    base_fees = fee_history.get("baseFeePerGas", [])
    rewards = fee_history.get("reward", [])
    gas_ratios = fee_history.get("gasUsedRatio", [])
    
    records = []
    for i, base_hex in enumerate(base_fees[:-1]):  # last entry is next block estimate
        base_gwei = int(base_hex, 16) / 1e9
        record = {"block_offset": i, "base_fee_gwei": round(base_gwei, 4)}
        
        if i < len(rewards) and rewards[i]:
            priority = [int(r, 16) / 1e9 for r in rewards[i]]
            record["priority_p25_gwei"] = round(priority[0], 4)
            record["priority_p50_gwei"] = round(priority[1], 4)
            record["priority_p75_gwei"] = round(priority[2], 4)
            record["total_p50_gwei"] = round(base_gwei + priority[1], 4)
        
        if i < len(gas_ratios):
            record["gas_used_ratio"] = round(gas_ratios[i], 4)
        
        records.append(record)
    
    return records


# =========================================================================
# ACTUS CONVERSION
# =========================================================================
def to_actus_gas_index(
    records: List[Dict],
    risk_factor_id: str = "ETH_GAS_01",
    start_date: str = None,
) -> Dict:
    """
    Convert gas price records to ACTUS ReferenceIndex.
    Uses daily average if multiple blocks per day.
    """
    if not start_date:
        start_date = datetime.now(timezone.utc).strftime("%Y-%m-%dT00:00:00")
    
    # Group by day (approximate: ~7200 blocks/day)
    blocks_per_day = 7200
    daily_data = {}
    base_dt = datetime.strptime(start_date, "%Y-%m-%dT%H:%M:%S")
    
    for rec in records:
        day_offset = rec["block_offset"] // blocks_per_day
        day_key = (base_dt + timedelta(days=day_offset)).strftime("%Y-%m-%dT%H:%M:%S")
        
        if day_key not in daily_data:
            daily_data[day_key] = []
        
        gas_val = rec.get("total_p50_gwei", rec.get("base_fee_gwei", 25.0))
        daily_data[day_key].append(gas_val)
    
    data = []
    for dt_str in sorted(daily_data.keys()):
        vals = daily_data[dt_str]
        avg_gas = round(sum(vals) / len(vals), 2)
        data.append({"time": dt_str, "value": avg_gas})
    
    return {
        "riskFactorID": risk_factor_id,
        "marketObjectCode": "ETH_GAS_PRICE",
        "base": 1.0,
        "data": data,
    }


# =========================================================================
# MAIN
# =========================================================================
def main():
    parser = argparse.ArgumentParser(description="ETH Gas -> ACTUS ReferenceIndex")
    parser.add_argument("--source", choices=["etherscan", "infura", "alchemy"],
                        default="etherscan")
    parser.add_argument("--blocks", type=int, default=1000, help="Blocks of history (RPC)")
    parser.add_argument("--output", type=str, help="Output JSON file")
    parser.add_argument("--post-to", type=str, help="POST to ACTUS risk service")
    args = parser.parse_args()

    if args.source == "etherscan":
        print("Fetching from Etherscan Gas Tracker...")
        gas = fetch_etherscan_gas()
        print(f"  Safe: {gas['safe_gwei']} Gwei")
        print(f"  Proposed: {gas['proposed_gwei']} Gwei")
        print(f"  Fast: {gas['fast_gwei']} Gwei")
        print(f"  Base Fee: {gas['base_fee_gwei']} Gwei")
        
        # For Etherscan, we get current snapshot only
        # Create single-point reference index
        ref = {
            "riskFactorID": "ETH_GAS_01",
            "marketObjectCode": "ETH_GAS_PRICE",
            "base": 1.0,
            "data": [{"time": gas["timestamp"][:19], "value": gas["proposed_gwei"]}],
        }
        results = [ref]

    else:
        rpc_url = get_infura_url() if args.source == "infura" else get_alchemy_url()
        print(f"Fetching from {args.source} ({args.blocks} blocks)...")
        
        current_gas = fetch_current_gas_price(rpc_url)
        print(f"  Current gas: {current_gas:.2f} Gwei")
        
        fee_hist = fetch_fee_history(rpc_url, args.blocks)
        records = fee_history_to_gwei(fee_hist)
        print(f"  Retrieved {len(records)} block records")
        
        if records:
            avg = sum(r.get("total_p50_gwei", r["base_fee_gwei"]) for r in records) / len(records)
            max_gas = max(r.get("total_p50_gwei", r["base_fee_gwei"]) for r in records)
            min_gas = min(r.get("total_p50_gwei", r["base_fee_gwei"]) for r in records)
            print(f"  Avg: {avg:.2f} Gwei, Min: {min_gas:.2f}, Max: {max_gas:.2f}")
        
        ref = to_actus_gas_index(records)
        results = [ref]

    if args.output:
        with open(args.output, "w") as f:
            json.dump(results, f, indent=2)
        print(f"\nSaved to {args.output}")

    if args.post_to:
        for idx in results:
            url = f"{args.post_to}/addReferenceIndex"
            resp = requests.post(url, json=idx, timeout=30)
            print(f"  Posted {idx['riskFactorID']}: {resp.status_code}")

    if not args.output and not args.post_to:
        print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
