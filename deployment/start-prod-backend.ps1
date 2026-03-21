param(
    [string]$EnvFile = "backend/.env.prod"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        if ($val.StartsWith('"') -and $val.EndsWith('"')) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        Set-Item -Path ("Env:" + $key) -Value $val
    }
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "prod"
}

Write-Host "Starting backend with profile: $env:SPRING_PROFILES_ACTIVE" -ForegroundColor Cyan
Push-Location (Join-Path $repoRoot "backend")

mvn -DskipTests clean package
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    throw "Backend package failed"
}

$jar = Get-ChildItem -Path "target" -Filter "trustid-backend-*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    Pop-Location
    throw "Built jar not found in backend/target"
}

Write-Host "Running $($jar.FullName)" -ForegroundColor Green
java -jar $jar.FullName
Pop-Location
