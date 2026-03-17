@echo off
REM ============================================================================
REM  CLEAN BUILD & RUN - Hybrid Treasury Multi-Contract Portfolio
REM  Builds BOTH actus-service (8083) and actus-riskservice (8082) from source
REM ============================================================================

echo.
echo ============================================================
echo  Step 0: Tear down any running containers
echo ============================================================
docker compose -f quickstart-docker-actus-rf20-local.yml down 2>nul
docker compose -f quickstart-docker-actus-rf20.yml down 2>nul

echo.
echo ============================================================
echo  Step 1: Fix Docker BuildKit cache corruption
echo ============================================================
docker builder prune -f

echo.
echo ============================================================
echo  Step 2: Copy actus-core into actus-service build context
echo ============================================================
echo  actus-core lives in actus-riskservice\actus-core
echo  actus-service\Dockerfile needs it in its own directory
echo.
if exist "..\actus-service\actus-core" (
    echo  Removing stale actus-core copy...
    rmdir /S /Q "..\actus-service\actus-core"
)
echo  Copying actus-core...
xcopy /E /I /Q "..\actus-riskservice\actus-core" "..\actus-service\actus-core"
echo  Done.

echo.
echo ============================================================
echo  Step 3: Build both images from source (no cache)
echo ============================================================
echo.
echo  Building actus-risksrv3-custom (riskservice, port 8082)...
docker build --no-cache -t actus-risksrv3-custom:latest ..\actus-riskservice
if %ERRORLEVEL% NEQ 0 (
    echo  ERROR: riskservice build failed!
    pause
    exit /b 1
)

echo.
echo  Building actus-server-rf20-custom (actus-service, port 8083)...
docker build --no-cache -t actus-server-rf20-custom:latest ..\actus-service
if %ERRORLEVEL% NEQ 0 (
    echo  ERROR: actus-service build failed!
    pause
    exit /b 1
)

echo.
echo ============================================================
echo  Step 4: Start all services
echo ============================================================
docker compose -f quickstart-docker-actus-rf20-local.yml up -d

echo.
echo ============================================================
echo  Step 5: Wait for services to start
echo ============================================================
echo  Waiting 30s for Gradle daemons to initialize...
timeout /t 30 /nobreak

echo.
echo ============================================================
echo  Step 6: Check logs
echo ============================================================
docker compose -f quickstart-docker-actus-rf20-local.yml logs --tail=20

echo.
echo ============================================================
echo  DONE - Services running:
echo    8082 = actus-riskservice (risksrv3, behavioral models)
echo    8083 = actus-service (ACTUS core engine, STF_PP_rf2 FIX)
echo    27018 = MongoDB
echo  Run Postman collection now.
echo ============================================================
pause
