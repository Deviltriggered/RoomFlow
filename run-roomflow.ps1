param(
    [int]$Port = 8080,
    [string]$DbUrl = "jdbc:postgresql://localhost:5432/BookingAgency",
    [string]$DbUsername = "postgres",
    [string]$DbPassword = "1342",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mavenWrapper = Join-Path $projectRoot "mvnw.cmd"
$jarPath = Join-Path $projectRoot "Backend\target\roomflow-backend-0.0.1-SNAPSHOT.jar"

if ($Rebuild) {
    Write-Host "Compiling frontend and backend..."
    & cmd /c "`"$mavenWrapper`" -pl Backend -am package -DskipTests"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven boot finished with error."
    }
}

if (-not (Test-Path $jarPath)) {
    throw "JAR file not found: $jarPath. Run the script with -Rebuild."
}

Write-Host "Booting RoomFlow using port $Port..."
Write-Host "JAR: $jarPath"

& java `
    "-DDB_URL=$DbUrl" `
    "-DDB_USERNAME=$DbUsername" `
    "-DDB_PASSWORD=$DbPassword" `
    -jar $jarPath `
    "--server.port=$Port"
