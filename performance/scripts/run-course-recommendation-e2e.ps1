param(
    [string]$BackendUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

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

$recommendationBody = '{"message":"\uac15\ub989\uc5d0\uc11c \ud558\ub8e8 \uc5ec\ud589\ud558\uace0 \uc2f6\uc5b4\uc694","region":"\uac15\ub989","travel_days":1,"nights":0,"pet_allowed":false,"preferences":[]}'

$response = Invoke-RestMethod `
    -Method Post `
    -Uri "$BackendUrl/api/v1/courses/recommendations" `
    -Headers @{ Authorization = "Bearer $token" } `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($recommendationBody))

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
