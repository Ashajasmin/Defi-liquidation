#!/usr/bin/env pwsh
# ============================================================================
# StableCoin BackingRatio + RedemptionPressure 30-day Stress Simulation
# Usage: .\run-simulation.ps1 [-RiskHost http://localhost:8082] [-SimHost http://localhost:8083]
# ============================================================================
param(
    [string]$RiskHost = "http://localhost:8082",
    [string]$SimHost  = "http://localhost:8083",
    [string]$OutputFile = ""
)

$ErrorActionPreference = "Stop"
$headers = @{ "Content-Type" = "application/json" }

function Post-Json($url, $body, $label) {
    Write-Host "  [$label] POST $url ..." -ForegroundColor Cyan
    $resp = Invoke-RestMethod -Uri $url -Method POST -Headers $headers -Body $body
    Write-Host "    -> $resp" -ForegroundColor Green
    return $resp
}

function Get-Json($url, $label) {
    Write-Host "  [$label] GET $url ..." -ForegroundColor Cyan
    $resp = Invoke-RestMethod -Uri $url -Method GET
    return $resp
}

Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "  STABLECOIN DE-PEG STRESS SIMULATION RUNNER" -ForegroundColor Yellow
Write-Host "  Risk Service: $RiskHost   Simulation: $SimHost" -ForegroundColor Yellow
Write-Host "════════════════════════════════════════════════════════════`n" -ForegroundColor Yellow

# ── Step 1: Add Reference Indexes ──
Write-Host "[1/8] Adding SC_TOTAL_RESERVES..." -ForegroundColor White
Post-Json "$RiskHost/addReferenceIndex" '{"riskFactorID":"SC_RESERVES_01","marketObjectCode":"SC_TOTAL_RESERVES","base":1.0,"data":[{"time":"2026-03-01T00:00:00","value":102000000},{"time":"2026-03-02T00:00:00","value":101800000},{"time":"2026-03-03T00:00:00","value":101500000},{"time":"2026-03-04T00:00:00","value":101000000},{"time":"2026-03-05T00:00:00","value":100500000},{"time":"2026-03-06T00:00:00","value":100000000},{"time":"2026-03-07T00:00:00","value":99500000},{"time":"2026-03-08T00:00:00","value":99000000},{"time":"2026-03-09T00:00:00","value":98000000},{"time":"2026-03-10T00:00:00","value":97000000},{"time":"2026-03-11T00:00:00","value":95500000},{"time":"2026-03-12T00:00:00","value":94000000},{"time":"2026-03-13T00:00:00","value":92500000},{"time":"2026-03-14T00:00:00","value":91000000},{"time":"2026-03-15T00:00:00","value":89500000},{"time":"2026-03-16T00:00:00","value":88500000},{"time":"2026-03-17T00:00:00","value":88000000},{"time":"2026-03-18T00:00:00","value":88000000},{"time":"2026-03-19T00:00:00","value":88500000},{"time":"2026-03-20T00:00:00","value":89000000},{"time":"2026-03-21T00:00:00","value":90000000},{"time":"2026-03-22T00:00:00","value":91000000},{"time":"2026-03-23T00:00:00","value":92500000},{"time":"2026-03-24T00:00:00","value":94000000},{"time":"2026-03-25T00:00:00","value":95500000},{"time":"2026-03-26T00:00:00","value":97000000},{"time":"2026-03-27T00:00:00","value":98000000},{"time":"2026-03-28T00:00:00","value":99000000},{"time":"2026-03-29T00:00:00","value":100000000},{"time":"2026-03-30T00:00:00","value":100500000}]}' "SC_RESERVES"

Write-Host "[2/8] Adding SC_CASH_RESERVE..." -ForegroundColor White
Post-Json "$RiskHost/addReferenceIndex" '{"riskFactorID":"SC_CASH_01","marketObjectCode":"SC_CASH_RESERVE","base":1.0,"data":[{"time":"2026-03-01T00:00:00","value":40800000},{"time":"2026-03-02T00:00:00","value":40200000},{"time":"2026-03-03T00:00:00","value":39500000},{"time":"2026-03-04T00:00:00","value":38500000},{"time":"2026-03-05T00:00:00","value":37000000},{"time":"2026-03-06T00:00:00","value":35500000},{"time":"2026-03-07T00:00:00","value":33000000},{"time":"2026-03-08T00:00:00","value":30000000},{"time":"2026-03-09T00:00:00","value":27000000},{"time":"2026-03-10T00:00:00","value":24000000},{"time":"2026-03-11T00:00:00","value":21000000},{"time":"2026-03-12T00:00:00","value":18500000},{"time":"2026-03-13T00:00:00","value":16500000},{"time":"2026-03-14T00:00:00","value":15000000},{"time":"2026-03-15T00:00:00","value":13500000},{"time":"2026-03-16T00:00:00","value":13200000},{"time":"2026-03-17T00:00:00","value":13500000},{"time":"2026-03-18T00:00:00","value":14000000},{"time":"2026-03-19T00:00:00","value":15000000},{"time":"2026-03-20T00:00:00","value":17000000},{"time":"2026-03-21T00:00:00","value":19000000},{"time":"2026-03-22T00:00:00","value":22000000},{"time":"2026-03-23T00:00:00","value":25000000},{"time":"2026-03-24T00:00:00","value":28000000},{"time":"2026-03-25T00:00:00","value":31000000},{"time":"2026-03-26T00:00:00","value":33000000},{"time":"2026-03-27T00:00:00","value":35000000},{"time":"2026-03-28T00:00:00","value":36500000},{"time":"2026-03-29T00:00:00","value":37500000},{"time":"2026-03-30T00:00:00","value":38000000}]}' "SC_CASH"

Write-Host "[3/8] Adding STABLECOIN_PEG_DEV..." -ForegroundColor White
Post-Json "$RiskHost/addReferenceIndex" '{"riskFactorID":"SC_PEG_DEV_01","marketObjectCode":"STABLECOIN_PEG_DEV","base":1.0,"data":[{"time":"2026-03-01T00:00:00","value":0.0},{"time":"2026-03-02T00:00:00","value":0.0},{"time":"2026-03-03T00:00:00","value":0.001},{"time":"2026-03-04T00:00:00","value":0.001},{"time":"2026-03-05T00:00:00","value":0.002},{"time":"2026-03-06T00:00:00","value":0.002},{"time":"2026-03-07T00:00:00","value":0.003},{"time":"2026-03-08T00:00:00","value":0.004},{"time":"2026-03-09T00:00:00","value":0.005},{"time":"2026-03-10T00:00:00","value":0.008},{"time":"2026-03-11T00:00:00","value":0.012},{"time":"2026-03-12T00:00:00","value":0.018},{"time":"2026-03-13T00:00:00","value":0.025},{"time":"2026-03-14T00:00:00","value":0.035},{"time":"2026-03-15T00:00:00","value":0.050},{"time":"2026-03-16T00:00:00","value":0.048},{"time":"2026-03-17T00:00:00","value":0.042},{"time":"2026-03-18T00:00:00","value":0.035},{"time":"2026-03-19T00:00:00","value":0.025},{"time":"2026-03-20T00:00:00","value":0.018},{"time":"2026-03-21T00:00:00","value":0.012},{"time":"2026-03-22T00:00:00","value":0.008},{"time":"2026-03-23T00:00:00","value":0.005},{"time":"2026-03-24T00:00:00","value":0.003},{"time":"2026-03-25T00:00:00","value":0.002},{"time":"2026-03-26T00:00:00","value":0.001},{"time":"2026-03-27T00:00:00","value":0.001},{"time":"2026-03-28T00:00:00","value":0.0},{"time":"2026-03-29T00:00:00","value":0.0},{"time":"2026-03-30T00:00:00","value":0.0}]}' "PEG_DEV"

# ── Step 2: Add Behavioral Models ──
Write-Host "[4/8] Adding BackingRatioModel..." -ForegroundColor White
Post-Json "$RiskHost/addBackingRatioModel" '{"riskFactorId":"br_sc01","totalReservesMOC":"SC_TOTAL_RESERVES","cashReserveMOC":"SC_CASH_RESERVE","backingThreshold":1.0,"liquidityThreshold":0.35,"monitoringEventTimes":["2026-03-01T00:00:00","2026-03-02T00:00:00","2026-03-03T00:00:00","2026-03-04T00:00:00","2026-03-05T00:00:00","2026-03-06T00:00:00","2026-03-07T00:00:00","2026-03-08T00:00:00","2026-03-09T00:00:00","2026-03-10T00:00:00","2026-03-11T00:00:00","2026-03-12T00:00:00","2026-03-13T00:00:00","2026-03-14T00:00:00","2026-03-15T00:00:00","2026-03-16T00:00:00","2026-03-17T00:00:00","2026-03-18T00:00:00","2026-03-19T00:00:00","2026-03-20T00:00:00","2026-03-21T00:00:00","2026-03-22T00:00:00","2026-03-23T00:00:00","2026-03-24T00:00:00","2026-03-25T00:00:00","2026-03-26T00:00:00","2026-03-27T00:00:00","2026-03-28T00:00:00","2026-03-29T00:00:00","2026-03-30T00:00:00"]}' "BR_MODEL"

Write-Host "[5/8] Adding RedemptionPressureModel..." -ForegroundColor White
Post-Json "$RiskHost/addRedemptionPressureModel" '{"riskFactorId":"rp_sc01","pegDeviationMOC":"STABLECOIN_PEG_DEV","cashReserveMOC":"SC_CASH_RESERVE","pegDeviationThreshold":0.005,"monitoringEventTimes":["2026-03-01T00:00:00","2026-03-02T00:00:00","2026-03-03T00:00:00","2026-03-04T00:00:00","2026-03-05T00:00:00","2026-03-06T00:00:00","2026-03-07T00:00:00","2026-03-08T00:00:00","2026-03-09T00:00:00","2026-03-10T00:00:00","2026-03-11T00:00:00","2026-03-12T00:00:00","2026-03-13T00:00:00","2026-03-14T00:00:00","2026-03-15T00:00:00","2026-03-16T00:00:00","2026-03-17T00:00:00","2026-03-18T00:00:00","2026-03-19T00:00:00","2026-03-20T00:00:00","2026-03-21T00:00:00","2026-03-22T00:00:00","2026-03-23T00:00:00","2026-03-24T00:00:00","2026-03-25T00:00:00","2026-03-26T00:00:00","2026-03-27T00:00:00","2026-03-28T00:00:00","2026-03-29T00:00:00","2026-03-30T00:00:00"]}' "RP_MODEL"

# ── Step 3: Add Scenario ──
Write-Host "[6/8] Adding Scenario..." -ForegroundColor White
Post-Json "$RiskHost/addScenario" '{"scenarioID":"sc_depeg_stress_scn01","riskFactorDescriptors":[{"riskFactorID":"SC_RESERVES_01","riskFactorType":"ReferenceIndex"},{"riskFactorID":"SC_CASH_01","riskFactorType":"ReferenceIndex"},{"riskFactorID":"SC_PEG_DEV_01","riskFactorType":"ReferenceIndex"},{"riskFactorID":"br_sc01","riskFactorType":"BackingRatioModel"},{"riskFactorID":"rp_sc01","riskFactorType":"RedemptionPressureModel"}]}' "SCENARIO"

# ── Step 4: Verify ──
Write-Host "[7/8] Verifying models..." -ForegroundColor White
$br = Get-Json "$RiskHost/findBackingRatioModel/br_sc01" "VERIFY_BR"
Write-Host "    BackingRatio: thresh=$($br.backingThreshold) liq=$($br.liquidityThreshold)" -ForegroundColor Green
$rp = Get-Json "$RiskHost/findRedemptionPressureModel/rp_sc01" "VERIFY_RP"
Write-Host "    RedemptionPressure: pegThresh=$($rp.pegDeviationThreshold)" -ForegroundColor Green

# ── Step 5: Run Simulation ──
Write-Host "[8/8] Running simulation..." -ForegroundColor White
$simBody = '{"contracts":[{"calendar":"NC","businessDayConvention":"SCF","contractType":"PAM","statusDate":"2026-02-28T00:00:00","contractRole":"RPA","contractID":"StableCoinA-Liability-01","cycleAnchorDateOfInterestPayment":"2026-03-31T00:00:00","cycleOfInterestPayment":"P1ML0","nominalInterestRate":0.0,"dayCountConvention":"30E360","currency":"USD","contractDealDate":"2026-02-28T00:00:00","initialExchangeDate":"2026-03-01T00:00:00","maturityDate":"2026-03-31T00:00:00","notionalPrincipal":100000000,"premiumDiscountAtIED":0,"stablecoinModels":["br_sc01","rp_sc01"]}],"scenarioDescriptor":{"scenarioID":"sc_depeg_stress_scn01","scenarioType":"scenario"},"simulateTo":"2026-03-31T00:00:00","monitoringTimes":[]}'

$result = Invoke-RestMethod -Uri "$SimHost/rf2/scenarioSimulation" -Method POST -Headers $headers -Body $simBody

# ── Output ──
$json = $result | ConvertTo-Json -Depth 10
if ($OutputFile) {
    $json | Out-File -FilePath $OutputFile -Encoding UTF8
    Write-Host "`nResults saved to: $OutputFile" -ForegroundColor Green
} else {
    $outPath = Join-Path $PSScriptRoot "simulation-result.json"
    $json | Out-File -FilePath $outPath -Encoding UTF8
    Write-Host "`nResults saved to: $outPath" -ForegroundColor Green
}

# ── Summary ──
$events = $result[0].events
$ppEvents = $events | Where-Object { $_.type -eq "PP" -and $_.payoff -gt 0 }
$mdEvent = $events | Where-Object { $_.type -eq "MD" } | Select-Object -First 1

Write-Host "`n════════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "  SIMULATION COMPLETE" -ForegroundColor Yellow
Write-Host "  Total events: $($events.Count)" -ForegroundColor White
Write-Host "  Redemption events (payoff > 0): $($ppEvents.Count)" -ForegroundColor White
Write-Host "  Final principal at maturity: `$$([math]::Round($mdEvent.payoff, 2))" -ForegroundColor White
$loss = 100000000 - [math]::Abs($mdEvent.payoff)
Write-Host "  Supply reduction: `$$([math]::Round($loss, 2)) ($([math]::Round($loss/1000000, 1))%)" -ForegroundColor Red
Write-Host "════════════════════════════════════════════════════════════" -ForegroundColor Yellow
