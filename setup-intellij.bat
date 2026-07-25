@echo off
setlocal
call gradlew.bat -version || goto :error
call gradlew.bat setupDecompWorkspace idea || goto :error
echo.
echo IntelliJ IDEA project files generated successfully.
exit /b 0
:error
echo.
echo Setup failed. Copy the complete console output into the next bug report.
exit /b 1
