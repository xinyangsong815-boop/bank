param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$required = @(
    'README.md', 'LICENSE', 'docker-compose.yml',
    'backend/pom.xml', 'backend/mvnw', 'backend/mvnw.cmd', 'backend/.mvn/wrapper/maven-wrapper.properties',
    'backend/src/main/java/com/example/bankapproval/BankApprovalApplication.java',
    'backend/src/main/resources/application.yml', 'backend/src/main/resources/mapper/TransactionApplicationMapper.xml',
    'backend/src/test/java/com/example/bankapproval/api/TransactionApiIntegrationTest.java',
    'frontend/package.json', 'frontend/package-lock.json', 'frontend/.env.development', 'frontend/src/App.vue',
    'database/01-schema.sql', 'database/02-data.sql',
    'docs/ARCHITECTURE.md', 'docs/API_EXAMPLES.md', 'docs/SECURITY.md', 'docs/TESTING.md', 'docs/VERIFICATION.md',
    'scripts/api-smoke.ps1'
)
foreach ($relative in $required) {
    $item = Get-Item -LiteralPath (Join-Path $projectRoot $relative) -ErrorAction Stop
    if ($item.Length -eq 0) { throw ('Empty required file: ' + $relative) }
}
Write-Host ('PASS: ' + $required.Count + ' required files exist and are non-empty')
if ($Build) {
    Push-Location (Join-Path $projectRoot 'backend')
    try {
        & .\mvnw.cmd clean verify
        if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }
    } finally { Pop-Location }
    Push-Location (Join-Path $projectRoot 'frontend')
    try {
        & npm.cmd ci --ignore-scripts
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed' }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    } finally { Pop-Location }
}
