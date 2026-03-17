# Running ACTUS Simulations from Claude Code

## Overview
Claude Code can run your Postman collections by translating them into `curl` commands 
or by executing the Node.js runner script.

## Method 1: Direct curl execution

Tell Claude Code:

```
Run the StableCoin-BackingRatio-RedemptionPressure-30d simulation against localhost:8082 (risk) and localhost:8083 (simulation).
The Postman collection is at: actus-riskservice/simulations/StableCoin-BackingRatio-RedemptionPressure-30d.json
Execute each request sequentially and save the final simulation result.
```

Claude Code will parse the Postman collection and execute equivalent curl commands:

```bash
# Step 1: Add reference indexes
curl -s -X POST http://localhost:8082/addReferenceIndex \
  -H "Content-Type: application/json" \
  -d '{"riskFactorID":"SC_RESERVES_01",...}'

# ... steps 2-7 ...

# Step 8: Run simulation
curl -s -X POST http://localhost:8083/rf2/scenarioSimulation \
  -H "Content-Type: application/json" \
  -d '{"contracts":[...],...}' \
  -o simulation-result.json
```

## Method 2: Run the Node.js script

```
cd actus-riskservice/runners
node run-simulation.js --output ../results/latest.json
```

## Method 3: Use the API server

```bash
# Terminal 1: Start API server
cd actus-riskservice/runners
npm init -y && npm install express cors
node simulation-api-server.js

# Terminal 2: Call it
curl -X POST http://localhost:3001/api/run-simulation \
  -H "Content-Type: application/json" \
  -d '{"collection":"StableCoin-BackingRatio-RedemptionPressure-30d.json"}'
```

## Method 4: Use Newman (see newman instructions)

```bash
cd actus-riskservice/runners
npx newman run ../simulations/StableCoin-BackingRatio-RedemptionPressure-30d.json \
  --reporters cli,json \
  --reporter-json-export newman-result.json
```

## Claude Code MCP Integration

If you have Claude Code set up as an MCP server, you can add a tool definition:

```json
{
  "name": "run_actus_simulation",
  "description": "Run an ACTUS financial simulation collection",
  "input_schema": {
    "type": "object",
    "properties": {
      "collection": {
        "type": "string",
        "description": "Name of the Postman collection JSON file in simulations/"
      },
      "risk_host": { "type": "string", "default": "http://localhost:8082" },
      "sim_host": { "type": "string", "default": "http://localhost:8083" }
    },
    "required": ["collection"]
  }
}
```

Then Claude Code can call:
```
Use the run_actus_simulation tool with collection="StableCoin-BackingRatio-RedemptionPressure-30d.json"
```
