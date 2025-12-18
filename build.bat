@echo off
setlocal

echo ========================================
echo Pay Everyone - Build Script
echo ========================================
echo.

if not exist "dist" mkdir dist

echo Building legacy and modern versions...
call gradlew.bat clean :legacy:build :modern:build

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

echo.
echo Copying JARs to dist...

for %%f in (legacy\build\libs\*.jar) do (
    if not "%%~xf"=="-dev.jar" (
        copy /Y "%%f" "dist\" >nul
        echo   %%~nxf
    )
)

for %%f in (modern\build\libs\*.jar) do (
    if not "%%~xf"=="-dev.jar" (
        copy /Y "%%f" "dist\" >nul
        echo   %%~nxf
    )
)

echo.
echo ========================================
echo Build complete! Output in dist\
echo ========================================
dir /b dist\*.jar 2>nul
echo.

endlocal

