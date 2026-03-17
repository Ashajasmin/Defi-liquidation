/**
 * ACTUS Simulation REST API Server
 * 
 * Wraps the ACTUS risk/simulation services behind a single endpoint.
 * 
 * Setup:
 *   npm init -y && npm install express cors
 *   node simulation-api-server.js
 * 
 * Endpoints:
 *   POST /api/run-simulation     — Run the full 8-step pipeline, returns JSON result
 *   GET  /api/health              — Health check
 *   GET  /api/simulations         — List available simulation collections
 * 
 * The response from /api/run-simulation is the same JSON array you see in Postman.
 */

const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

const RISK_HOST = process.env.RISK_HOST || 'http://localhost:8082';
const SIM_HOST  = process.env.SIM_HOST  || 'http://localhost:8083';
const PORT      = process.env.PORT      || 3001;

// ── Helper ──
async function postJson(host, endpoint, body) {
  const res = await fetch(`${host}${endpoint}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`POST ${endpoint} → ${res.status}: ${text}`);
  return text;
}

async function getJson(host, endpoint) {
  const res = await fetch(`${host}${endpoint}`);
  if (!res.ok) throw new Error(`GET ${endpoint} → ${res.status}`);
  return res.json();
}

// ── Load Postman collection and extract payloads ──
function parseCollection(collectionPath) {
  const col = JSON.parse(fs.readFileSync(collectionPath, 'utf8'));
  const steps = col.item.map(item => ({
    name: item.name,
    method: item.request.method,
    url: item.request.url.raw,
    body: item.request.body ? JSON.parse(item.request.body.raw) : null
  }));
  return steps;
}

// ── Run a Postman collection file programmatically ──
async function runCollection(collectionPath) {
  const steps = parseCollection(collectionPath);
  const log = [];
  let simulationResult = null;

  for (const step of steps) {
    const host = step.url.includes(':8083') ? SIM_HOST : RISK_HOST;
    const urlPath = '/' + step.url.split('/').slice(3).join('/');
    
    try {
      if (step.method === 'POST') {
        const resp = await postJson(host, urlPath, step.body);
        log.push({ step: step.name, status: 'ok', response: resp.trim() });
        
        // If this is the simulation step, parse the JSON response
        if (urlPath.includes('scenarioSimulation')) {
          simulationResult = JSON.parse(resp);
        }
      } else {
        const resp = await getJson(host, urlPath);
        log.push({ step: step.name, status: 'ok', response: resp });
      }
    } catch (err) {
      log.push({ step: step.name, status: 'error', error: err.message });
      throw err;
    }
  }

  return { log, simulationResult };
}

// ── Endpoints ──

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', riskHost: RISK_HOST, simHost: SIM_HOST });
});

app.get('/api/simulations', (req, res) => {
  const simDir = path.join(__dirname, '..', 'simulations');
  const files = fs.readdirSync(simDir).filter(f => f.endsWith('.json'));
  res.json(files);
});

// Run a specific simulation collection
app.post('/api/run-simulation', async (req, res) => {
  try {
    const collectionFile = req.body.collection || 'StableCoin-BackingRatio-RedemptionPressure-30d.json';
    const collectionPath = path.join(__dirname, '..', 'simulations', collectionFile);
    
    if (!fs.existsSync(collectionPath)) {
      return res.status(404).json({ error: `Collection not found: ${collectionFile}` });
    }

    console.log(`Running: ${collectionFile}`);
    const { log, simulationResult } = await runCollection(collectionPath);
    
    res.json({
      collection: collectionFile,
      stepsCompleted: log.length,
      log,
      result: simulationResult
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// Run simulation with custom parameters (override scenario inputs)
app.post('/api/run-custom-simulation', async (req, res) => {
  try {
    const { riskFactors, models, scenario, contract } = req.body;
    const log = [];

    // Add risk factors
    if (riskFactors) {
      for (const rf of riskFactors) {
        await postJson(RISK_HOST, '/addReferenceIndex', rf);
        log.push({ step: `ReferenceIndex: ${rf.riskFactorID}`, status: 'ok' });
      }
    }

    // Add models
    if (models?.backingRatio) {
      await postJson(RISK_HOST, '/addBackingRatioModel', models.backingRatio);
      log.push({ step: 'BackingRatioModel', status: 'ok' });
    }
    if (models?.redemptionPressure) {
      await postJson(RISK_HOST, '/addRedemptionPressureModel', models.redemptionPressure);
      log.push({ step: 'RedemptionPressureModel', status: 'ok' });
    }

    // Add scenario
    if (scenario) {
      await postJson(RISK_HOST, '/addScenario', scenario);
      log.push({ step: 'Scenario', status: 'ok' });
    }

    // Run simulation
    const simRes = await fetch(`${SIM_HOST}/rf2/scenarioSimulation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(contract)
    });
    const result = await simRes.json();
    log.push({ step: 'Simulation', status: 'ok' });

    res.json({ log, result });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`\n🚀 ACTUS Simulation API running on http://localhost:${PORT}`);
  console.log(`   Risk service: ${RISK_HOST}`);
  console.log(`   Sim service:  ${SIM_HOST}`);
  console.log(`\n   POST /api/run-simulation  { "collection": "StableCoin-BackingRatio-RedemptionPressure-30d.json" }`);
  console.log(`   GET  /api/simulations`);
  console.log(`   GET  /api/health\n`);
});
