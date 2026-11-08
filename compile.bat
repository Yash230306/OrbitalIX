@echo off
cd /d "%~dp0"
if not exist out mkdir out
powershell -NoProfile -Command "Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { [char]34 + ($_.FullName -replace '\\','/') + [char]34 } | Set-Content -Encoding ascii sources.txt"
javac --release 17 -encoding UTF-8 -d out @sources.txt
if errorlevel 1 exit /b 1
echo Compiled OK
