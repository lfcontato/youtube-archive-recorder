@echo off
setlocal EnableExtensions

set "ROOT=%~dp0"
pushd "%ROOT%" || exit /b 1

for /f %%I in ('powershell -NoProfile -Command "Get-Date -Format ''yyyy-MM-dd-HH-mm-ss''"') do set "CURRENT_DATE=%%I"
set "COMPUTER_NAME=%COMPUTERNAME%"

git add .
if errorlevel 1 goto :error

git commit -m "%COMPUTER_NAME% %CURRENT_DATE%"
if errorlevel 1 goto :error

git push
if errorlevel 1 goto :error

popd
exit /b 0

:error
set "EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %EXIT_CODE%
