@echo off
cd /d "%~dp0"
setlocal EnableDelayedExpansion
if not exist out mkdir out
set "SOURCES="
for /r src\main\java %%f in (*.java) do set SOURCES=!SOURCES! "%%f"
javac --release 17 -encoding UTF-8 -d out !SOURCES!
if errorlevel 1 exit /b 1
echo Compiled OK
