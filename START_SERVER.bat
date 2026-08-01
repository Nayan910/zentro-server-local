@echo off
title Zentro Server - Rajkot [MONITORING MODE]
color 0A

echo.
echo  ╔══════════════════════════════════════════════════════════════╗
echo  ║           ZENTRO SERVER - LOCAL NETWORK SERVER              ║
echo  ║           Request Monitor & Activity Logger                 ║
echo  ╠══════════════════════════════════════════════════════════════╣
echo  ║  Team: Chauhan Dhruv, Nayan Chotaliya, Yagna Desai,        ║
echo  ║        Daksh Gondoliya                                      ║
echo  ║  Mentor: S.V. Ramani, A.V.P.T.I. Rajkot                   ║
echo  ╚══════════════════════════════════════════════════════════════╝
echo.

:: Check if Java is available
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found in PATH!
    echo Please install Java 21 or add it to PATH.
    echo.
    pause
    exit /b 1
)

:: Display Java version
echo [INFO] Java version:
java -version 2>&1
echo.

:: Get local IP address
echo [INFO] Detecting local IP address...
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4" ^| findstr /v "127.0.0.1"') do (
    set LOCAL_IP=%%a
)
set LOCAL_IP=%LOCAL_IP: =%

echo.
echo  ╔══════════════════════════════════════════════════════════════╗
echo  ║  SERVER CONFIGURATION                                       ║
echo  ╠══════════════════════════════════════════════════════════════╣
echo  ║  IP Address: %LOCAL_IP%
echo  ║  Port:       8080
echo  ║  Console:    http://%LOCAL_IP%:8080/h2-console
echo  ║  Stats:      http://%LOCAL_IP%:8080/api/server/stats
echo  ║  Health:     http://%LOCAL_IP%:8080/api/server/health
echo  ╚══════════════════════════════════════════════════════════════╝
echo.
echo  [MONITORING] All requests will be logged below:
echo  ──────────────────────────────────────────────────────────────
echo.

:: Create data directory
if not exist "data" mkdir data
if not exist "assets" mkdir assets

:: Build and run with Maven
echo [INFO] Building and starting server...
echo [INFO] First run may take a few minutes to download dependencies.
echo.

cd server

:: Check if Maven wrapper exists, if not use system mvn
if exist "mvnw.cmd" (
    call mvnw.cmd spring-boot:run
) else (
    :: Try system Maven
    where mvn >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        call mvn spring-boot:run
    ) else (
        echo [ERROR] Maven not found!
        echo Please install Maven or use the Maven wrapper.
        echo.
        echo Alternative: Use the pre-built JAR if available:
        echo   java -jar target\zentro-server-1.0.0.jar
        echo.
        pause
        exit /b 1
    )
)

cd ..
pause
