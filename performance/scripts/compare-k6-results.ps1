param(
    [Parameter(Mandatory = $true)][string]$Before,
    [Parameter(Mandatory = $true)][string]$After,
    [string]$Output = "performance/results/comparison.md"
)

$beforeResult = Get-Content -LiteralPath $Before -Raw | ConvertFrom-Json
$afterResult = Get-Content -LiteralPath $After -Raw | ConvertFrom-Json

function Format-Number([double]$value, [int]$digits = 2) {
    return [Math]::Round($value, $digits).ToString("N$digits")
}

function Change-Percent([double]$oldValue, [double]$newValue) {
    if ($oldValue -eq 0) { return "n/a" }
    $change = (($newValue - $oldValue) / $oldValue) * 100
    $prefix = if ($change -gt 0) { "+" } else { "" }
    return "$prefix$(Format-Number $change)%"
}

$errorBefore = [double]$beforeResult.errorRate * 100
$errorAfter = [double]$afterResult.errorRate * 100
$lines = @(
    "# k6 improvement report",
    "",
    "Before: ``$($beforeResult.runId)``  ",
    "After: ``$($afterResult.runId)``",
    "",
    "| Metric | Before | After | Change |",
    "|---|---:|---:|---:|",
    "| p95 | $(Format-Number $beforeResult.p95Ms) ms | $(Format-Number $afterResult.p95Ms) ms | $(Change-Percent $beforeResult.p95Ms $afterResult.p95Ms) |",
    "| p99 | $(Format-Number $beforeResult.p99Ms) ms | $(Format-Number $afterResult.p99Ms) ms | $(Change-Percent $beforeResult.p99Ms $afterResult.p99Ms) |",
    "| Throughput | $(Format-Number $beforeResult.throughputRps) req/s | $(Format-Number $afterResult.throughputRps) req/s | $(Change-Percent $beforeResult.throughputRps $afterResult.throughputRps) |",
    "| Error rate | $(Format-Number $errorBefore)% | $(Format-Number $errorAfter)% | $(Change-Percent $errorBefore $errorAfter) |",
    "| Requests | $($beforeResult.requests) | $($afterResult.requests) | $(Change-Percent $beforeResult.requests $afterResult.requests) |"
)

$outputDirectory = Split-Path -Parent $Output
if ($outputDirectory) { New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null }
Set-Content -LiteralPath $Output -Value ($lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "Report written to $Output"
