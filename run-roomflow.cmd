@echo off
setlocal

set "REBUILD=0"

if /I "%~1"=="--rebuild" (
    set "REBUILD=1"
    set "PORT=%~2"
    set "DB_URL=%~3"
    set "DB_USERNAME=%~4"
    set "DB_PASSWORD=%~5"
) else (
    set "PORT=%~1"
    set "DB_URL=%~2"
    set "DB_USERNAME=%~3"
    set "DB_PASSWORD=%~4"
)

if "%PORT%"=="" set "PORT=8080"

if "%DB_URL%"=="" set "DB_URL=jdbc:postgresql://localhost:5432/BookingAgency"

if "%DB_USERNAME%"=="" set "DB_USERNAME=postgres"

if "%DB_PASSWORD%"=="" set "DB_PASSWORD=1342"

set "PROJECT_ROOT=%~dp0"
set "JAR_PATH=%PROJECT_ROOT%Backend\target\roomflow-backend-0.0.1-SNAPSHOT.jar"

if "%REBUILD%"=="1" (
    echo Compiling frontend and backend...
    call "%PROJECT_ROOT%mvnw.cmd" -pl Backend -am package -DskipTests
    if errorlevel 1 exit /b 1
)

if not exist "%JAR_PATH%" (
    echo JAR file not found: %JAR_PATH%
    echo Run .\run-roomflow.cmd --rebuild %PORT% to build it first.
    exit /b 1
)

echo Booting RoomFlow using port %PORT%...
java -DDB_URL="%DB_URL%" -DDB_USERNAME="%DB_USERNAME%" -DDB_PASSWORD="%DB_PASSWORD%" -jar "%JAR_PATH%" --server.port=%PORT%
