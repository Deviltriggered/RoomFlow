param(
    [int]$Port = 8080,
    [string]$DbUrl = $env:DB_URL,
    [string]$DbUsername = $env:DB_USERNAME,
    [string]$DbPassword = $env:DB_PASSWORD,
    [string]$JwtSecret = $env:JWT_SECRET,
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mavenWrapper = Join-Path $projectRoot "mvnw.cmd"
$jarPath = Join-Path $projectRoot "Backend\target\roomflow-backend-1.0.0.jar"

if ([string]::IsNullOrWhiteSpace($DbUrl) -or [string]::IsNullOrWhiteSpace($DbUsername) -or [string]::IsNullOrWhiteSpace($DbPassword)) {
    throw "Set DB_URL, DB_USERNAME, and DB_PASSWORD before running RoomFlow."
}

if ([string]::IsNullOrWhiteSpace($JwtSecret) -or $JwtSecret.Length -lt 32) {
    throw "Set JWT_SECRET with at least 32 characters before running RoomFlow."
}

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
    "-DJWT_SECRET=$JwtSecret" `
    -jar $jarPath `
    "--server.port=$Port"
