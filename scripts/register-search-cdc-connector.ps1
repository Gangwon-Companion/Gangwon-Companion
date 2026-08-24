$ErrorActionPreference = "Stop"

$connectUrl = $env:KAFKA_CONNECT_URL
if ([string]::IsNullOrWhiteSpace($connectUrl)) {
    $connectUrl = "http://localhost:8083"
}

$configPath = Join-Path $PSScriptRoot "debezium-postgres-connector.json"

Write-Host "Registering Debezium connector from $configPath to $connectUrl"
Invoke-RestMethod `
    -Method Post `
    -Uri "$connectUrl/connectors" `
    -ContentType "application/json" `
    -Body (Get-Content -LiteralPath $configPath -Raw) |
    ConvertTo-Json -Depth 20
