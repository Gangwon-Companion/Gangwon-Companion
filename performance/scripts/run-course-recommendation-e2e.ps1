param(
    [string]$BackendUrl = "http://localhost:8080",
    [string]$Message = "",
    [string]$Region = "",
    [int]$TravelDays = 1,
    [int]$Nights = 0,
    [bool]$PetAllowed = $false,
    [bool]$WheelchairAccessible = $false,
    [string[]]$Preferences = @(),
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

# Windows PowerShell 5 reads UTF-8 files without a BOM as the system code page.
# Keep the script source ASCII-only while preserving Korean defaults.
if ([string]::IsNullOrWhiteSpace($Region)) {
    $Region = -join [char[]](0xAC15, 0xB989)
}
if ([string]::IsNullOrWhiteSpace($Message)) {
    $Message = $Region + (-join [char[]](0xC5D0, 0xC11C, 0x20, 0xD558, 0xB8E8, 0x20, 0xC5EC, 0xD589, 0xD558, 0xACE0, 0x20, 0xC2F6, 0xC5B4, 0xC694))
}

$id = [Guid]::NewGuid().ToString("N")
$username = "e2e" + $id.Substring(0, 12)
$nickname = "e" + $id.Substring(0, 5)
$password = "E2eTest1234!"

$signupBody = @{
    username = $username
    password = $password
    email = "$username@example.com"
    nickname = $nickname
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "$BackendUrl/api/v1/auth/signup" `
    -ContentType "application/json" `
    -Body $signupBody | Out-Null

$loginBody = @{
    username = $username
    password = $password
} | ConvertTo-Json

$token = (Invoke-RestMethod `
    -Method Post `
    -Uri "$BackendUrl/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody).token

if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login response did not contain a JWT."
}

$recommendationBody = @{
    message = $Message
    region = $Region
    travel_days = $TravelDays
    nights = $Nights
    pet_allowed = $PetAllowed
    wheelchair_accessible = $WheelchairAccessible
    preferences = $Preferences
} | ConvertTo-Json -Depth 5

$httpResponse = Invoke-WebRequest `
    -UseBasicParsing `
    -Method Post `
    -Uri "$BackendUrl/api/v1/courses/recommendations" `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($recommendationBody))
$responseBytes = $httpResponse.RawContentStream.ToArray()
$response = [System.Text.Encoding]::UTF8.GetString($responseBytes) | ConvertFrom-Json

# Preserve failed responses as diagnostics as well as successful evidence.
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $outputDirectory = Split-Path -Parent $OutputPath
    if ($outputDirectory) {
        New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
    }
    $response | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $OutputPath -Encoding UTF8
}

if ($response.status -ne "completed") {
    throw "Expected status=completed but received '$($response.status)'."
}
if ($response.final_response.response_status -ne "READY") {
    throw "Expected final response READY but received '$($response.final_response.response_status)'."
}
if ($response.itinerary_status -ne "READY") {
    throw "Expected itinerary READY but received '$($response.itinerary_status)'."
}
if ($response.hard_validation.status -ne "VALID") {
    throw "Expected hard validation VALID but received '$($response.hard_validation.status)'."
}
if ($response.quality_validation.status -ne "PASS") {
    throw "Expected quality validation PASS but received '$($response.quality_validation.status)'."
}
if (@($response.itinerary).Count -eq 0) {
    throw "Expected at least one itinerary item."
}

[pscustomobject]@{
    status = $response.status
    responseStatus = $response.final_response.response_status
    retryCount = $response.retry_count
    itineraryStatus = $response.itinerary_status
    hardValidation = $response.hard_validation.status
    qualityValidation = $response.quality_validation.status
    itineraryCount = @($response.itinerary).Count
} | ConvertTo-Json
