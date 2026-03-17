#!/usr/bin/env python3
"""
ETH ETF Flow Data for ACTUS CollateralRebalancingModel
=======================================================
Fetches daily ETH ETF net flows from farside.co.uk/eth.

Source: https://farside.co.uk/eth (daily net flows in $M)
Used by: CollateralRebalancingModel - ETF sentiment signal
  Large outflows -> bearish -> tighten thresholds by etfSensitivity (0.05 per $100M)

NOTE: farside.co.uk does not have a public API. This script:
  1. Provides a CSV parser for manually downloaded data
  2. Generates synthetic ETF flow data for simulation testing
  3. Shows how to POST to ACTUS risk service

For production: Download CSV from farside.co.uk/eth -> parse -> POST.

Usage:
    python etf_flow_data.py --generate --days 90 --output etf_flows.json
    python etf_flow_data.py --csv etf_data.csv --post-to http://localhost:8082
"""

import argparse
import csv
import json
import math
import os
import random
import sys
from datetime import datetime, timedelta
from typing import Dict, List

try:
    import requests
except ImportError:
    requests = None


# =========================================================================
# CSV PARSER (for manually downloaded farside.co.uk data)
# =========================================================================
def parse_farside_csv(filepath: str) -> List[Dict]:
    records = []
    with open(filepath, "r") as f:
        reader = csv.DictReader(f)
        for row in reader:
            date_str = row.get("Date", "").strip()
            total_str = row.get("Total", "0").strip()
            if not date_str:
                continue
            dt = None
            for fmt in ["%d %b %Y", "%Y-%m-%d", "%m/%d/%Y", "%d/%m/%Y"]:
                try:
                    dt = datetime.strptime(date_str, fmt)
                    break
                except ValueError:
                    continue
            if not dt:
                print(f"  WARNING: Cannot parse date '{date_str}', skipping")
                continue
            total = total_str.replace(",", "").replace("$", "").replace("(", "-").replace(")", "")
            try:
                flow_m = float(total)
            except ValueError:
                flow_m = 0.0
            records.append({"time": dt.strftime("%Y-%m-%dT%H:%M:%S"), "value": round(flow_m, 2)})
    return sorted(records, key=lambda r: r["time"])


# =========================================================================
# SYNTHETIC DATA GENERATOR (for simulation testing)
# =========================================================================
def generate_etf_flows(days: int, seed: int = 42) -> List[Dict]:
    random.seed(seed)
    base = datetime.now() - timedelta(days=days)
    records = []
    for i in range(days):
        dt = base + timedelta(days=i)
        if dt.weekday() >= 5:
            continue
        if i < 10:
            trend = 80 - i * 10
        elif i < 20:
            trend = -20 - (i - 10) * 18
        elif i < 30:
            trend = -200 + (i - 20) * 8
        elif i < 45:
            trend = -120 + (i - 30) * 10
        elif i < 60:
            trend = 30 + (i - 45) * 3
        else:
            trend = 75 + min(i - 60, 20) * 2
        noise = random.gauss(0, 40)
        flow = round(trend + noise, 2)
        records.append({"time": dt.strftime("%Y-%m-%dT%H:%M:%S"), "value": flow})
    return records


# =========================================================================
# ACTUS CONVERSION
# =========================================================================
def to_actus_etf_flow_index(records: List[Dict]) -> Dict:
    return {"riskFactorID": "ETH_ETF_FLOW_01", "marketObjectCode": "ETH_ETF_FLOW", "base": 1.0, "data": records}


# =========================================================================
# MAIN
# =========================================================================
def main():
    parser = argparse.ArgumentParser(description="ETH ETF Flow -> ACTUS ReferenceIndex")
    parser.add_argument("--csv", type=str, help="Path to farside.co.uk CSV download")
    parser.add_argument("--generate", action="store_true", help="Generate synthetic data")
    parser.add_argument("--days", type=int, default=90, help="Days for synthetic data")
    parser.add_argument("--output", type=str, help="Output JSON file")
    parser.add_argument("--post-to", type=str, help="POST to ACTUS risk service")
    args = parser.parse_args()

    if args.csv:
        print(f"Parsing ETF flow CSV: {args.csv}")
        records = parse_farside_csv(args.csv)
        print(f"  Parsed {len(records)} trading days")
    elif args.generate:
        print(f"Generating synthetic ETF flow data ({args.days} days)...")
        records = generate_etf_flows(args.days)
        print(f"  Generated {len(records)} trading days")
    else:
        print("Specify --csv <path> or --generate")
        print("\nTo get real data:")
        print("  1. Visit https://farside.co.uk/eth")
        print("  2. Download/copy the table to CSV")
        print("  3. Run: python etf_flow_data.py --csv etf_data.csv")
        return

    if records:
        total_inflow = sum(r["value"] for r in records if r["value"] > 0)
        total_outflow = sum(r["value"] for r in records if r["value"] < 0)
        net = sum(r["value"] for r in records)
        max_day = max(records, key=lambda r: r["value"])
        min_day = min(records, key=lambda r: r["value"])
        print(f"\n  Net flow: ${net:.0f}M")
        print(f"  Total inflows: +${total_inflow:.0f}M")
        print(f"  Total outflows: ${total_outflow:.0f}M")
        print(f"  Best day: {max_day['time'][:10]} +${max_day['value']:.0f}M")
        print(f"  Worst day: {min_day['time'][:10]} ${min_day['value']:.0f}M")

    ref = to_actus_etf_flow_index(records)
    results = [ref]

    if args.output:
        with open(args.output, "w") as f:
            json.dump(results, f, indent=2)
        print(f"\nSaved to {args.output}")
    if args.post_to:
        if not requests:
            print("ERROR: pip install requests"); return
        url = f"{args.post_to}/addReferenceIndex"
        resp = requests.post(url, json=ref, timeout=30)
        print(f"  Posted ETH_ETF_FLOW_01: {resp.status_code}")


if __name__ == "__main__":
    main()
