@echo off
setlocal EnableDelayedExpansion
cd /d %~dp0

REM ===============================
REM CONFIG
REM ===============================
set KEEP_REPORTS=5

set ALLURE_HOME=%~dp0tools\allure
set ALLURE_CMD=%ALLURE_HOME%\bin\allure.bat
if not exist "%ALLURE_CMD%" set ALLURE_CMD=allure

set RESULTS_DIR=allure-results
set REPORTS_DIR=allure-report\reports

REM ===============================
REM Ensure directories
REM ===============================
mkdir "%RESULTS_DIR%\history" 2>nul
mkdir "%REPORTS_DIR%" 2>nul

REM ===============================
REM Detect last report number
REM ===============================
set LAST_NUM=0
for /d %%D in ("%REPORTS_DIR%\allure_report*") do (
    set NAME=%%~nxD
    set NUM=!NAME:allure_report=!
    if !NUM! GTR !LAST_NUM! set LAST_NUM=!NUM!
)

set /a NEXT_NUM=LAST_NUM+1
set REPORT_DIR=%REPORTS_DIR%\allure_report%NEXT_NUM%

echo [INFO] Creating allure_report%NEXT_NUM%

REM ===============================
REM Seed history
REM ===============================
if %LAST_NUM% GTR 0 (
    if exist "%REPORTS_DIR%\allure_report%LAST_NUM%\history" (
        xcopy "%REPORTS_DIR%\allure_report%LAST_NUM%\history" "%RESULTS_DIR%\history" /E /I /Y >nul
    )
) else (
    echo [INFO] First execution - no history
)

REM ===============================
REM Generate report
REM ===============================
"%ALLURE_CMD%" generate "%RESULTS_DIR%" -o "%REPORT_DIR%"
if errorlevel 1 (
    echo [WARN] Allure history generation failed - continuing build
)

REM ===============================
REM Cleanup old reports
REM ===============================
set /a DELETE_BEFORE=NEXT_NUM-KEEP_REPORTS
if %DELETE_BEFORE% GTR 0 (
    for /d %%D in ("%REPORTS_DIR%\allure_report*") do (
        set NAME=%%~nxD
        set NUM=!NAME:allure_report=!
        if !NUM! LEQ %DELETE_BEFORE% (
            rmdir /S /Q "%%D"
        )
    )
)

exit /b 0
