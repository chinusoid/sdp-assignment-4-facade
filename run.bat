@echo off
setlocal
cd /d "%~dp0"
if not exist build mkdir build
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build src\com\company\*.java
if errorlevel 1 exit /b 1
if "%~1"=="test" (
    javac --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build -d build tests\com\company\*.java
    if errorlevel 1 exit /b 1
    java -cp build com.company.FacadeTests
) else (
    java -cp build com.company.Main
)
exit /b %errorlevel%
