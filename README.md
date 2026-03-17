# DeFi Liquidation & Collateral Protection

> **ACTUS-based simulation of ETH-collateralized DeFi lending positions with real-time LTV monitoring, buffer protection, and competing-risk behavioral models — powered by the generalRisk MCP server.**

---

## What This Does

This project simulates DeFi lending positions where ETH is used as collateral for USD-denominated loans. The ACTUS financial contract engine computes contract events (rate resets, prepayments, interest payments, liquidations) based on market data you provide — ETH price curves and DeFi borrowing rate curves.

When connected to Claude via the `generalRisk` MCP server, you upload a Postman collection JSON, ask a question in plain English, and receive real simulation results rendered as interactive visualizations — all from actual ACTUS contract event computations, not mocked data.

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────────────┐
│  Claude AI   │────▶│  generalRisk     │────▶│    ACTUS Risk Engine     │
│  + JSON file │     │  MCP Server      │     │  :8082 Risk Factor Svc   │
│  + Query     │◀────│  (7 tools)       │◀────│  :8083 Simulation Svc    │
└──────────────┘     └──────────────────┘     └──────────────────────────┘
       │                                              │
       ▼                                     localhost (Docker) OR
  Interactive charts,                        AWS (34.203.247.32)
  LTV dashboards,
  scenario comparisons
```

---

## Client-Side Setup

### Step 1 — Clone the Repository

```bash
git clone <https://github.com/Ashajasmin/Defi-liquidation.git>
cd Defi-liquidation
```
### Step 2 - Docker Setup
```powershell
cd <path-to-your-clone>\Defi-liquidation\Defi-actus-external-risk
.\start-risk-actusservice-local.bat
```

### Step 3 — Install Dependencies & Build the MCP Server

```bash
cd Backend
npm install
npm run build
```

This compiles `src/mcp-server.ts` → `dist/mcp-server.js` using TypeScript (`tsc`).

### Step 4 — Add the MCP Server to Claude Desktop

Open your Claude Desktop config file:

| OS | Path |
|---|---|
| **Windows** | `%APPDATA%\Claude\claude_desktop_config.json` |
| **macOS** | `~/Library/Application Support/Claude/claude_desktop_config.json` |

Add the `generalRisk` server entry:

```json
{
  "mcpServers": {
    "generalRisk": {
      "command": "node",
      "args": [
        "<path-to-your-clone>/Defi-liquidation/Defi-interface/Backend/dist/mcp-server.js"
      ]
      
      "cwd" : "<path-to-your-clone>/Defi-liquidation/Defi-interface/Backend"

    }
  }
}
```

Replace `<path-to-your-clone>` with your actual clone location.


### Step 5 — Restart Claude Desktop

Close and reopen Claude Desktop. The `generalRisk` MCP server will start automatically.

### Step 6 — Verify the Tools Are Available

In Claude Desktop, look for the MCP tools icon (hammer/wrench). You should see the server named **`generalRisk`** with **7 tools**:

| Tool | Purpose |
|---|---|
| `run_simulation` | Execute ACTUS simulations (the main tool) |
| `list_simulations` | Browse available collections by domain |
| `load_simulation` | Load a specific collection file |
| `verify_portfolio` | Check stablecoin portfolio compliance |
| `get_threshold_presets` | Get EU MiCA / US GENIUS Act thresholds |
| `list_sample_portfolios` | List sample portfolio files |
| `load_sample_portfolio` | Load a sample portfolio |

When Claude calls any tool for the first time, you will see a permission prompt — click **Allow** (or **Allow for this chat**) to proceed.

### Step 7 — ACTUS Engine (Local Docker — Optional)

For local execution, you need the ACTUS Docker containers running.

```

This starts:
- **MongoDB** on port `27018`
- **actus-riskserver-ce** (risk factor service) on port `8082`
- **actus-server-rf20** (simulation service) on port `8083`

Verify:
```bash
curl http://localhost:8082/findAllScenarios
curl http://localhost:8083/
```

If Docker is not running, the MCP server automatically falls back to the AWS-hosted engine at `34.203.247.32:8082` / `34.203.247.32:8083`.

---
## OPTION A: 
to get response from local host use these json files: 
[local/defi-liquidation-collateral](https://github.com/Ashajasmin/Defi-liquidation/tree/main/Defi-interface/Frontend/DEMO-Defi-liquidation/local/defi-liquidation-collateral/defi-liquidation-demo)


## OPTION B:
if localhost doesnot work, check your hosted files in system 32 and also try 127.0.0.1
[hosted/defi-liquidation-collateral](https://github.com/Ashajasmin/Defi-liquidation/tree/main/Defi-interface/Frontend/DEMO-Defi-liquidation/hosted/defi-liquidation-collateral)


## Demo Prompts

Upload a DeFi liquidation collection JSON to Claude, then run these prompts:

---

## PROMPT 1 — Module 1: Four Granularity Simulations

**Upload:** All 4 JSON files from `defi-liquidation-collateral-3`

```
Run the ETH liquidation simulation for all 4 attached files against 
localhost (port 8082 risk, 8083 simulation). For each granularity — 
daily (30-day), hourly (1-week), minutely (1-hour), secondly (1-minute) 
— show me:

1. ETH/USD market risk factor chart across time
2. DeFi borrowing rate market risk factor across time
3. Collateral LTV over time with 75% warning and 82.5% liquidation 
   threshold lines
4. Full event table: time, event type, ETH price, ETH collateral, 
   buffer collateral, ETH collateral value, LTV, nominal, 
   total portfolio value, intervention
```

---

## PROMPT 2 — Module 2: Strategy Engine

**Upload:** `ETH-Liq-LTV-TPP-COM-COL-BUF-SELL-1W-100Y1W-hourly-FIXED-V5.json`

```
Run the ETH collateral strategy simulation for both daily (monthly) 
and hourly (weekly) configurations in the attached file against 
localhost (port 8082 risk, 8083 simulation). Show me:

1. ETH/USD and DeFi borrowing rate on a dual-axis chart across 
   the full simulation period
2. Collateral LTV trajectory over time — mark every buffer 
   intervention with exact payoff amount and notional after
3. Full event table: time, event type, ETH price, ETH collateral, 
   buffer ETH, collateral value, LTV, nominal, total portfolio 
   value, intervention flag
4. Raw JSON response from the ACTUS server
```

---

## PROMPT 3 — Module 3: Run All 4 Scenarios

**Upload:** Same `ETH-Liq-LTV-TPP-COM-COL-BUF-SELL-1W-100Y1W-hourly-FIXED-V5.json`

```
Run all 4 what-if scenarios from the attached file against localhost 
(port 8082 risk, 8083 simulation):

S1 — Base Case: 0% ETH drop, 65% starting LTV, 15% invoice buffer  
S2 — Stress: -10% ETH drop, 70% starting LTV, 15% invoice buffer  
S3 — Severe Crash: -20% ETH drop, 75% starting LTV, 0% buffer  
S4 — Buffered Recovery: -20% ETH drop, 75% starting LTV, 25% buffer  

Show me:
1. ETH/USD price trajectory across the simulation period
2. DeFi borrowing rate trajectory across all scenarios
3. Collateral LTV over time — all 4 scenarios overlaid on one chart 
   with 75% warning and 82.5% liquidation threshold lines
4. Scenario summary table: initial LTV, final notional, total prepaid, 
   deleveraging percentage, buffer intervention count
```

---

## PROMPT 4 — Module 3: Deep Dive Analysis

**No new upload — continue same session**

```
Using the 4 scenario results just computed:

1. For S2 (Stress): show every buffer intervention that fired — 
   exact time, payoff amount, notional after, cumulative 
   deleveraging percentage at each step

2. For S3 vs S4: both face the same -20% ETH crash and 75% starting 
   LTV — explain why S4's buffer did not deploy despite having 25% 
   buffer capacity. What is the falling knife protection mechanism 
   and what threshold triggered it?

3. DeFi borrowing rate: when does the rate peak across all scenarios, 
   what is the exact peak value, and how does the rising rate interact 
   with prepayment behavior?
```

---

## How It Works — API Call Sequence

The MCP `run_simulation` tool calls the ACTUS engine sequentially for each scenario:

1. `POST :8082/addReferenceIndex` — loads DeFi borrowing rate time series
2. `POST :8082/addReferenceIndex` — loads ETH/USD price time series
3. `POST :8082/addTwoDimensionalPrepaymentModel` — loads rate-dependent prepayment surface
4. `POST :8082/addBufferLTVModel` or `POST :8082/addCollateralLTVModel` — loads LTV monitoring config
5. `POST :8082/addScenario` — binds all risk factors into a named scenario
6. `POST :8083/rf2/scenarioSimulation` — executes the full simulation

ACTUS returns contract event arrays with: `type`, `time`, `payoff`, `currency`, `nominalValue`, `nominalRate`, `nominalAccrued`.

---

## ACTUS Contracts

| Contract ID Pattern | Type | Role | Purpose |
|---|---|---|---|
| `ETH-Collateral-{scenario}` | COM | RPA | ETH collateral position, priced via `ETH_USD` market object |
| `ETH-Buffer-{scenario}` | COM | RPA | ETH buffer reserve, deployed during LTV interventions |
| `DeFi-PAM-{scenario}` | PAM | RPA | The DeFi loan — floating rate resets via `DEFI_RATE`, prepayment models attached |

---

## Risk Models

### ReferenceIndex — Market Data
Two per scenario: **DeFi borrowing rate** (`DEFI_RATE`) and **ETH/USD price** (`ETH_USD`).

### TwoDimensionalPrepaymentModel — Voluntary Deleveraging
2D surface (rate spread × loan age) modeling voluntary rate-driven prepayment. Based on Schwartz & Torous (1989, 1993).

### CollateralLTVModel — Protocol-Enforced Liquidation
Monitors collateral LTV with hourly checks. Triggers partial repay at `ltvThreshold` (75%) targeting `ltvTarget` (65%), full liquidation at `liquidationThreshold` (82.5%). Calibrated to Aave V3.

### BufferLTVModel — Advanced Collateral Protection
The core innovation — monitors LTV and triggers buffer-to-collateral transfers with safety controls:

```json
{
  "collateralQuantity": 3.85,
  "initialBufferQuantity": 0.577,
  "bufferContractId": "ETH-Buffer-{scenario}",
  "ltvThreshold": 0.75,
  "ltvTarget": 0.73,
  "liquidationThreshold": 0.825,
  "maxInterventions": 2,
  "maxBufferUsagePerIntervention": 0.18,
  "cooldownMillis": 3600000,
  "fallingKnifePriceDropThreshold": 0.12,
  "fallingKnifeTimeWindowMillis": 14400000
}
```

`fallingKnifePriceDropThreshold` blocks buffer deployment if price drops >12% within the 4-hour lookback window — preventing buffer waste during freefall.

---

## Competing-Risk Framework

Collections implement the Deng, Quigley & Van Order (2000) competing-risks framework:

- **Risk 1: Voluntary prepayment** — rate-driven, TwoDimensionalPrepaymentModel, daily checks
- **Risk 2: Forced liquidation** — ETH-price-driven, CollateralLTVModel or BufferLTVModel, hourly monitoring

Both attach to the same PAM contract via `prepaymentModels`. ACTUS evaluates both at their monitoring times and applies whichever triggers first.

---

## ACTUS Event Types

| Event | Meaning |
|---|---|
| `IED` | Initial Exchange Date — loan origination |
| `RR` | Rate Reset — DeFi borrowing rate updated |
| `PP` | Prepayment — **payoff > 0 = real buffer/collateral intervention** |
| `IP` | Interest Payment — periodic interest settlement |
| `MD` | Maturity Date — final principal return |

---

## Collection Library

**Domain:** `defi-liquidation-collateral/` — 25 collections across 4 subcategories

- **Subcategory 1:** Multi-factor DeFi risk simulations (cascade probability, collateral rebalancing, health factor velocity) — 90-day horizons
- **Subcategory 2:** Core ETH liquidation at multiple time granularities (1-minute, 1-hour, 1-week, 1-month) — PAM + prepayment only
- **Subcategory 3:** LTV monitoring + competing risks — CollateralLTVModel and BufferLTVModel with COM collateral/buffer contracts
- **Subcategory 4:** Time-epoch collateral protection simulations

---

## Scholarly Basis

- Deng, Quigley & Van Order (2000), Econometrica — Competing risks framework
- Schwartz & Torous (1989, 1993) — Rate-driven prepayment modeling
- Qin et al. (2021) — DeFi liquidation mechanisms & thresholds
- Heimbach & Huang (2024), BIS WP 1171 — DeFi leverage & voluntary buffers
- Lehar & Parlour (2022), BIS WP 1062 — Systemic fragility, cascades

---

