/**
 * ACTUS StableCoin Simulation Runner — standalone Node.js script
 * 
 * Usage:
 *   node run-simulation.js                        # defaults to localhost
 *   node run-simulation.js --risk http://host:8082 --sim http://host:8083
 *   node run-simulation.js --output results.json
 * 
 * No dependencies required — uses native fetch (Node 18+).
 */

const RISK_HOST = process.argv.includes('--risk') 
  ? process.argv[process.argv.indexOf('--risk') + 1] 
  : 'http://localhost:8082';
const SIM_HOST = process.argv.includes('--sim')
  ? process.argv[process.argv.indexOf('--sim') + 1]
  : 'http://localhost:8083';
const OUTPUT = process.argv.includes('--output')
  ? process.argv[process.argv.indexOf('--output') + 1]
  : 'simulation-result.json';

const fs = require('fs');

// ────────────────────────────────────────────────────
// Payloads extracted from the Postman collection
// ────────────────────────────────────────────────────

const REFERENCE_INDEXES = [
  {
    label: 'SC_TOTAL_RESERVES ($102M → $88M trough)',
    url: '/addReferenceIndex',
    body: {"riskFactorID":"SC_RESERVES_01","marketObjectCode":"SC_TOTAL_RESERVES","base":1.0,"data":[{"time":"2026-03-01T00:00:00","value":102000000},{"time":"2026-03-02T00:00:00","value":101800000},{"time":"2026-03-03T00:00:00","value":101500000},{"time":"2026-03-04T00:00:00","value":101000000},{"time":"2026-03-05T00:00:00","value":100500000},{"time":"2026-03-06T00:00:00","value":100000000},{"time":"2026-03-07T00:00:00","value":99500000},{"time":"2026-03-08T00:00:00","value":99000000},{"time":"2026-03-09T00:00:00","value":98000000},{"time":"2026-03-10T00:00:00","value":97000000},{"time":"2026-03-11T00:00:00","value":95500000},{"time":"2026-03-12T00:00:00","value":94000000},{"time":"2026-03-13T00:00:00","value":92500000},{"time":"2026-03-14T00:00:00","value":91000000},{"time":"2026-03-15T00:00:00","value":89500000},{"time":"2026-03-16T00:00:00","value":88500000},{"time":"2026-03-17T00:00:00","value":88000000},{"time":"2026-03-18T00:00:00","value":88000000},{"time":"2026-03-19T00:00:00","value":88500000},{"time":"2026-03-20T00:00:00","value":89000000},{"time":"2026-03-21T00:00:00","value":90000000},{"time":"2026-03-22T00:00:00","value":91000000},{"time":"2026-03-23T00:00:00","value":92500000},{"time":"2026-03-24T00:00:00","value":94000000},{"time":"2026-03-25T00:00:00","value":95500000},{"time":"2026-03-26T00:00:00","value":97000000},{"time":"2026-03-27T00:00:00","value":98000000},{"time":"2026-03-28T00:00:00","value":99000000},{"time":"2026-03-29T00:00:00","value":100000000},{"time":"2026-03-30T00:00:00","value":100500000}]}
  },
  {
    label: 'SC_CASH_RESERVE ($40.8M → $13.2M trough)',
    url: '/addReferenceIndex',
    body: {"riskFactorID":"SC_CASH_01","marketObjectCode":"SC_CASH_RESERVE","base":1.0,"data":[{"time":"2026-03-01T00:00:00","value":40800000},{"time":"2026-03-02T00:00:00","value":40200000},{"time":"2026-03-03T00:00:00","value":39500000},{"time":"2026-03-04T00:00:00","value":38500000},{"time":"2026-03-05T00:00:00","value":37000000},{"time":"2026-03-06T00:00:00","value":35500000},{"time":"2026-03-07T00:00:00","value":33000000},{"time":"2026-03-08T00:00:00","value":30000000},{"time":"2026-03-09T00:00:00","value":27000000},{"time":"2026-03-10T00:00:00","value":24000000},{"time":"2026-03-11T00:00:00","value":21000000},{"time":"2026-03-12T00:00:00","value":18500000},{"time":"2026-03-13T00:00:00","value":16500000},{"time":"2026-03-14T00:00:00","value":15000000},{"time":"2026-03-15T00:00:00","value":13500000},{"time":"2026-03-16T00:00:00","value":13200000},{"time":"2026-03-17T00:00:00","value":13500000},{"time":"2026-03-18T00:00:00","value":14000000},{"time":"2026-03-19T00:00:00","value":15000000},{"time":"2026-03-20T00:00:00","value":17000000},{"time":"2026-03-21T00:00:00","value":19000000},{"time":"2026-03-22T00:00:00","value":22000000},{"time":"2026-03-23T00:00:00","value":25000000},{"time":"2026-03-24T00:00:00","value":28000000},{"time":"2026-03-25T00:00:00","value":31000000},{"time":"2026-03-26T00:00:00","value":33000000},{"time":"2026-03-27T00:00:00","value":35000000},{"time":"2026-03-28T00:00:00","value":36500000},{"time":"2026-03-29T00:00:00","value":37500000},{"time":"2026-03-30T00:00:00","value":38000000}]}
  },
  {
    label: 'STABLECOIN_PEG_DEV (0% → 5% peak)',
    url: '/addReferenceIndex',
    body: {"riskFactorID":"SC_PEG_DEV_01","marketObjectCode":"STABLECOIN_PEG_DEV","base":1.0,"data":[{"time":"2026-03-01T00:00:00","value":0.0},{"time":"2026-03-02T00:00:00","value":0.0},{"time":"2026-03-03T00:00:00","value":0.001},{"time":"2026-03-04T00:00:00","value":0.001},{"time":"2026-03-05T00:00:00","value":0.002},{"time":"2026-03-06T00:00:00","value":0.002},{"time":"2026-03-07T00:00:00","value":0.003},{"time":"2026-03-08T00:00:00","value":0.004},{"time":"2026-03-09T00:00:00","value":0.005},{"time":"2026-03-10T00:00:00","value":0.008},{"time":"2026-03-11T00:00:00","value":0.012},{"time":"2026-03-12T00:00:00","value":0.018},{"time":"2026-03-13T00:00:00","value":0.025},{"time":"2026-03-14T00:00:00","value":0.035},{"time":"2026-03-15T00:00:00","value":0.050},{"time":"2026-03-16T00:00:00","value":0.048},{"time":"2026-03-17T00:00:00","value":0.042},{"time":"2026-03-18T00:00:00","value":0.035},{"time":"2026-03-19T00:00:00","value":0.025},{"time":"2026-03-20T00:00:00","value":0.018},{"time":"2026-03-21T00:00:00","value":0.012},{"time":"2026-03-22T00:00:00","value":0.008},{"time":"2026-03-23T00:00:00","value":0.005},{"time":"2026-03-24T00:00:00","value":0.003},{"time":"2026-03-25T00:00:00","value":0.002},{"time":"2026-03-26T00:00:00","value":0.001},{"time":"2026-03-27T00:00:00","value":0.001},{"time":"2026-03-28T00:00:00","value":0.0},{"time":"2026-03-29T00:00:00","value":0.0},{"time":"2026-03-30T00:00:00","value":0.0}]}
  }
];

const BACKING_RATIO_MODEL = {"riskFactorId":"br_sc01","totalReservesMOC":"SC_TOTAL_RESERVES","cashReserveMOC":"SC_CASH_RESERVE","backingThreshold":1.0,"liquidityThreshold":0.35,"monitoringEventTimes":["2026-03-01T00:00:00","2026-03-02T00:00:00","2026-03-03T00:00:00","2026-03-04T00:00:00","2026-03-05T00:00:00","2026-03-06T00:00:00","2026-03-07T00:00:00","2026-03-08T00:00:00","2026-03-09T00:00:00","2026-03-10T00:00:00","2026-03-11T00:00:00","2026-03-12T00:00:00","2026-03-13T00:00:00","2026-03-14T00:00:00","2026-03-15T00:00:00","2026-03-16T00:00:00","2026-03-17T00:00:00","2026-03-18T00:00:00","2026-03-19T00:00:00","2026-03-20T00:00:00","2026-03-21T00:00:00","2026-03-22T00:00:00","2026-03-23T00:00:00","2026-03-24T00:00:00","2026-03-25T00:00:00","2026-03-26T00:00:00","2026-03-27T00:00:00","2026-03-28T00:00:00","2026-03-29T00:00:00","2026-03-30T00:00:00"]};

const REDEMPTION_PRESSURE_MODEL = {"riskFactorId":"rp_sc01","pegDeviationMOC":"STABLECOIN_PEG_DEV","cashReserveMOC":"SC_CASH_RESERVE","pegDeviationThreshold":0.005,"monitoringEventTimes":["2026-03-01T00:00:00","2026-03-02T00:00:00","2026-03-03T00:00:00","2026-03-04T00:00:00","2026-03-05T00:00:00","2026-03-06T00:00:00","2026-03-07T00:00:00","2026-03-08T00:00:00","2026-03-09T00:00:00","2026-03-10T00:00:00","2026-03-11T00:00:00","2026-03-12T00:00:00","2026-03-13T00:00:00","2026-03-14T00:00:00","2026-03-15T00:00:00","2026-03-16T00:00:00","2026-03-17T00:00:00","2026-03-18T00:00:00","2026-03-19T00:00:00","2026-03-20T00:00:00","2026-03-21T00:00:00","2026-03-22T00:00:00","2026-03-23T00:00:00","2026-03-24T00:00:00","2026-03-25T00:00:00","2026-03-26T00:00:00","2026-03-27T00:00:00","2026-03-28T00:00:00","2026-03-29T00:00:00","2026-03-30T00:00:00"]};

const SCENARIO = {"scenarioID":"sc_depeg_stress_scn01","riskFactorDescriptors":[{"riskFactorID":"SC_RESERVES_01","riskFactorType":"ReferenceIndex"},{"riskFactorID":"SC_CASH_01","riskFactorType":"ReferenceIndex"},{"riskFactorID":"SC_PEG_DEV_01","riskFactorType":"ReferenceIndex"},{"riskFactorID":"br_sc01","riskFactorType":"BackingRatioModel"},{"riskFactorID":"rp_sc01","riskFactorType":"RedemptionPressureModel"}]};

const SIMULATION_REQUEST = {"contracts":[{"calendar":"NC","businessDayConvention":"SCF","contractType":"PAM","statusDate":"2026-02-28T00:00:00","contractRole":"RPA","contractID":"StableCoinA-Liability-01","cycleAnchorDateOfInterestPayment":"2026-03-31T00:00:00","cycleOfInterestPayment":"P1ML0","nominalInterestRate":0.0,"dayCountConvention":"30E360","currency":"USD","contractDealDate":"2026-02-28T00:00:00","initialExchangeDate":"2026-03-01T00:00:00","maturityDate":"2026-03-31T00:00:00","notionalPrincipal":100000000,"premiumDiscountAtIED":0,"stablecoinModels":["br_sc01","rp_sc01"]}],"scenarioDescriptor":{"scenarioID":"sc_depeg_stress_scn01","scenarioType":"scenario"},"simulateTo":"2026-03-31T00:00:00","monitoringTimes":[]};

// ────────────────────────────────────────────────────
// Runner
// ────────────────────────────────────────────────────

async function post(host, path, body, label) {
  console.log(`  [${label}] POST ${host}${path}`);
  const res = await fetch(`${host}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${label} failed: ${res.status} ${text}`);
  console.log(`    ✅ ${text.trim()}`);
  return text;
}

async function get(host, path, label) {
  console.log(`  [${label}] GET ${host}${path}`);
  const res = await fetch(`${host}${path}`);
  if (!res.ok) throw new Error(`${label} failed: ${res.status}`);
  return res.json();
}

async function run() {
  console.log('\n══════════════════════════════════════════════════════');
  console.log('  STABLECOIN DE-PEG STRESS SIMULATION');
  console.log(`  Risk: ${RISK_HOST}  Sim: ${SIM_HOST}`);
  console.log('══════════════════════════════════════════════════════\n');

  // 1-3: Reference indexes
  for (let i = 0; i < REFERENCE_INDEXES.length; i++) {
    const ri = REFERENCE_INDEXES[i];
    await post(RISK_HOST, ri.url, ri.body, `${i+1}. ${ri.label}`);
  }

  // 4-5: Behavioral models
  await post(RISK_HOST, '/addBackingRatioModel', BACKING_RATIO_MODEL, '4. BackingRatioModel');
  await post(RISK_HOST, '/addRedemptionPressureModel', REDEMPTION_PRESSURE_MODEL, '5. RedemptionPressureModel');

  // 6: Scenario
  await post(RISK_HOST, '/addScenario', SCENARIO, '6. Scenario');

  // 7: Verify
  const br = await get(RISK_HOST, '/findBackingRatioModel/br_sc01', '7a. Verify BR');
  console.log(`    backingThreshold=${br.backingThreshold}, liquidityThreshold=${br.liquidityThreshold}`);
  const rp = await get(RISK_HOST, '/findRedemptionPressureModel/rp_sc01', '7b. Verify RP');
  console.log(`    pegDeviationThreshold=${rp.pegDeviationThreshold}`);

  // 8: Simulate!
  console.log('\n  [8. SIMULATION] POST to rf2/scenarioSimulation ...');
  const simRes = await fetch(`${SIM_HOST}/rf2/scenarioSimulation`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(SIMULATION_REQUEST)
  });
  const result = await simRes.json();

  // Save
  fs.writeFileSync(OUTPUT, JSON.stringify(result, null, 2));
  console.log(`\n  ✅ Results saved to ${OUTPUT}`);

  // Summary
  const events = result[0].events;
  const ppWithPayoff = events.filter(e => e.type === 'PP' && e.payoff > 0);
  const md = events.find(e => e.type === 'MD');
  const loss = 100000000 - Math.abs(md.payoff);

  console.log('\n══════════════════════════════════════════════════════');
  console.log(`  Total events: ${events.length}`);
  console.log(`  Redemption events (payoff > 0): ${ppWithPayoff.length}`);
  console.log(`  Final principal: $${md.payoff.toFixed(2)}`);
  console.log(`  Supply reduction: $${loss.toFixed(2)} (${(loss/1e6).toFixed(1)}M)`);
  console.log('══════════════════════════════════════════════════════\n');

  return result;
}

run().catch(err => {
  console.error('❌ Simulation failed:', err.message);
  process.exit(1);
});
