@echo off

setlocal enabledelayedexpansion

:: Use PowerShell to get a locale-independent timestamp
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format \"yyyy_MM_dd_HH_mm_ss\""') do set timestamp=%%i

echo Timestamp: !timestamp!
mysqldump --host=localhost --port=3306 --default-character-set=utf8 --user=root -pqwe123 --protocol=tcp --single-transaction=TRUE --routines --events "pos_manager" > "dbDump/backup_!timestamp!.sql"