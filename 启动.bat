@echo off
cd /d "%~dp0"

set "JAVA=%~dp0runtime\bin\java.exe"

if not exist "%JAVA%" (
    echo [ERROR] runtime\bin\java.exe not found!
    echo Please make sure the "runtime" folder is in the same directory.
    goto end
)

if not exist "LovePopups.class" (
    echo [ERROR] LovePopups.class not found!
    goto end
)

echo Starting LovePopups...
echo ============================================
"%JAVA%" "-Dfile.encoding=UTF-8" -cp . LovePopups
set "EXIT=%ERRORLEVEL%"
echo ============================================

if "%EXIT%"=="0" (
    echo Program exited normally.
) else (
    echo [ERROR] JVM exited with code %EXIT%
)

:end
pause
