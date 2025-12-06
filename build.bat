@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    Pay Everyone - Multi-Version Build
echo ========================================
echo.
echo Available Minecraft versions:
echo   1.  1.21.4 (default)
echo   2.  1.21.5
echo   3.  1.21.6
echo   4.  1.21.7
echo   5.  1.21.8
echo   6.  1.21.9
echo   7.  1.21.10
echo   8.  Build ALL versions
echo   0.  Exit
echo.

set /p choice="Select version to build (0-8): "

if "%choice%"=="1" (
    set "MC_VER=1.21.4"
    goto build_single
)
if "%choice%"=="2" (
    set "MC_VER=1.21.5"
    goto build_single
)
if "%choice%"=="3" (
    set "MC_VER=1.21.6"
    goto build_single
)
if "%choice%"=="4" (
    set "MC_VER=1.21.7"
    goto build_single
)
if "%choice%"=="5" (
    set "MC_VER=1.21.8"
    goto build_single
)
if "%choice%"=="6" (
    set "MC_VER=1.21.9"
    goto build_single
)
if "%choice%"=="7" (
    set "MC_VER=1.21.10"
    goto build_single
)
if "%choice%"=="8" goto build_all
if "%choice%"=="0" (
    echo Exiting...
    exit /b 0
)

echo Invalid choice. Please run again.
pause
exit /b 1

:build_single
echo.
echo Building for Minecraft %MC_VER%...
echo ----------------------------------------
call gradlew.bat clean build "-PMC_VERSION=%MC_VER%"
if errorlevel 1 (
    echo.
    echo BUILD FAILED for Minecraft %MC_VER%
    pause
    exit /b 1
)
echo.
echo BUILD SUCCESSFUL!
echo Output: build\libs\pay-everyone-%MC_VER%-1.0.2.jar
goto end

:build_all
echo.
echo Building ALL versions...
echo ========================================

set "VERSIONS=1.21.4 1.21.5 1.21.6 1.21.7 1.21.8 1.21.9 1.21.10"
set "FAILED="
set "SUCCESS="

for %%v in (%VERSIONS%) do (
    echo.
    echo Building for Minecraft %%v...
    echo ----------------------------------------
    call gradlew.bat clean build "-PMC_VERSION=%%v"
    if errorlevel 1 (
        echo FAILED: Minecraft %%v
        set "FAILED=!FAILED! %%v"
    ) else (
        echo SUCCESS: Minecraft %%v
        set "SUCCESS=!SUCCESS! %%v"
        
        REM Copy to output folder
        if not exist "output" mkdir output
        copy "build\libs\pay-everyone-%%v-1.0.2.jar" "output\" >nul 2>&1
    )
)

echo.
echo ========================================
echo Build Summary:
echo ========================================
if "!FAILED!"=="" (
    echo All versions built successfully!
) else (
    echo.
    echo Successful:!SUCCESS!
    echo Failed:!FAILED!
)
echo.
echo Output files copied to: output\

:end
echo.
pause
