@echo off
setlocal

set "REBUILD=0"

if /I "%~1"=="--rebuild" (
    set "REBUILD=1"
    set "PORT=%~2"
    if not "%~3"=="" set "DB_URL=%~3"
    if not "%~4"=="" set "DB_USERNAME=%~4"
    if not "%~5"=="" set "DB_PASSWORD=%~5"
    if not "%~6"=="" set "JWT_SECRET=%~6"
) else (
    set "PORT=%~1"
    if not "%~2"=="" set "DB_URL=%~2"
    if not "%~3"=="" set "DB_USERNAME=%~3"
    if not "%~4"=="" set "DB_PASSWORD=%~4"
    if not "%~5"=="" set "JWT_SECRET=%~5"
)

if "%PORT%"=="" set "PORT=8080"

if "%DB_URL%"=="" (
    echo Set DB_URL before running RoomFlow.
    exit /b 1
)

if "%DB_USERNAME%"=="" (
    echo Set DB_USERNAME before running RoomFlow.
    exit /b 1
)

if "%DB_PASSWORD%"=="" (
    echo Set DB_PASSWORD before running RoomFlow.
    exit /b 1
)

if "%JWT_SECRET%"=="" (
    echo Set JWT_SECRET with at least 32 characters before running RoomFlow.
    exit /b 1
)

set "PROJECT_ROOT=%~dp0"
set "JAR_PATH=%PROJECT_ROOT%Backend\target\roomflow-backend-1.0.0.jar"

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
java -DDB_URL="%DB_URL%" -DDB_USERNAME="%DB_USERNAME%" -DDB_PASSWORD="%DB_PASSWORD%" -DJWT_SECRET="%JWT_SECRET%" -jar "%JAR_PATH%" --server.port=%PORT%
