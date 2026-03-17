@echo off
echo.
echo ==========================================
echo  ACTUS Risk+ACTUS Service - LOCAL Docker Setup
echo  Both services built from local source
echo  FORCE REBUILD (--no-cache)
echo ==========================================
echo.

echo [1/6] Copying fixed actus-core into actus-service build context...
if exist "%~dp0actus-service\actus-core" rmdir /S /Q "%~dp0actus-service\actus-core"
xcopy /E /I /Q "%~dp0actus-riskservice\actus-core" "%~dp0actus-service\actus-core"
if %errorlevel% neq 0 (
    echo ERROR: Failed to copy actus-core to actus-service.
    pause
    exit /b 1
)
echo    actus-core copied to actus-service build context.

echo.
echo [2/6] Building Docker image from actus-riskservice (port 8082) [NO CACHE]...
docker build --no-cache -t actus-risksrv3-custom:latest "%~dp0actus-riskservice"
if %errorlevel% neq 0 (
    echo ERROR: Docker build for riskservice failed. Make sure Docker Desktop is running.
    pause
    exit /b 1
)

echo.
echo [3/6] Building Docker image from actus-service (port 8083) [NO CACHE]...
docker build --no-cache -t actus-server-rf20-custom:latest "%~dp0actus-service"
if %errorlevel% neq 0 (
    echo ERROR: Docker build for actus-service failed.
    pause
    exit /b 1
)

echo.
echo [4/6] Stopping any existing containers...
docker compose -f "%~dp0actus-docker-networks\quickstart-docker-actus-rf20-local.yml" down

echo.
echo [5/6] Starting containers...
docker compose -f "%~dp0actus-docker-networks\quickstart-docker-actus-rf20-local.yml" up -d
if %errorlevel% neq 0 (
    echo ERROR: Docker compose up failed.
    pause
    exit /b 1
)

echo.
echo [6/6] Fetching logs for actus-riskserver-ce...
docker compose -f "%~dp0actus-docker-networks\quickstart-docker-actus-rf20-local.yml" logs actus-riskserver-ce

echo.
echo ==========================================
echo  Done! Both containers running with
echo  fixed actus-core (SWAPS support).
echo  Port 8082: actus-risksrv3-custom
echo  Port 8083: actus-server-rf20-custom
echo  
echo  BUILT WITH --no-cache (FRESH BUILD)
echo ==========================================
pause
