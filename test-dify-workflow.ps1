param(
    [ValidateSet("Direct", "Backend", "All")]
    [string]$Mode = "All",

    [string]$InputsJson = "{}",

    [string]$DocPath = "",

    [string]$DocUrl = "",

    [string]$Plan = "",

    [string]$Token = "",

    [string]$BackendUrl = "http://127.0.0.1:5678"
)

$ErrorActionPreference = "Stop"
$script:Failed = $false
$script:TemporaryToken = $false
$script:RedisKey = $null
$script:BackendUserId = $null

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

function Convert-InputsJson {
    try {
        $inputs = $InputsJson | ConvertFrom-Json
    } catch {
        throw "InputsJson is not valid JSON: $($_.Exception.Message)"
    }

    if ($null -eq $inputs) {
        return [PSCustomObject]@{}
    }
    return $inputs
}

function Set-InputValue {
    param(
        $Inputs,
        [string]$Name,
        $Value
    )

    if ($Inputs.PSObject.Properties.Name -contains $Name) {
        $Inputs.$Name = $Value
    } else {
        $Inputs | Add-Member -NotePropertyName $Name -NotePropertyValue $Value
    }
}

function Upload-WorkflowFile {
    param(
        [string]$Path,
        [string]$UserId
    )

    $resolvedPath = Resolve-Path -LiteralPath $Path -ErrorAction Stop
    $file = Get-Item -LiteralPath $resolvedPath.Path
    if ($file.PSIsContainer) {
        throw "DocPath must point to a file: $Path"
    }

    $uri = "$($env:DIFY_WORKFLOW_BASE_URL.TrimEnd('/'))/files/upload"
    Write-Host "Uploading document: $($file.Name)"
    $response = Invoke-RestMethod `
        -Uri $uri `
        -Method Post `
        -Headers @{ Authorization = "Bearer $env:DIFY_WORKFLOW_API_KEY" } `
        -Form @{ file = $file; user = $UserId } `
        -TimeoutSec 130

    if ([string]::IsNullOrWhiteSpace($response.id)) {
        throw "Dify file upload did not return a file ID."
    }
    Write-Host "Document uploaded. File ID: $($response.id)" -ForegroundColor Green
    return [PSCustomObject]@{
        transfer_method = "local_file"
        upload_file_id = $response.id
        type = "document"
    }
}

function New-WorkflowInputs {
    param(
        $BaseInputs,
        [string]$UserId
    )

    $inputs = ($BaseInputs | ConvertTo-Json -Depth 20) | ConvertFrom-Json
    if ($DocPath -and $DocUrl) {
        throw "Use either DocPath or DocUrl, not both."
    }

    if ($DocPath) {
        Set-InputValue -Inputs $inputs -Name "doc" -Value (Upload-WorkflowFile -Path $DocPath -UserId $UserId)
    } elseif ($DocUrl) {
        Set-InputValue -Inputs $inputs -Name "doc" -Value ([PSCustomObject]@{
            transfer_method = "remote_url"
            url = $DocUrl
            type = "document"
        })
    }

    if ($Plan) {
        Set-InputValue -Inputs $inputs -Name "plan" -Value $Plan
    }

    if (-not ($inputs.PSObject.Properties.Name -contains "doc")) {
        throw "Workflow requires doc. Pass -DocPath, -DocUrl, or include a doc file object in -InputsJson."
    }
    return $inputs
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

function Write-WorkflowResult {
    param($Response)

    Write-Host "Workflow run ID: $($Response.workflow_run_id)"
    Write-Host "Task ID: $($Response.task_id)"
    Write-Host "Workflow status: $($Response.data.status)"
    if ($Response.data.error) {
        Write-Host "Workflow error: $($Response.data.error)" -ForegroundColor Red
    }
    Write-Host "Outputs:" -ForegroundColor Cyan
    $Response.data.outputs | ConvertTo-Json -Depth 20
}

function Test-DifyDirect {
    param($BaseInputs)

    Write-Host "`n=== Direct Dify Workflow Test ===" -ForegroundColor Yellow

    if ([string]::IsNullOrWhiteSpace($env:DIFY_WORKFLOW_BASE_URL)) {
        throw "DIFY_WORKFLOW_BASE_URL is not configured in .env"
    }
    if ([string]::IsNullOrWhiteSpace($env:DIFY_WORKFLOW_API_KEY)) {
        throw "DIFY_WORKFLOW_API_KEY is not configured in .env"
    }

    $baseUri = [Uri]$env:DIFY_WORKFLOW_BASE_URL
    if (-not (Test-TcpEndpoint -Uri $baseUri)) {
        $script:Failed = $true
        return
    }

    $uri = "$($env:DIFY_WORKFLOW_BASE_URL.TrimEnd('/'))/workflows/run"
    $inputs = New-WorkflowInputs -BaseInputs $BaseInputs -UserId "manual-workflow-test"
    $body = @{
        inputs = $inputs
        response_mode = "blocking"
        user = "manual-workflow-test"
    } | ConvertTo-Json -Depth 20

    Write-Host "POST $uri"
    try {
        $response = Invoke-RestMethod `
            -Uri $uri `
            -Method Post `
            -Headers @{ Authorization = "Bearer $env:DIFY_WORKFLOW_API_KEY" } `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec 130
        Write-WorkflowResult -Response $response
        if ($response.data.status -ne "succeeded") {
            $script:Failed = $true
        }
    } catch {
        $script:Failed = $true
        Write-Host "Dify Workflow request failed: $(Get-HttpErrorBody -ErrorRecord $_)" -ForegroundColor Red
    }
}

function New-TemporaryRedisToken {
    $ping = (& wsl redis-cli ping 2>$null).Trim()
    if ($ping -ne "PONG") {
        throw "Redis is unavailable. Start Redis or pass a real login token with -Token."
    }

    $script:TemporaryToken = $true
    $script:Token = "workflow-test-$([Guid]::NewGuid().ToString('N'))"
    $script:RedisKey = "token:$($script:Token)"
    $script:BackendUserId = "workflow-test-user"
    $tokenValue = @{
        id = $script:BackendUserId
        token = $script:Token
        userId = $script:BackendUserId
        name = "Workflow Test"
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

function Get-TokenUserId {
    param([string]$LoginToken)

    $rawValue = (& wsl redis-cli GET "token:$LoginToken" 2>$null)
    if ([string]::IsNullOrWhiteSpace($rawValue)) {
        throw "The supplied Token was not found in local Redis."
    }
    try {
        return ($rawValue | ConvertFrom-Json).id
    } catch {
        throw "Unable to read the supplied Token user from Redis."
    }
}

function Remove-TemporaryRedisToken {
    if ($script:TemporaryToken -and $script:RedisKey) {
        & wsl redis-cli DEL $script:RedisKey 2>$null | Out-Null
        Write-Host "Temporary Redis token deleted."
    }
}

function Test-BackendProxy {
    param($BaseInputs)

    Write-Host "`n=== Backend Workflow Proxy Test ===" -ForegroundColor Yellow

    if (-not $Token) {
        New-TemporaryRedisToken
        $Token = $script:Token
    } else {
        $script:BackendUserId = Get-TokenUserId -LoginToken $Token
    }

    $uri = "$($BackendUrl.TrimEnd('/'))/api/workflow/run"
    $inputs = New-WorkflowInputs -BaseInputs $BaseInputs -UserId $script:BackendUserId
    $body = @{ inputs = $inputs } | ConvertTo-Json -Depth 20

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
            Write-WorkflowResult -Response $response.data
            if ($response.data.data.status -ne "succeeded") {
                $script:Failed = $true
            }
        } else {
            $script:Failed = $true
            Write-Host "Backend Workflow proxy test failed." -ForegroundColor Red
        }
    } catch {
        $script:Failed = $true
        Write-Host "Backend request failed: $(Get-HttpErrorBody -ErrorRecord $_)" -ForegroundColor Red
    }
}

try {
    Import-DotEnv -Path (Join-Path $PSScriptRoot ".env")
    $inputs = Convert-InputsJson

    if ($Mode -eq "Direct" -or $Mode -eq "All") {
        Test-DifyDirect -BaseInputs $inputs
    }
    if ($Mode -eq "Backend" -or $Mode -eq "All") {
        Test-BackendProxy -BaseInputs $inputs
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
