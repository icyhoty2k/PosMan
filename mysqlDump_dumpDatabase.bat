@echo off

setlocal enabledelayedexpansion

:: Use PowerShell to get a locale-independent timestamp
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format \"yyyy_MM_dd_HH_mm_ss\""') do set timestamp=%%i
mysqldump --defaults-extra-file=I:/10_JavaSources/PosMan/config.cnf --default-character-set=utf8 --protocol=tcp --single-transaction=TRUE --routines --events "pos_manager" > "dbDump/backup_!timestamp!.sql"