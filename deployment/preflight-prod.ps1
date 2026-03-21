param(
    [string]$EnvFile = "backend/.env.prod"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

function Write-Check($ok, $message) {
    if ($ok) {
        Write-Host "[OK]  $message" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $message" -ForegroundColor Red
    }
}

function Load-DotEnv($path) {
    if (-not (Test-Path $path)) {
        return @{}
    }

    $map = @{}
    Get-Content $path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $val = $line.Substring($idx + 1).Trim()
        if ($val.StartsWith('"') -and $val.EndsWith('"')) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        $map[$key] = $val
    }
    return $map
}

$loaded = Load-DotEnv $EnvFile
foreach ($k in $loaded.Keys) {
    if (-not [string]::IsNullOrWhiteSpace($loaded[$k])) {
        Set-Item -Path ("Env:" + $k) -Value $loaded[$k]
    }
}

Write-Host "Running TrustID production preflight..." -ForegroundColor Cyan
Write-Host "Repo: $repoRoot"
Write-Host "Env file: $EnvFile"

$required = @("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "APP_ALLOWED_ORIGINS")
$missing = @()

foreach ($name in $required) {
    $val = [Environment]::GetEnvironmentVariable($name)
    $invalid = [string]::IsNullOrWhiteSpace($val) -or $val.Contains("<") -or $val.Contains(">")
    if ($invalid) { $missing += $name }
}

Write-Check ($missing.Count -eq 0) "Required environment variables are set"
if ($missing.Count -gt 0) {
    Write-Host "Missing/placeholder vars: $($missing -join ', ')" -ForegroundColor Yellow
}

$jwt = [Environment]::GetEnvironmentVariable("JWT_SECRET")
$jwtOk = -not [string]::IsNullOrWhiteSpace($jwt) -and $jwt.Length -ge 32 -and -not $jwt.Contains("<")
Write-Check $jwtOk "JWT secret length is >= 32"

$origins = [Environment]::GetEnvironmentVariable("APP_ALLOWED_ORIGINS")
$originOk = -not [string]::IsNullOrWhiteSpace($origins) -and -not $origins.Contains("localhost")
Write-Check $originOk "CORS origins do not use localhost"

$javaOk = $null -ne (Get-Command java -ErrorAction SilentlyContinue)
$mvnOk = $null -ne (Get-Command mvn -ErrorAction SilentlyContinue)
Write-Check $javaOk "Java is installed"
Write-Check $mvnOk "Maven is installed"

$uploadDir = [Environment]::GetEnvironmentVariable("APP_UPLOAD_DIR")
if ([string]::IsNullOrWhiteSpace($uploadDir) -or $uploadDir.Contains("<")) {
    $uploadDir = (Join-Path $repoRoot "uploads")
}

try {
    New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null
    $probe = Join-Path $uploadDir "_write_test.tmp"
    "ok" | Out-File -FilePath $probe -Encoding ascii
    Remove-Item $probe -Force
    Write-Check $true "Upload directory exists and is writable ($uploadDir)"
} catch {
    Write-Check $false "Upload directory is not writable ($uploadDir)"
}

$buildOk = $false
if ($mvnOk) {
    try {
        Push-Location (Join-Path $repoRoot "backend")
        & mvn -q -DskipTests clean compile | Out-Null
        $buildOk = $true
    } catch {
        $buildOk = $false
    } finally {
        Pop-Location
    }
}
Write-Check $buildOk "Backend compiles"

$failed = @()
if ($missing.Count -gt 0) { $failed += "env" }
if (-not $jwtOk) { $failed += "jwt" }
if (-not $originOk) { $failed += "cors" }
if (-not $javaOk) { $failed += "java" }
if (-not $mvnOk) { $failed += "maven" }
if (-not $buildOk) { $failed += "build" }

if ($failed.Count -gt 0) {
    Write-Host "Preflight failed: $($failed -join ', ')" -ForegroundColor Red
    exit 1
}

Write-Host "Preflight passed. Ready for production deployment." -ForegroundColor Green
exit 0
