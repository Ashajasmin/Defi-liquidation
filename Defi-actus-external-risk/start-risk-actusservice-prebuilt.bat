@echo off
echo.
echo ==========================================
echo  ACTUS Risk Service - Docker Setup
echo ==========================================
echo.

echo [1/4] Building Docker image from actus-riskservice...
docker build -t actus-risksrv3-custom:latest "%~dp0actus-riskservice"
if %errorlevel% neq 0 (
    echo ERROR: Docker build failed. Make sure Docker Desktop is running.
    pause
    exit /b 1
)

echo.
echo [2/4] Stopping any existing containers...
docker compose -f "%~dp0actus-docker-networks\quickstart-docker-actus-rf20.yml" down

echo.
echo [3/4] Starting containers...
docker compose -f "%~dp0actus-docker-networks\quickstart-docker-actus-rf20.yml" up -d
if %errorlevel% neq 0 (
    echo ERROR: Docker compose up failed.
    pause
    exit /b 1
)

echo.
echo [4/4] Fetching logs for actus-riskserver-ce...
docker compose -f "%~dp0actus-docker-networks\quickstart-docker-actus-rf20.yml" logs actus-riskserver-ce

echo.
echo ==========================================
echo  Done! Containers are running.
echo ==========================================
pause