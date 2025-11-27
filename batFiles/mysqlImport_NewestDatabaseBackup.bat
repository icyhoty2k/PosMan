@ECHO OFF

set BACKUP_DIR=./dbDump

:: ✅ 2. Move into backup directory
cd /d "%BACKUP_DIR%"

:: ✅ 3. Get the latest .sql file (newest by date)
for /f "delims=" %%f in ('dir /b /a:-d /o-d *.sql') do (
    set "newest=%%f"
    goto found
)
:found
if not defined newest (
    echo ❌ No .sql backup files found in %BACKUP_DIR%
    pause
    exit /b
)
echo ✅ newest Backup File: %newest%

:: ✅ 5. Run MySQL import automatically
mysql --defaults-extra-file=I:/10_JavaSources/PosMan/config.cnf "pos_manager" < "%newest%"