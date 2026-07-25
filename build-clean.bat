@echo off
setlocal
call gradlew.bat clean build
if errorlevel 1 (
  echo.
  echo Build failed. Copy the complete console output into the next bug report.
  exit /b 1
)
echo.
echo Build completed. Check build\libs\
