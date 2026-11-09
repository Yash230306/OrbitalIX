@echo off
setlocal EnableDelayedExpansion
if not exist out mkdir out
set "SOURCES="
for /r src\main\java %%f in (*.java) do set SOURCES=!SOURCES! "%%f"
javac --release 17 -encoding UTF-8 -d out !SOURCES!
exit /b %ERRORLEVEL%
