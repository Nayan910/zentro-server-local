@echo off
title Zentro Server - Quick Start
color 0A

echo ============================================
echo    ZENTRO SERVER - Quick Start
echo ============================================
echo.

:: Check if pre-built JAR exists
if exist "server\target\zentro-server-1.0.0.jar" (
    echo [INFO] Found pre-built JAR. Starting server...
    java -jar server\target\zentro-server-1.0.0.jar
) else (
    echo [INFO] No pre-built JAR found. Building first...
    cd server
    
    where mvn >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        call mvn clean package -DskipTests
        if %ERRORLEVEL% equ 0 (
            cd ..
            java -jar server\target\zentro-server-1.0.0.jar
        ) else (
            echo [ERROR] Build failed!
            pause
        )
    ) else (
        echo [ERROR] Maven not found. Install Maven or use START_SERVER.bat
        pause
    )
)
