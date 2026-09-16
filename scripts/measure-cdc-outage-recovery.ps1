param(
    [int]$RestaurantId = 64,
    [int]$OutageSeconds = 20,
    [int]$TimeoutSeconds = 180,
    [double]$SecondRating = -1,
    [string]$OutputPath = "performance/results/kafka-cdc-es-outage-recovery-$(Get-Date -Format yyyyMMdd-HHmmss).json"
)

$ErrorActionPreference = "Stop"
$esContainer = "elasticsearch_gangwon"
$dbContainer = "postgres_gangwon"
$composeArgs = @("compose", "-f", "compose.yaml", "-f", "compose.observability.yaml")

function Invoke-Docker {
    param([string[]]$Arguments)
    $ErrorActionPreference = "Continue"
    $lastOutput = $null
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        $lastOutput = & docker @Arguments 2>$null
        if ($LASTEXITCODE -eq 0) { return $lastOutput }
        Start-Sleep -Seconds 1
    }
    throw "Docker command failed after retries: docker $($Arguments -join ' ')`n$lastOutput"
}

function Get-EsRating {
    param([int]$Id)
    try {
        $doc = Invoke-RestMethod -Method Get -Uri "http://localhost:9200/gangwon-places/_doc/RESTAURANT:$Id" -ErrorAction Stop
        return [double]$doc._source.rating
    } catch { return $null }
}

function Get-DbRating {
    param([int]$Id)
    return [double](Invoke-Docker -Arguments @("exec", $dbContainer, "psql", "-U", "gangwon_user", "-d", "gangwon", "-tA", "-c", "select rating from restaurants where id=$Id;" ) | Select-Object -First 1)
}

function Set-DbRating {
    param([int]$Id, [double]$Rating)
    $value = $Rating.ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture)
    Invoke-Docker -Arguments @("exec", $dbContainer, "psql", "-U", "gangwon_user", "-d", "gangwon", "-c", "update restaurants set rating=$value where id=$Id;") | Out-Null
}

function Wait-EsHealthy {
    param([int]$TimeoutSeconds)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:9200/_cluster/health" -ErrorAction Stop
            if ($health.status -in @("green", "yellow")) { return $true }
        } catch { }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    return $false
}

$baseline = Get-EsRating -Id $RestaurantId
if ($null -eq $baseline) { throw "ES 문서 RESTAURANT:$RestaurantId 를 찾을 수 없습니다." }
$target = if ($baseline -lt 4.99) { [math]::Round($baseline + 0.01, 2) } else { [math]::Round($baseline - 0.01, 2) }
$eventAt = $null
$esRecoveredAt = $null
$reflectedAt = $null
$reflected = $false
$esWasStopped = $false

try {
    Write-Host "Baseline ES rating: $baseline"
    Invoke-Docker -Arguments ($composeArgs + @("stop", "elasticsearch")) | Out-Null
    $esWasStopped = $true
    $outageStartedAt = Get-Date
    Write-Host "ES stopped: $outageStartedAt"

    Set-DbRating -Id $RestaurantId -Rating $target
    $eventAt = Get-Date
    Write-Host "PostgreSQL CDC update: rating=$target at $eventAt"
    if ($SecondRating -ge 0) {
        Start-Sleep -Seconds 2
        $target = [math]::Round($SecondRating, 2)
        Set-DbRating -Id $RestaurantId -Rating $target
        Write-Host "PostgreSQL second CDC update: rating=$target at $(Get-Date)"
    }
    Start-Sleep -Seconds $OutageSeconds

    Invoke-Docker -Arguments ($composeArgs + @("start", "elasticsearch")) | Out-Null
    $esRecoveredAt = Get-Date
    if (-not (Wait-EsHealthy -TimeoutSeconds 90)) { throw "ES가 복구되지 않았습니다." }
    Write-Host "ES healthy: $esRecoveredAt"

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $current = Get-EsRating -Id $RestaurantId
        if ($null -ne $current -and [math]::Abs($current - $target) -lt 0.001) {
            $reflectedAt = Get-Date
            $reflected = $true
            break
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
}
finally {
    if ($esWasStopped) { Invoke-Docker -Arguments ($composeArgs + @("start", "elasticsearch")) | Out-Null }
    Set-DbRating -Id $RestaurantId -Rating $baseline
}

$report = [pscustomobject]@{
    measuredAt = (Get-Date).ToString("o")
    restaurantId = $RestaurantId
    baselineRating = $baseline
    testRating = $target
    outageSeconds = $OutageSeconds
    esStoppedAt = $outageStartedAt.ToString("o")
    cdcUpdateAt = $eventAt.ToString("o")
    esHealthyAt = if ($null -ne $esRecoveredAt) { $esRecoveredAt.ToString("o") } else { $null }
    reflectedAt = if ($null -ne $reflectedAt) { $reflectedAt.ToString("o") } else { $null }
    reflectedAfterRecovery = $reflected
    recoveryToReflectionMs = if ($reflected) { [math]::Round(($reflectedAt - $esRecoveredAt).TotalMilliseconds, 1) } else { $null }
    totalCdcRecoveryMs = if ($reflected) { [math]::Round(($reflectedAt - $eventAt).TotalMilliseconds, 1) } else { $null }
}

$report | ConvertTo-Json | Out-File -FilePath $OutputPath -Encoding utf8
$report | Format-List
Write-Host "Saved to: $OutputPath"
