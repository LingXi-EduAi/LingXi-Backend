param(
    [ValidateSet("Direct", "Backend", "All")]
    [string]$Mode = "All",

    [string]$Query = "1+1=?",

    [string]$Token = "",

    [string]$BackendUrl = "http://127.0.0.1:5678"
)

$ErrorActionPreference = "Stop"
$script:Failed = $false
$script:TemporaryToken = $false
$script:RedisKey = $null

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw ".env not found: $Path"
    }

    Get-Content -LiteralPath $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $separator = $line.IndexOf("=")
        if ($separator -lt 1) {
            return
        }

        $name = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        Set-Item -Path "Env:$name" -Value $value
    }
}

function Test-TcpEndpoint {
    param([Uri]$Uri)

    $port = $Uri.Port
    if ($Uri.IsDefaultPort) {
        $port = if ($Uri.Scheme -eq "https") { 443 } else { 80 }
    }

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync($Uri.Host, $port)
        if (-not $task.Wait(5000)) {
            throw "Connection timed out"
        }
        if (-not $client.Connected) {
            throw "Connection failed"
        }
        Write-Host "TCP OK: $($Uri.Host):$port" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "TCP FAILED: $($Uri.Host):$port - $($_.Exception.Message)" -ForegroundColor Red
        return $false
    } finally {
        $client.Dispose()
    }
}

function Get-HttpErrorBody {
    param($ErrorRecord)

    if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
        return $ErrorRecord.ErrorDetails.Message
    }
    return $ErrorRecord.Exception.Message
}

function Write-DifyResult {
    param($Response)

    Write-Host "Dify request succeeded." -ForegroundColor Green
    Write-Host "Message ID: $($Response.message_id)"
    Write-Host "Conversation ID: $($Response.conversation_id)"
    Write-Host "Answer: $($Response.answer)" -ForegroundColor Cyan
}

function Test-DifyDirect {
    Write-Host "`n=== Direct Dify Chatflow Test ===" -ForegroundColor Yellow

    if ([string]::IsNullOrWhiteSpace($env:DIFY_CHATFLOW_BASE_URL)) {
        throw "DIFY_CHATFLOW_BASE_URL is not configured in .env"
    }

    $apiKey = $env:DIFY_CHATFLOW_API_KEY
    if ([string]::IsNullOrWhiteSpace($apiKey)) {
        throw "DIFY_CHATFLOW_API_KEY is not configured in .env"
    }

    $baseUri = [Uri]$env:DIFY_CHATFLOW_BASE_URL
    if (-not (Test-TcpEndpoint -Uri $baseUri)) {
        $script:Failed = $true
        return
    }

    $uri = "$($env:DIFY_CHATFLOW_BASE_URL.TrimEnd('/'))/chat-messages"
    $body = @{
        inputs = @{}
        query = $Query
        response_mode = "blocking"
        conversation_id = ""
        user = "manual-chatflow-test"
        files = @()
        auto_generate_name = $true
    } | ConvertTo-Json -Depth 10

    Write-Host "POST $uri"
    try {
        $response = Invoke-RestMethod `
            -Uri $uri `
            -Method Post `
            -Headers @{ Authorization = "Bearer $apiKey" } `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec 130
        Write-DifyResult -Response $response
    } catch {
        $script:Failed = $true
        Write-Host "Dify request failed: $(Get-HttpErrorBody -ErrorRecord $_)" -ForegroundColor Red
    }
}

function New-TemporaryRedisToken {
    $ping = (& wsl redis-cli ping 2>$null).Trim()
    if ($ping -ne "PONG") {
        throw "Redis is unavailable. Start Redis or pass a real login token with -Token."
    }

    $script:TemporaryToken = $true
    $script:Token = "chatflow-test-$([Guid]::NewGuid().ToString('N'))"
    $script:RedisKey = "token:$($script:Token)"
    $tokenValue = @{
        id = "chatflow-test-user"
        token = $script:Token
        userId = "chatflow-test-user"
        name = "Chatflow Test"
        updateTime = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
        ip = "127.0.0.1"
        state = "1"
    } | ConvertTo-Json -Compress

    $setResult = ($tokenValue | & wsl redis-cli -x SET $script:RedisKey).Trim()
    $expireResult = (& wsl redis-cli EXPIRE $script:RedisKey 300).Trim()
    if ($setResult -ne "OK" -or $expireResult -ne "1") {
        throw "Failed to create the temporary Redis token."
    }

    Write-Host "Temporary Redis token created (TTL: 300 seconds)." -ForegroundColor Green
}

function Remove-TemporaryRedisToken {
    if ($script:TemporaryToken -and $script:RedisKey) {
        & wsl redis-cli DEL $script:RedisKey 2>$null | Out-Null
        Write-Host "Temporary Redis token deleted."
    }
}

function Test-BackendProxy {
    Write-Host "`n=== Backend Proxy Test ===" -ForegroundColor Yellow

    if (-not $Token) {
        New-TemporaryRedisToken
        $Token = $script:Token
    }

    $uri = "$($BackendUrl.TrimEnd('/'))/api/chatflow/messages"
    $body = @{
        query = $Query
        inputs = @{}
        conversationId = ""
        files = @()
        autoGenerateName = $true
    } | ConvertTo-Json -Depth 10

    Write-Host "POST $uri"
    try {
        $response = Invoke-RestMethod `
            -Uri $uri `
            -Method Post `
            -Headers @{ token = $Token } `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec 130

        Write-Host "Backend status: $($response.status)"
        Write-Host "Backend message: $($response.msg)"
        if ($response.status -eq 200) {
            Write-Host "Answer delivered through backend: $($response.data.answer)" -ForegroundColor Cyan
        } else {
            $script:Failed = $true
            Write-Host "Backend proxy test failed." -ForegroundColor Red
        }
    } catch {
        $script:Failed = $true
        Write-Host "Backend request failed: $(Get-HttpErrorBody -ErrorRecord $_)" -ForegroundColor Red
    }
}

try {
    Import-DotEnv -Path (Join-Path $PSScriptRoot ".env")

    if ($Mode -eq "Direct" -or $Mode -eq "All") {
        Test-DifyDirect
    }
    if ($Mode -eq "Backend" -or $Mode -eq "All") {
        Test-BackendProxy
    }
} catch {
    $script:Failed = $true
    Write-Host "Test setup failed: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Remove-TemporaryRedisToken
}

if ($script:Failed) {
    exit 1
}

Write-Host "`nAll selected tests passed." -ForegroundColor Green
