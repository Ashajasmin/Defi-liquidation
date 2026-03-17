# ACTUS DeFi Liquidation — Reference Index Setup & API Guide

## Overview

The 7 DeFi liquidation behavioral models consume market data via ACTUS ReferenceIndex objects. Each model specifies a `marketObjectCode` (MOC) that maps to a time-series index loaded into MongoDB via the risk service REST API.

---

## Reference Index Registry

| MOC | Description | Used By | Source | Update Freq |
|-----|-------------|---------|--------|-------------|
| `ETH_USD` | ETH spot price (USD) | HealthFactor, Velocity, Rebalancing, Cascade, Gas, Invoice | CoinGecko | Daily/Hourly |
| `BTC_USD` | BTC spot price (USD) | CorrelationRisk (asset2) | CoinGecko | Daily |
| `WSTETH_USD` | wstETH price (USD) | HealthFactor (multi-collateral) | CoinGecko | Daily |
| `USDC_USD` | USDC peg price | HealthFactor, Rebalancing | CoinGecko | Daily |
| `ETH_GAS_PRICE` | Gas price (Gwei) | GasOptimization | Etherscan / Infura | Real-time |
| `POOL_AGG_LTV` | Aggregate pool LTV (0-1) | CascadeProbability | DeFi Llama (derived) | Daily |
| `MARKET_DEPTH` | Order book depth ($M) | CascadeProbability | CoinGecko tickers | Daily |
| `ETH_ETF_FLOW` | Daily ETF net flow ($M) | CollateralRebalancing | farside.co.uk/eth | Daily |
| `INVOICE_PAYMENT_PROB` | Invoice payment probability (0-1) | InvoiceMaturity | Credit scoring service | Weekly |

### Technical Indicator Indexes (Optional, from TradingView computation)

| MOC | Description | Used By | Computation |
|-----|-------------|---------|-------------|
| `ETH_RSI` | RSI(14) 0-100 | CollateralVelocity context | Local (14-period) |
| `ETH_MACD` | MACD line | Velocity trend context | Local (12,26,9) |
| `ETH_MACD_SIGNAL` | MACD signal line | Velocity trend context | Local |
| `ETH_MACD_HISTOGRAM` | MACD histogram | Momentum measurement | Local |
| `ETH_BB_UPPER` | Bollinger upper band | Volatility envelope | Local (20,2s) |
| `ETH_BB_LOWER` | Bollinger lower band | Volatility envelope | Local |
| `ETH_BB_WIDTH` | Bollinger band width | Volatility regime | Local |
| `ETH_ATR` | Average True Range($) | Absolute volatility | Local (14-period) |
| `ETH_VWAP` | Volume Weighted Avg Price | Fair value reference | Local (cumulative) |
| `ETH_ANNUALIZED_VOL` | Annualized volatility (%) | CollateralVelocity | Local (30-day rolling) |

---

## API Sources — Detailed Setup

### 1. CoinGecko (ETH_USD, BTC_USD, WSTETH_USD, USDC_USD)

**Endpoint:** `GET /coins/{id}/market_chart`
**Base URL:** `https://api.coingecko.com/api/v3` (free) or `https://pro-api.coingecko.com/api/v3` (pro)

**Free tier:** 30 calls/min, 10,000 calls/month
**Pro tier:** 500 calls/min ($129/month)

```bash
# Current price
curl "https://api.coingecko.com/api/v3/simple/price?ids=ethereum,bitcoin&vs_currencies=usd"

# 90-day daily history
curl "https://api.coingecko.com/api/v3/coins/ethereum/market_chart?vs_currency=usd&days=90&interval=daily"

# Tickers for order book depth proxy
curl "https://api.coingecko.com/api/v3/coins/ethereum/tickers?include_exchange_logo=false&depth=true"
```

**ACTUS Script:** `scripts/coingecko_eth_data.py`
```bash
pip install requests
python coingecko_eth_data.py --days 90 --post-to http://localhost:8082
# Optional: export COINGECKO_API_KEY=<key> for Pro tier
```

**Coin IDs:** ethereum, bitcoin, wrapped-steth, usd-coin

---

### 2. Etherscan Gas Tracker (ETH_GAS_PRICE)

**Endpoint:** `GET /api?module=gastracker&action=gasoracle`
**Base URL:** `https://api.etherscan.io`
**Free tier:** 5 calls/sec (requires API key, free registration)

```bash
curl "https://api.etherscan.io/api?module=gastracker&action=gasoracle&apikey=YOUR_KEY"
```

**Alternative: Infura/Alchemy EIP-1559 (for historical fee data)**
```bash
# eth_feeHistory — last 1000 blocks (~3.5 hours)
curl -X POST https://mainnet.infura.io/v3/YOUR_ID \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"eth_feeHistory","params":["0x3E8","latest",[25,50,75]],"id":1}'
```

**ACTUS Script:** `scripts/etherscan_gas_data.py`
```bash
export ETHERSCAN_API_KEY=<key>
python etherscan_gas_data.py --source etherscan --post-to http://localhost:8082
```

---

### 3. DeFi Llama (POOL_AGG_LTV, MARKET_DEPTH)

**Endpoint:** `GET /protocol/{name}` (historical TVL)
**Base URL:** `https://api.llama.fi`
**Tier:** 100% free, no API key, no rate limits

```bash
# Current TVL
curl "https://api.llama.fi/tvl/aave"

# Full protocol detail with historical TVL
curl "https://api.llama.fi/protocol/aave"

# All yield pools (includes utilization metrics)
curl "https://yields.llama.fi/pools"
```

**POOL_AGG_LTV derivation:**
- Fetch TVL history for Aave/Compound/Maker
- As TVL drops during stress -> utilization rises -> aggregate LTV rises
- Formula: `pool_agg_ltv = base_utilization / (current_tvl / max_tvl)`

**MARKET_DEPTH derivation:**
- Order book depth ~ 1% of lending TVL (proxy)
- More precise: CoinGecko `/coins/ethereum/tickers` bid/ask depth

**ACTUS Script:** `scripts/defillama_data.py`
```bash
python defillama_data.py --protocol aave --days 90 --post-to http://localhost:8082
python defillama_data.py --all-lending  # aggregate across protocols
```

---

### 4. ETH ETF Flow (ETH_ETF_FLOW)

**Source:** `https://farside.co.uk/eth` (manual download, no API)

**Format:** Daily net flow in $M per fund, with total column.
**Interpretation:**
- Positive = net inflow (bullish sentiment)
- Negative = net outflow (bearish -> tighten CollateralRebalancing thresholds)
- CollateralRebalancingModel uses: `etfSensitivity x (outflow / $100M)` to adjust LTV trigger

**ACTUS Script:** `scripts/etf_flow_data.py`
```bash
# From downloaded CSV
python etf_flow_data.py --csv ~/Downloads/eth_etf_flows.csv --post-to http://localhost:8082

# Generate synthetic for testing
python etf_flow_data.py --generate --days 90 --post-to http://localhost:8082
```

---

### 5. TradingView Technical Indicators

**Note:** TradingView does not offer a public REST API. Our script computes identical indicators locally from CoinGecko OHLCV data.

**Indicators computed:**

| Indicator | Formula | DeFi Model Relevance |
|-----------|---------|----------------------|
| **RSI(14)** | 100 - 100/(1+RS) | >70 overbought -> expect correction; <30 oversold -> expect bounce |
| **MACD(12,26,9)** | EMA(12)-EMA(26) | Crossover below 0 -> bearish momentum -> CollateralVelocity alert |
| **Bollinger(20,2s)** | SMA+/-2s | Price outside bands -> extreme volatility -> regime change |
| **ATR(14)** | EMA of True Range | High ATR -> high $ volatility -> urgency escalation |
| **VWAP** | Sum(PV)/Sum(V) | Price below VWAP -> bearish pressure -> rebalancing signal |
| **Ann. Volatility** | s(ln returns)*sqrt(365) | Direct input for days-to-liquidation calculation |

**ACTUS Script:** `scripts/tradingview_indicators.py`
```bash
python tradingview_indicators.py --coin ethereum --days 120 --post-to http://localhost:8082
```

---

## Quick Start: Load All Data

```bash
cd scripts/

# Option A: Load everything (live APIs where available, synthetic fallbacks)
python load_all_data.py --days 90 --post-to http://localhost:8082

# Option B: Individual scripts
python coingecko_eth_data.py --days 90 --post-to http://localhost:8082
python etherscan_gas_data.py --source etherscan --post-to http://localhost:8082
python defillama_data.py --protocol aave --days 90 --post-to http://localhost:8082
python etf_flow_data.py --generate --days 90 --post-to http://localhost:8082
python tradingview_indicators.py --coin ethereum --days 120 --post-to http://localhost:8082
```

---

## Simulation Files

Pre-built Postman collections with realistic ETH crash scenarios:

| File | Models Tested | Scenario |
|------|--------------|----------|
| `DeFi-HealthFactor-CollateralVelocity-90d.json` | HealthFactor + Velocity | ETH $2800->$1400 crash, multi-collateral |
| `DeFi-CollateralRebalancing-CorrelationRisk-90d.json` | Rebalancing + Correlation | ETH/BTC correlation spike, ETF outflows |
| `DeFi-CascadeProbability-GasOptimization-90d.json` | Cascade + Gas | Pool stress + 200 Gwei gas spike |
| `DeFi-InvoiceMaturity-90d.json` | InvoiceMaturity | RWA collateral with credit degradation |

**To run:** Import any `.json` file into Postman -> Run Collection. Open Postman Console (Ctrl+Alt+C) to see behavioral events.

**Ports:**
- `localhost:8082` — actus-riskservice (REST API for model/index CRUD)
- `localhost:8083` — actus-service (simulation engine, `/rf2/scenarioSimulation`)

---

## Model-Index Dependency Matrix

```
                    ETH   BTC   wstETH  USDC  GAS   POOL  DEPTH  ETF   INV_PROB
                    USD   USD   USD     USD   PRICE  LTV          FLOW
HealthFactor        *     o     *       *
CollateralVelocity  *
CollateralRebalancing *         *             *                   *
CorrelationRisk     *     *
CascadeProbability  *                               *     *
GasOptimization     *                         *
InvoiceMaturity     *                                                    *

* = required   o = optional (for multi-collateral)
```

---

## Historical ETH Volatility Context

| Period | Event | Annualized Vol | ETH Drawdown |
|--------|-------|---------------|--------------|
| 2017-18 | ICO boom/bust | 80-150% | -94% ($1400->$84) |
| Mar 2020 | COVID Black Thursday | 200%+ | -63% ($280->$104) |
| 2021 | Bull run | 70-110% | -55% ($4300->$1900) |
| May 2022 | UST/LUNA collapse | 120%+ | -82% ($3500->$880) |
| Mar 2023 | SVB/USDC de-peg | 60-90% | -15% ($1650->$1400) |
| 2024-26 | ETH ETF era | 40-100% | Varies |

---

## Scholarly References

- **Lehar & Parlour (2022)** — "Systemic Fragility in Decentralized Markets," BIS Working Paper 1062.
- **Heimbach & Huang (2024)** — "DeFi Liquidations," BIS Working Paper 1171.
- **Sadeghi (2025)** — "Transaction fee role in liquidation dynamics."
- **Hertel (1997)** — Global Trade Analysis Project (GTAP) framework.
- **Armington (1969)** — Elasticity foundations.
