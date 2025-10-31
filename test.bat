@echo off
setlocal enabledelayedexpansion

:: Use PowerShell to get a locale-independent timestamp
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format \"yyyy\/MM\/dd_HH-mm-ss\""') do set timestamp=%%i

echo Timestamp: !timestamp!