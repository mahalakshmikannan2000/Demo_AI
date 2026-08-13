@echo off
setlocal
cd /d %~dp0

REM ==============================
REM Allure setup
REM Place the Allure CLI distribution under tools\allure (bin\allure.bat), or
REM install Allure CLI and make sure "allure" is available on PATH.
REM ==============================
set ALLURE_HOME=%~dp0tools\allure
set ALLURE_CMD=%ALLURE_HOME%\bin\allure.bat
if not exist "%ALLURE_CMD%" set ALLURE_CMD=allure

REM ==============================
REM Directories (must match allure.results.directory in config.properties)
REM ==============================
set RESULTS_DIR=allure-results
set OUTPUT_DIR=allure-report\single

if not exist "%RESULTS_DIR%" exit /b 0
mkdir "%OUTPUT_DIR%" 2>nul

echo [INFO] Generating Allure SINGLE file

"%ALLURE_CMD%" generate "%RESULTS_DIR%" ^
 --single-file ^
 --clean ^
 -o "%OUTPUT_DIR%"

REM Never fail the pipeline because of report generation
exit /b 0
