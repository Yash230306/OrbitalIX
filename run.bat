@echo off
call compile.bat
if errorlevel 1 exit /b %ERRORLEVEL%
java -cp out com.orbitalix.app.Main %*
