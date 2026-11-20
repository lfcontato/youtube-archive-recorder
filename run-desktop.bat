@echo off
setlocal

cd /d "%~dp0"

set "APP_EXE=%~dp0desktop\build\compose\binaries\main\exe\YouTubeArchiveRecorder-0.1.0.exe"

if exist "%APP_EXE%" (
    start "" "%APP_EXE%"
    exit /b 0
)

call gradlew.bat :desktop:packageExe

if errorlevel 1 (
    echo.
    echo Falha ao iniciar o aplicativo.
    pause
    exit /b 1
)

if exist "%APP_EXE%" (
    start "" "%APP_EXE%"
) else (
    echo.
    echo O executavel nao foi encontrado depois do empacotamento.
    pause
    exit /b 1
)

endlocal
