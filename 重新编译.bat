@echo off
cd /d "%~dp0"

rem ============================================================
rem  One-click recompile: double-click after editing .java
rem  NO JDK required!
rem  Uses the bundled JRE (runtime\bin\java.exe) together with
rem  the standalone Eclipse compiler tools\ecj.jar.
rem ============================================================

set "JAVA=%~dp0runtime\bin\java.exe"
set "ECJ=%~dp0tools\ecj.jar"

if not exist "%JAVA%" (
    echo [ERROR] runtime\bin\java.exe not found!
    echo Please make sure the "runtime" folder is in the same directory.
    goto end
)

if not exist "%ECJ%" (
    echo [ERROR] tools\ecj.jar not found!
    echo Please make sure the "tools" folder is in the same directory.
    goto end
)

if not exist "LovePopups.java" (
    echo [ERROR] LovePopups.java not found here.
    goto end
)

echo Compiling LovePopups.java with built-in compiler ...
echo ============================================
"%JAVA%" "-Dfile.encoding=UTF-8" -cp "%ECJ%" org.eclipse.jdt.internal.compiler.batch.Main -encoding UTF-8 -cp . -d . LovePopups.java
set "EXIT=%ERRORLEVEL%"
echo ============================================

if "%EXIT%"=="0" (
    echo [OK] Compiled successfully.
    echo Now double-click the start script to run the updated program.
) else (
    echo [FAILED] Compilation error! See above.
)

:end
echo.
pause
