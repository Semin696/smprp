@echo off
title Building Oraxen...
echo Building Oraxen plugin...
call gradlew.bat shadowJar
if %errorlevel% neq 0 (
    echo.
    echo Build FAILED!
    pause
    exit /b %errorlevel%
)
echo.
echo Build successful! JAR file: build\libs\
pause
