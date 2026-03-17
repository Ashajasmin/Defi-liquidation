#!/bin/bash
# ============================================================================
#  PP-before-IED Fix Verification Test Script
# ============================================================================
#
#  Tests all 13 models that received the PP-before-IED fix.
#  
#  WHAT IT DOES:
#    1. Loads reference indexes (market data) for each domain
#    2. Loads behavioral models with monitoringEventTimes that include
#       dates BEFORE the contract's initialExchangeDate (IED)
#    3. Creates scenarios referencing these models
#    4. Runs scenarioSimulation with contracts whose IED is AFTER some
#       monitoring times
#    5. You verify in docker logs that "SKIPPING pre-IED callout" appears
#
#  HOW TO RUN:
#    cd actus-docker-networks
#    docker compose -f quickstart-docker-actus-rf20.yml up -d
#    # Wait for startup, then:
#    bash test/pp_ied_fix_test.sh
#    # Check logs:
#    docker compose -f quickstart-docker-actus-rf20.yml logs actus-riskserver-ce | grep "SKIPPING"
#
#  EXPECTED RESULT:
#    For each model you should see lines like:
#    **** TwoDimensionalPrepaymentModel: SKIPPING pre-IED callout 2014-06-01T00:00:00 (IED=2015-01-02T00:00:00)
#    **** TariffSpreadModel: SKIPPING pre-IED callout 2024-06-01T00:00:00 (IED=2025-01-02T00:00:00)
#    etc.
#
#  FILES CHANGED IN THIS FIX (13 files):
#    Core (2):
#      1. TwoDimensionalPrepaymentModel.java
#      2. TwoDimensionalDepositTrxModel.java
#    Supply Chain Tariff (5):
#      3. TariffSpreadModel.java
#      4. WorkingCapitalStressModel.java
#      5. HedgeEffectivenessModel.java
#      6. FXTariffCorrelationModel.java
#      7. PortCongestionModel.java
#    DeFi Liquidation (6):
#      8. CollateralVelocityModel.java
#      9. CollateralRebalancingModel.java
#     10. CorrelationRiskModel.java
#     11. CascadeProbabilityModel.java
#     12. GasOptimizationModel.java
#     13. InvoiceMaturityModel.java
# ============================================================================

RISK_PORT=8082
SIM_PORT=8083
echo "============================================"
echo " PP-before-IED Fix Verification Tests"
echo " Risk server: localhost:${RISK_PORT}"
echo " Sim  server: localhost:${SIM_PORT}"
echo "============================================"

# ============================================================================
# STEP 0: Prerequisite — reference indexes (market data)
# ============================================================================
echo ""
echo "--- Step 0: Loading reference indexes ---"

# Reuse existing UST 5Y falling rate (needed by TwoDimensionalPrepaymentModel)
source test/putUst5Y_falling.txt 2>/dev/null || \
curl -s -H 'Content-Type: application/json' --data '{"riskFactorId":"ust5Y_falling","riskFactorType":"ReferenceIndex","timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant","margins":[{"dimension":1,"values":[0.0,1.0,2.0,3.0,5.0,10.0]}],"data":[[0.05,0.045,0.04,0.035,0.03,0.025]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> ust5Y_falling loaded"

# ETH_USD price series (for DeFi models)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"ETH_USD","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,0.5,1.0,1.5,2.0]}],
    "data":[[3500.0, 3200.0, 2800.0, 2500.0, 2200.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> ETH_USD loaded"

# BTC_USD price series (for CorrelationRiskModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"BTC_USD","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,0.5,1.0,1.5,2.0]}],
    "data":[[65000.0, 60000.0, 55000.0, 50000.0, 45000.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> BTC_USD loaded"

# USDC_USD peg (for CollateralRebalancingModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"USDC_USD","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[1.0, 0.999, 0.998]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> USDC_USD loaded"

# ETH_ETF_FLOW (for CollateralRebalancingModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"ETH_ETF_FLOW","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[-50.0, -75.0, -120.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> ETH_ETF_FLOW loaded"

# GAS_PRICE_GWEI (for GasOptimizationModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"GAS_PRICE_GWEI","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[25.0, 50.0, 100.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> GAS_PRICE_GWEI loaded"

# POOL_AGG_LTV (for CascadeProbabilityModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"POOL_AGG_LTV","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[0.65, 0.72, 0.78]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> POOL_AGG_LTV loaded"

# MARKET_DEPTH (for CascadeProbabilityModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"MARKET_DEPTH_USD","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[5000000.0, 3000000.0, 1500000.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> MARKET_DEPTH_USD loaded"

# INVOICE_PAYMENT_PROB (for InvoiceMaturityModel)
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"INVOICE_PAYMENT_PROB","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[0.95, 0.90, 0.85]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> INVOICE_PAYMENT_PROB loaded"

# Supply chain tariff reference indexes
# TARIFF_INDEX — needed by TariffSpreadModel, WorkingCapitalStressModel, HedgeEffectivenessModel, FXTariffCorrelationModel, PortCongestionModel
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"TARIFF_INDEX","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[0.10, 0.20, 0.25]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> TARIFF_INDEX loaded"

# USD_INR_SPOT — needed by HedgeEffectivenessModel, FXTariffCorrelationModel
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"USD_INR_SPOT","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[83.0, 84.5, 86.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> USD_INR_SPOT loaded"

# REVENUE_INDEX — needed by WorkingCapitalStressModel
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"REVENUE_INDEX","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[1.0, 0.90, 0.80]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> REVENUE_INDEX loaded"

# DSO_INDEX — needed by WorkingCapitalStressModel
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"DSO_INDEX","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[45.0, 55.0, 65.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> DSO_INDEX loaded"

# EXPORT_REVENUE_USD — needed by HedgeEffectivenessModel
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"EXPORT_REVENUE_USD","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[5000000.0, 4500000.0, 4000000.0]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> EXPORT_REVENUE_USD loaded"

# PORT_CONGESTION_INDEX — needed by PortCongestionModel
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId":"PORT_CONGESTION_INDEX","riskFactorType":"ReferenceIndex",
  "timeSeries":{"interpolationMethod":"linear","extrapolationMethod":"constant",
    "margins":[{"dimension":1,"values":[0.0,1.0,2.0]}],
    "data":[[1.0, 1.5, 2.5]]}}' http://localhost:${RISK_PORT}/addReferenceIndex
echo " -> PORT_CONGESTION_INDEX loaded"

echo ""
echo "--- Step 1: Loading behavioral models (with pre-IED monitoring times) ---"

# ============================================================================
# TEST 1: TwoDimensionalPrepaymentModel
#   Contract IED = 2015-01-02
#   Pre-IED event times: 2014-06-01, 2014-12-01 (should be SKIPPED)
#   Post-IED event times: 2015-03-01, 2015-09-01, 2016-03-01 (should pass)
# ============================================================================
echo ""
echo "TEST 1: TwoDimensionalPrepaymentModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "ppm01_ied_test",
  "referenceRateId": "ust5Y_falling",
  "prepaymentEventTimes": [
    "2014-06-01T00:00:00","2014-12-01T00:00:00",
    "2015-03-01T00:00:00","2015-09-01T00:00:00","2016-03-01T00:00:00"
  ],
  "surface": {
    "interpolationMethod": "linear", "extrapolationMethod": "constant",
    "margins": [
      {"dimension": 1, "values": [0.03, 0.025, 0.02, 0.015, 0.01, 0.0, -0.05]},
      {"dimension": 2, "values": [0,1,2,3,5,10]}
    ],
    "data": [
      [0.01, 0.05, 0.1, 0.07, 0.02, 0],
      [0.01, 0.04, 0.8, 0.05, 0.01, 0],
      [0, 0.02, 0.5, 0.03, 0.005, 0],
      [0, 0.01, 0.3, 0.01, 0, 0],
      [0, 0.01, 0.2, 0, 0, 0],
      [0, 0, 0.1, 0, 0, 0],
      [0, 0, 0, 0, 0, 0]
    ]
  }
}' http://localhost:${RISK_PORT}/addTwoDimensionalPrepaymentModel
echo " -> ppm01_ied_test model loaded (2 pre-IED + 3 post-IED events)"

# ============================================================================
# TEST 2: CollateralVelocityModel
#   Contract IED = 2025-01-02
#   Pre-IED: 2024-06-01, 2024-12-01 (should be SKIPPED)
#   Post-IED: 2025-02-01, 2025-06-01, 2025-12-01
# ============================================================================
echo ""
echo "TEST 2: CollateralVelocityModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "cv01_ied_test",
  "collateralPriceMOC": "ETH_USD",
  "collateralQuantity": 10.0,
  "liquidationThreshold": 0.83,
  "safeHorizonDays": 7.0,
  "urgentDays": 2.0,
  "moderateRepayFraction": 0.10,
  "aggressiveRepayFraction": 0.25,
  "rollingWindowSize": 5,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addCollateralVelocityModel
echo " -> cv01_ied_test loaded"

# ============================================================================
# TEST 3: CollateralRebalancingModel
# ============================================================================
echo ""
echo "TEST 3: CollateralRebalancingModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "cr01_ied_test",
  "volatileAssetMOC": "ETH_USD",
  "stableAssetMOC": "USDC_USD",
  "etfFlowMOC": "ETH_ETF_FLOW",
  "volatileAssetQty": 5.0,
  "stableAssetQty": 5000.0,
  "invoiceValueUSD": 3000.0,
  "overallLtvThreshold": 0.75,
  "liquidationThreshold": 0.83,
  "ltvTarget": 0.65,
  "etfFlowThreshold": -100.0,
  "etfSensitivity": 0.05,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addCollateralRebalancingModel
echo " -> cr01_ied_test loaded"

# ============================================================================
# TEST 4: CorrelationRiskModel
# ============================================================================
echo ""
echo "TEST 4: CorrelationRiskModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "corr01_ied_test",
  "asset1MOC": "ETH_USD",
  "asset2MOC": "BTC_USD",
  "asset1Quantity": 5.0,
  "asset2Quantity": 0.5,
  "correlationThreshold": 0.90,
  "diversificationHaircut": 0.15,
  "baseLtvThreshold": 0.75,
  "liquidationThreshold": 0.83,
  "rollingWindowSize": 10,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addCorrelationRiskModel
echo " -> corr01_ied_test loaded"

# ============================================================================
# TEST 5: CascadeProbabilityModel
# ============================================================================
echo ""
echo "TEST 5: CascadeProbabilityModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "casc01_ied_test",
  "collateralPriceMOC": "ETH_USD",
  "poolAggLtvMOC": "POOL_AGG_LTV",
  "marketDepthMOC": "MARKET_DEPTH_USD",
  "collateralQuantity": 10.0,
  "positionValueUSD": 25000.0,
  "cascadeThreshold": 0.60,
  "priceImpactFactor": 0.001,
  "defensiveRepayFraction": 0.20,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addCascadeProbabilityModel
echo " -> casc01_ied_test loaded"

# ============================================================================
# TEST 6: GasOptimizationModel
# ============================================================================
echo ""
echo "TEST 6: GasOptimizationModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "gas01_ied_test",
  "collateralPriceMOC": "ETH_USD",
  "gasPriceMOC": "GAS_PRICE_GWEI",
  "collateralQuantity": 10.0,
  "gasUnitsPerTx": 250000.0,
  "ltvThreshold": 0.75,
  "liquidationThreshold": 0.83,
  "ltvTarget": 0.65,
  "minBenefitUSD": 50.0,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addGasOptimizationModel
echo " -> gas01_ied_test loaded"

# ============================================================================
# TEST 7: InvoiceMaturityModel
# ============================================================================
echo ""
echo "TEST 7: InvoiceMaturityModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "inv01_ied_test",
  "collateralPriceMOC": "ETH_USD",
  "invoicePaymentProbMOC": "INVOICE_PAYMENT_PROB",
  "collateralQuantity": 5.0,
  "invoiceFaceValue": 10000.0,
  "invoiceMaturityDate": "2025-12-01T00:00:00",
  "overdueDegradationRate": 0.03,
  "creditDiscountRate": 0.05,
  "ltvThreshold": 0.75,
  "liquidationThreshold": 0.83,
  "ltvTarget": 0.65,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addInvoiceMaturityModel
echo " -> inv01_ied_test loaded"

# ============================================================================
# TEST 8: TariffSpreadModel
# ============================================================================
echo ""
echo "TEST 8: TariffSpreadModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "ts01_ied_test",
  "tariffIndexMOC": "TARIFF_INDEX",
  "baseSpread": 0.02,
  "baseTariffIndex": 0.10,
  "baseTariffSensitivity": 0.5,
  "maxSpreadCap": 0.08,
  "armingtonElasticity": 2.8,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addTariffSpreadModel
echo " -> ts01_ied_test loaded"

# ============================================================================
# TEST 9: WorkingCapitalStressModel
# ============================================================================
echo ""
echo "TEST 9: WorkingCapitalStressModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "wcs01_ied_test",
  "tariffIndexMOC": "TARIFF_INDEX",
  "revenueIndexMOC": "REVENUE_INDEX",
  "dsoIndexMOC": "DSO_INDEX",
  "baseDSO": 45.0,
  "baseDIO": 30.0,
  "baseDPO": 35.0,
  "tariffDSOSensitivity": 0.5,
  "tariffDIOSensitivity": 0.3,
  "maxDrawdownFraction": 1.0,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addWorkingCapitalStressModel
echo " -> wcs01_ied_test loaded"

# ============================================================================
# TEST 10: HedgeEffectivenessModel
# ============================================================================
echo ""
echo "TEST 10: HedgeEffectivenessModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "he01_ied_test",
  "tariffIndexMOC": "TARIFF_INDEX",
  "fxRateMOC": "USD_INR_SPOT",
  "hedgedNotional": 5000000.0,
  "currentExposureMOC": "EXPORT_REVENUE_USD",
  "lowerEffectivenessBound": 0.80,
  "upperEffectivenessBound": 1.25,
  "tariffExposureSensitivity": 0.3,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addHedgeEffectivenessModel
echo " -> he01_ied_test loaded"

# ============================================================================
# TEST 11: FXTariffCorrelationModel
# ============================================================================
echo ""
echo "TEST 11: FXTariffCorrelationModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "fxtc01_ied_test",
  "tariffIndexMOC": "TARIFF_INDEX",
  "fxRateMOC": "USD_INR_SPOT",
  "baseFxRate": 83.0,
  "correlationCoefficient": 0.65,
  "fxSensitivity": 0.4,
  "amplificationFactor": 1.3,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addFXTariffCorrelationModel
echo " -> fxtc01_ied_test loaded"

# ============================================================================
# TEST 12: PortCongestionModel
# ============================================================================
echo ""
echo "TEST 12: PortCongestionModel"
curl -s -H 'Content-Type: application/json' --data '{
  "riskFactorId": "pc01_ied_test",
  "portCongestionIndexMOC": "PORT_CONGESTION_INDEX",
  "tariffIndexMOC": "TARIFF_INDEX",
  "baseDwellDays": 5.0,
  "congestionSensitivity": 0.4,
  "maxDelayDays": 30.0,
  "financialImpactPerDay": 0.001,
  "monitoringEventTimes": [
    "2024-06-01T00:00:00","2024-12-01T00:00:00",
    "2025-02-01T00:00:00","2025-06-01T00:00:00","2025-12-01T00:00:00"
  ]
}' http://localhost:${RISK_PORT}/addPortCongestionModel
echo " -> pc01_ied_test loaded"

echo ""
echo "--- Step 2: Creating scenario with all test models ---"

# ============================================================================
# STEP 2: Create scenario referencing all test models
# ============================================================================

# Scenario for TwoDimensionalPrepaymentModel test
curl -s -H 'Content-Type: application/json' --data '{
  "scenarioID": "scn_ied_test_ppm",
  "riskFactorDescriptors":[
    {"riskFactorID":"ust5Y_falling","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"ppm01_ied_test","riskFactorType":"TwoDimensionalPrepaymentModel"}
  ]
}' http://localhost:${RISK_PORT}/addScenario
echo " -> scn_ied_test_ppm scenario created"

# Scenario for DeFi Liquidation models test
curl -s -H 'Content-Type: application/json' --data '{
  "scenarioID": "scn_ied_test_defi",
  "riskFactorDescriptors":[
    {"riskFactorID":"ETH_USD","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"BTC_USD","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"USDC_USD","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"ETH_ETF_FLOW","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"GAS_PRICE_GWEI","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"POOL_AGG_LTV","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"MARKET_DEPTH_USD","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"INVOICE_PAYMENT_PROB","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"cv01_ied_test","riskFactorType":"CollateralVelocityModel"},
    {"riskFactorID":"cr01_ied_test","riskFactorType":"CollateralRebalancingModel"},
    {"riskFactorID":"corr01_ied_test","riskFactorType":"CorrelationRiskModel"},
    {"riskFactorID":"casc01_ied_test","riskFactorType":"CascadeProbabilityModel"},
    {"riskFactorID":"gas01_ied_test","riskFactorType":"GasOptimizationModel"},
    {"riskFactorID":"inv01_ied_test","riskFactorType":"InvoiceMaturityModel"}
  ]
}' http://localhost:${RISK_PORT}/addScenario
echo " -> scn_ied_test_defi scenario created"

# Scenario for Supply Chain Tariff models test
curl -s -H 'Content-Type: application/json' --data '{
  "scenarioID": "scn_ied_test_tariff",
  "riskFactorDescriptors":[
    {"riskFactorID":"TARIFF_INDEX","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"USD_INR_SPOT","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"REVENUE_INDEX","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"DSO_INDEX","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"EXPORT_REVENUE_USD","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"PORT_CONGESTION_INDEX","riskFactorType":"ReferenceIndex"},
    {"riskFactorID":"ts01_ied_test","riskFactorType":"TariffSpreadModel"},
    {"riskFactorID":"wcs01_ied_test","riskFactorType":"WorkingCapitalStressModel"},
    {"riskFactorID":"he01_ied_test","riskFactorType":"HedgeEffectivenessModel"},
    {"riskFactorID":"fxtc01_ied_test","riskFactorType":"FXTariffCorrelationModel"},
    {"riskFactorID":"pc01_ied_test","riskFactorType":"PortCongestionModel"}
  ]
}' http://localhost:${RISK_PORT}/addScenario
echo " -> scn_ied_test_tariff scenario created"

echo ""
echo "--- Step 3: Running scenario simulations ---"

# ============================================================================
# STEP 3: Run simulations — IED is AFTER some monitoring event times
# ============================================================================

# Test 1: PAM contract with prepayment model (IED = 2015-01-02)
echo ""
echo "RUN TEST 1: TwoDimensionalPrepaymentModel PAM (IED=2015-01-02)"
echo "  Expect SKIP: 2014-06-01, 2014-12-01"
echo "  Expect PASS: 2015-03-01, 2015-09-01, 2016-03-01"
curl -s -H 'Content-Type: application/json' --data '{
  "contracts":[{
    "calendar":"WEEKDAY","businessDayConvention":"SCF","contractType":"PAM",
    "statusDate":"2014-01-01T00:00:00","contractRole":"RPA","contractID":"PPM-IED-Test-01",
    "cycleAnchorDateOfInterestPayment":"2016-01-02T00:00:00","cycleOfInterestPayment":"P6ML0",
    "nominalInterestRate":0.02,"dayCountConvention":"30E360","currency":"USD",
    "contractDealDate":"2014-01-01T00:00:00",
    "initialExchangeDate":"2015-01-02T00:00:00",
    "maturityDate":"2020-01-02T00:00:00","notionalPrincipal":1000,"premiumDiscountAtIED":0,
    "cycleAnchorDateOfRateReset":"2015-07-02T00:00:00","cycleOfRateReset":"P1YL1",
    "rateSpread":0.01,"marketObjectCodeOfRateReset":"ust5Y_falling",
    "prepaymentModels":["ppm01_ied_test"]
  }],
  "scenarioDescriptor":{"scenarioID":"scn_ied_test_ppm","scenarioType":"scenario"},
  "simulateTo":"2020-01-01T00:00:00","monitoringTimes":[]
}' http://localhost:${SIM_PORT}/rf2/scenarioSimulation
echo ""

# Test 2: PAM contract with DeFi models (IED = 2025-01-02)
echo ""
echo "RUN TEST 2: DeFi Liquidation models PAM (IED=2025-01-02)"
echo "  Expect SKIP: 2024-06-01, 2024-12-01 for each of 6 models"
echo "  Expect PASS: 2025-02-01, 2025-06-01, 2025-12-01"
curl -s -H 'Content-Type: application/json' --data '{
  "contracts":[{
    "calendar":"WEEKDAY","businessDayConvention":"SCF","contractType":"PAM",
    "statusDate":"2024-01-01T00:00:00","contractRole":"RPA","contractID":"DEFI-IED-Test-01",
    "cycleAnchorDateOfInterestPayment":"2025-07-02T00:00:00","cycleOfInterestPayment":"P6ML0",
    "nominalInterestRate":0.05,"dayCountConvention":"30E360","currency":"USD",
    "contractDealDate":"2024-01-01T00:00:00",
    "initialExchangeDate":"2025-01-02T00:00:00",
    "maturityDate":"2027-01-02T00:00:00","notionalPrincipal":25000,"premiumDiscountAtIED":0
  }],
  "scenarioDescriptor":{"scenarioID":"scn_ied_test_defi","scenarioType":"scenario"},
  "simulateTo":"2027-01-01T00:00:00","monitoringTimes":[]
}' http://localhost:${SIM_PORT}/rf2/scenarioSimulation
echo ""

# Test 3: PAM contract with Supply Chain Tariff models (IED = 2025-01-02)
echo ""
echo "RUN TEST 3: Supply Chain Tariff models PAM (IED=2025-01-02)"
echo "  Expect SKIP: 2024-06-01, 2024-12-01 for each of 5 models"
echo "  Expect PASS: 2025-02-01, 2025-06-01, 2025-12-01"
curl -s -H 'Content-Type: application/json' --data '{
  "contracts":[{
    "calendar":"WEEKDAY","businessDayConvention":"SCF","contractType":"PAM",
    "statusDate":"2024-01-01T00:00:00","contractRole":"RPA","contractID":"TARIFF-IED-Test-01",
    "cycleAnchorDateOfInterestPayment":"2025-07-02T00:00:00","cycleOfInterestPayment":"P6ML0",
    "nominalInterestRate":0.04,"dayCountConvention":"30E360","currency":"USD",
    "contractDealDate":"2024-01-01T00:00:00",
    "initialExchangeDate":"2025-01-02T00:00:00",
    "maturityDate":"2027-01-02T00:00:00","notionalPrincipal":100000,"premiumDiscountAtIED":0
  }],
  "scenarioDescriptor":{"scenarioID":"scn_ied_test_tariff","scenarioType":"scenario"},
  "simulateTo":"2027-01-01T00:00:00","monitoringTimes":[]
}' http://localhost:${SIM_PORT}/rf2/scenarioSimulation
echo ""

echo "============================================"
echo " Tests complete!"
echo ""
echo " VERIFY BY CHECKING DOCKER LOGS:"
echo "   docker compose -f quickstart-docker-actus-rf20.yml logs actus-riskserver-ce | grep 'SKIPPING'"
echo ""
echo " EXPECTED OUTPUT (26 SKIP lines total):"
echo "   2 skips × TwoDimensionalPrepaymentModel"
echo "   2 skips × CollateralVelocityModel"
echo "   2 skips × CollateralRebalancingModel"
echo "   2 skips × CorrelationRiskModel"
echo "   2 skips × CascadeProbabilityModel"
echo "   2 skips × GasOptimizationModel"
echo "   2 skips × InvoiceMaturityModel"
echo "   2 skips × TariffSpreadModel"
echo "   2 skips × WorkingCapitalStressModel"
echo "   2 skips × HedgeEffectivenessModel"
echo "   2 skips × FXTariffCorrelationModel"
echo "   2 skips × PortCongestionModel"
echo "   (TwoDimensionalDepositTrxModel requires label surface - tested via existing deposit test)"
echo ""
echo " Also verify NO pre-IED PP events in simulation output:"
echo "   docker compose -f quickstart-docker-actus-rf20.yml logs actus-riskserver-ce | grep -E 'PP.*2014|PP.*2024-06|PP.*2024-12'"
echo "   (should return ZERO lines)"
echo "============================================"
