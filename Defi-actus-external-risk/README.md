# DeFi ETH Collateral LTV Simulation — ACTUS RiskService

## Overview

This project extends the [ACTUS RiskService](https://github.com/actusfrf/actus-riskservice) with a custom **CollateralLTVModel** behavioral risk model that simulates ETH-collateralized DeFi loan liquidation mechanics.

Two scenarios are included as Postman collections:

| Collection | Scenario | ETH Pattern | Rate Pattern |
|---|---|---|---|
| `ETH-Liq-LTV-Coll1-mon-30` | Steady crash (stress test) | $2,775 → $1,870 | 5% → 7.62% rising |
| `ETH-Liq-LTV-Coll2-Mon-30` | Crash + bounce (realistic) | $3,310 → $1,850 → $1,970 | 2.5% → 11.8% → 2.7% |

Both scenarios simulate a **$5,000 USDC PAM loan over 30 days** with competing behavioral risks:
- **CollateralLTVModel** — triggers partial repayment when ETH collateral value falls and LTV exceeds 75%
- **TwoDimensionalPrepaymentModel** — triggers prepayment when market rates fall below contract rate

---

## Prerequisites

Make sure you have the following installed and running:

- **Docker Desktop** (with WSL2 backend on Windows)
- **Postman** (for running the simulation collections)
- **Git** (to clone repos)

---

## Step 1: Clone and Set Up the Modified RiskService

The standard `actus-riskservice` does not include the CollateralLTVModel. You need the modified version.

```bash
# Clone the riskservice
git clone https://github.com/actusfrf/actus-riskservice.git
cd actus-riskservice

# Clone actus-core dependency (required by Dockerfile)
git clone https://github.com/simplexityware/actus-core.git
```

### Apply the Modified Files

Copy the following modified/new files into the cloned repo. These are the files that implement the CollateralLTV feature:

**New files to create (4 files):**

| File | Location in repo |
|---|---|
| `CollateralLTVModel.java` | `src/main/java/org/actus/risksrv3/utils/` |
| `CollateralLTVModelData.java` | `src/main/java/org/actus/risksrv3/models/` |
| `CollateralLTVModelStore.java` | `src/main/java/org/actus/risksrv3/repository/` |
| `CollateralLTVModelNotFoundException.java` | `src/main/java/org/actus/risksrv3/controllers/` |

**Existing files to replace (3 files):**

| File | Location in repo |
|---|---|
| `RiskDataManager.java` | `src/main/java/org/actus/risksrv3/controllers/` |
| `RiskObservationHandler.java` | `src/main/java/org/actus/risksrv3/controllers/` |
| `ContractModel.java` | `src/main/java/org/actus/risksrv3/core/attributes/` |

> **Note:** Source code for all 7 files is provided separately in the `src/` folder alongside this README.

---

## Step 2: Build the Custom Docker Image

From inside the `actus-riskservice` folder:

```bash
docker build -t actus-risksrv3-custom:latest .
```

This build takes **5–10 minutes** (Gradle downloads dependencies inside the container). Wait for:

```
Successfully built <image-id>
Successfully tagged actus-risksrv3-custom:latest
```

---

## Step 3: Set Up Docker Compose Network

Clone the ACTUS Docker networks repo:

```bash
git clone https://github.com/actusfrf/actus-docker-networks.git
cd actus-docker-networks
```

Edit `quickstart-docker-actus-rf20.yml` — find the `actus-riskserver-ce` service and change its image:

```yaml
# BEFORE:
image: actusfrf/actus-riskserver-ce:latest

# AFTER:
image: actus-risksrv3-custom:latest
```

---

## Step 4: Start the Containers

```bash
docker compose -f quickstart-docker-actus-rf20.yml up -d
```

You should see 4 containers start:

```
✔ Container actus-docker-networks-mongodb-1              Started
✔ Container actus-docker-networks-actus-server-rf20-1   Started
✔ Container actus-docker-networks-actus-riskserver-ce-1 Started
```

---

## Step 5: Wait for the RiskService to Compile and Start

The riskservice **compiles Java source code on startup**. This takes **3–5 minutes**.

Monitor the logs:

```bash
docker compose -f quickstart-docker-actus-rf20.yml logs actus-riskserver-ce
```

Wait until you see:

```
Found 5 MongoDB repository interfaces.
Tomcat started on port 8082 (http)
Started Risksrv3Application in X seconds
```

> **Important:** `Found 5 MongoDB repository interfaces` confirms the CollateralLTVModel is correctly loaded. If you see only 4, the new files were not applied correctly.



## Step 6: Import the Postman Collections

1. Open Postman
2. Click **Import**
3. Import both collection files:
   - `ETH-Liq-LTV-Coll1-mon-30_postman_collection.json`
   - `ETH-Liq-LTV-Coll2-Mon-30_postman_collection.json`

---

## Step 8: Run a Simulation

Each collection has **10 requests** that must be run **in order**:

| Step | Request Name | Method | Port | Purpose |
|---|---|---|---|---|
| 1 | Add DeFi Rate Index | POST | 8082 | Load interest rate time series |
| 2 | Add ETH Price Index | POST | 8082 | Load ETH/USD price time series |
| 3 | Add Prepayment Model | POST | 8082 | Load 2D prepayment surface |
| 4 | Add CollateralLTV Model | POST | 8082 | Load LTV liquidation config |
| 5 | Add Competing-Risk Scenario | POST | 8082 | Register scenario linking all risk factors |
| 6a | Verify Reference Indexes | GET | 8082 | Confirm data loaded |
| 6b | Verify Scenarios | GET | 8082 | Confirm scenario registered |
| 6c | Verify Prepayment Model | GET | 8082 | Confirm model loaded |
| 6d | Verify CollateralLTV Model | GET | 8082 | Confirm LTV model loaded |
| 7 | Run Simulation | POST | 8083 | Execute the full simulation |

### Running the Collection

In Postman, click the **collection name → Run collection → Run**.

Or run each request manually in order (1 → 7).

---

## Step 9: Understanding the Simulation Output

The final request (Step 7) returns a JSON array of ACTUS contract events. Look for:

- **`PP` events** — Prepayment events. These reduce the notional principal.
- **`IP` events** — Interest payment events.
- **`PR` events** — Principal redemption at maturity.

The `payoff` field on each event shows the cash flow amount.

### What to Look for in the Logs

While the simulation runs, watch the riskservice container logs:

```bash
docker compose -f quickstart-docker-actus-rf20.yml logs -f actus-riskserver-ce
```

You will see real-time LTV calculations like:

```
CollateralLTVModel: time=2026-03-09T00:00 ethPrice=2200.0 collateralValue=6600.0 notionalPrincipal=5000.0 LTV=0.75
CollateralLTVModel: PARTIAL REPAY fraction=0.13333333333333333
```

---

## Scenario Details

### Coll1 — Steady Crash (Stress Test)

- ETH price: steady decline from $2,775 → $1,870 over 30 days
- DeFi rate: steady rise from 5.0% → 7.62%
- LTV triggers: 2 partial repayments (Mar 9 and Mar 19)
- Prepayment triggers: 1 small event (Mar 4, brief rate window)
- Final principal: **$3,705** | Total deleveraged: **$1,295**

### Coll2 — Crash + Bounce (Realistic)

- ETH price: crash $3,310 → $1,850, then bounce to $1,970 (based on Jan-Feb 2026 actual market)
- DeFi rate: spike 2.5% → 11.8%, then normalize to 2.7%
- LTV triggers: multiple during crash phase (cascade)
- Prepayment triggers: brief windows during rate normalization
- Final principal: **$2,871.57** | Total deleveraged: **$2,128.43**

---

## Architecture — What Was Changed

### New Files (4 added to actus-riskservice)

**`CollateralLTVModel.java`** — Core behavioral logic. At each monitoring date reads ETH price from the market model, computes LTV = debt ÷ collateral value, and returns:
- `0.0` — LTV healthy, no action
- `fraction` (0–1) — partial repayment needed to restore LTV to target
- `1.0` — full liquidation (83% threshold)

**`CollateralLTVModelData.java`** — MongoDB document storing the model config: collateral quantity, ETH price index code, LTV thresholds, monitoring schedule.

**`CollateralLTVModelStore.java`** — Spring Data repository interface for CRUD operations.

**`CollateralLTVModelNotFoundException.java`** — 404 exception handler.

### Modified Files (3 changed)

**`RiskDataManager.java`** — Added 4 REST endpoints:
- `POST /addCollateralLTVModel`
- `GET /findCollateralLTVModel/{id}`
- `GET /findAllCollateralLTVModels`
- `DELETE /deleteCollateralLTVModel/{id}`

**`RiskObservationHandler.java`** — Added `CollateralLTVModel` branch in scenario simulation, so the model is loaded and registered alongside prepayment and market models.

**`ContractModel.java`** — Added parsing of `collateralModels` array from contract JSON.

---

## CollateralLTV Model Configuration Reference

When calling `POST /addCollateralLTVModel`, the body structure is:

```json
{
  "riskFactorId": "ltv_mon_01",
  "collateralPriceMarketObjectCode": "ETH_USD",
  "collateralQuantity": 2.0,
  "ltvThreshold": 0.75,
  "ltvTarget": 0.65,
  "liquidationThreshold": 0.83,
  "monitoringEventTimes": [
    "2026-02-20T00:00:00",
    "2026-02-22T00:00:00",
    "2026-02-24T00:00:00",
    "2026-02-28T00:00:00"
  ]
}
```

| Field | Description |
|---|---|
| `riskFactorId` | Unique ID. Must match the ID in the scenario descriptor and `prepaymentModels` array in contract JSON |
| `collateralPriceMarketObjectCode` | Must match `marketObjectCode` of a ReferenceIndex already registered |
| `collateralQuantity` | Amount of collateral posted (e.g. 2.0 ETH) |
| `ltvThreshold` | LTV ratio that triggers partial repayment (e.g. 0.75 = 75%) |
| `ltvTarget` | Target LTV after repayment (e.g. 0.65 = 65%) |
| `liquidationThreshold` | LTV ratio that triggers full liquidation (e.g. 0.83 = 83%) |
| `monitoringEventTimes` | ISO-8601 datetimes when LTV is evaluated |

---

## Contract JSON — How to Link the LTV Model

In the simulation request body, the contract must include the CollateralLTV model ID in the `prepaymentModels` array:

```json
{
  "contractType": "PAM",
  "notionalPrincipal": 5000,
  "nominalInterestRate": 0.05,
  "prepaymentModels": [
    "defi_pp_mon_01",
    "ltv_mon_01"
  ]
}
```

The scenario descriptor must also include the LTV model:

```json
{
  "riskFactorDescriptors": [
    { "riskFactorID": "ltv_mon_01", "riskFactorType": "CollateralLTVModel" }
  ]
}
```

---

## Troubleshooting

**Container shows 0% CPU and 0B memory (hollow circle icon)**
The riskservice crashed. Check logs: `docker compose -f quickstart-docker-actus-rf20.yml logs actus-riskserver-ce`

**Logs show `Found 4 MongoDB repository interfaces` (not 5)**
The new Java files were not placed in the correct package directories before building. Re-apply files and rebuild.

**Build shows `package org.actus.risksrv3.riskfactors does not exist`**
The `actus-core` folder is missing from inside `actus-riskservice/`. Clone it: `git clone https://github.com/simplexityware/actus-core.git` inside the riskservice folder.

**`CollateralLTVModel cannot be converted to BehaviorRiskModelProvider`**
`CollateralLTVModel.java` is missing `implements BehaviorRiskModelProvider` on the class declaration. The interface is in `org.actus.risksrv3.utils` (same package, no import needed).

**POST returns 405 Method Not Allowed**
You sent a GET request to a POST endpoint. Change the method in Postman.

**GET `/findCollateralLTVModel/{id}` returns null**
Known display issue — does not affect simulation. The simulation loads models correctly from MongoDB even when the GET endpoint returns null.

**`localhost:8082` in browser shows Whitelabel Error Page (404)**
Normal — the riskservice is a REST API with no homepage. Use specific endpoints like `/findAllCollateralLTVModels`.

---

## Port Reference

| Port | Service |
|---|---|
| `8082` | ACTUS RiskService (data management — add/get risk factors, scenarios) |
| `8083` | ACTUS Server (simulation execution — run scenarioSimulation) |
| `27017` / `27018` | MongoDB |

---

## Behavioral Model Theory — Competing Risks

This implementation follows **Deng, Quigley, and Van Order (2000)** — "Mortgage Terminations, Heterogeneity and the Exercise of Mortgage Options" — which models prepayment and default as competing risks in a hazard model framework.

In this DeFi adaptation:
- **Prepayment risk** = refinancing incentive when market rates fall below contract rate
- **Collateral risk** = forced deleveraging when ETH price falls and LTV breaches threshold

Both risks reduce the outstanding principal. When one fires, it changes the state (notional principal) for the other. During an ETH crash, LTV dominates. During rate normalization after recovery, prepayment takes over. The two models run simultaneously on every evaluation date, with the higher return value winning.

---

*Built by ChainAim Technologies — ACTUS CollateralLTV Extension v1.0*
