param([string]$BaseUrl = 'http://localhost:8088')
$ErrorActionPreference = 'Stop'

# 仅向当前本地教学服务发送虚构数据。它会保留两笔记录供前端查看，不删除数据。
function Invoke-DemoApi {
    param([string]$Method, [string]$Path, [string]$User = 'applicant01',
          [string]$Role = 'APPLICANT', [string]$Dept = 'D001', $Body = $null)
    $headers = @{ 'X-User-Id'=$User; 'X-User-Name'=$User; 'X-Role'=$Role; 'X-Dept-Id'=$Dept }
    $parameters = @{ Method=$Method; Uri=($BaseUrl.TrimEnd('/') + '/api' + $Path); Headers=$headers }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json; charset=utf-8'
        $parameters.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
    }
    $result = Invoke-RestMethod @parameters
    if (-not $result.success) { throw ('API failed: ' + $result.code) }
    return $result.data
}

function Assert-That([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw ('Assertion failed: ' + $Message) }
}

function Expect-HttpStatus([int]$Expected, [scriptblock]$Action) {
    try { & $Action | Out-Null }
    catch {
        if ($null -ne $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq $Expected) { return }
        throw
    }
    throw ('Expected HTTP ' + $Expected + ', but request succeeded')
}

$payload = @{
    mobile='13800138000'; idCard='110101199001011234'
    account='6222021234567890'; counterpartyAccount='6217001234567890'
    amount='150000.00'; currency='CNY'; purpose='Teaching purchase - synthetic data only'
}

Write-Host 'Checking four-level approval, scope and stale-version behavior...'
$tx = Invoke-DemoApi POST '/transactions' -Body $payload
$firstId = $tx.id
Assert-That ($tx.status -eq 'DRAFT') 'new application must be DRAFT'
Assert-That ($tx.mobileMasked -ne $payload.mobile) 'response must be masked'
Expect-HttpStatus 403 { Invoke-DemoApi GET "/transactions/$firstId" -User 'applicant02' -Dept 'D002' }
$tx = Invoke-DemoApi POST "/transactions/$firstId/submit" -Body @{version=$tx.version}
Expect-HttpStatus 403 {
    Invoke-DemoApi POST "/transactions/$firstId/approve" -User 'approverD002' -Role 'APPROVER_L1' -Dept 'D002' -Body @{version=$tx.version}
}
for ($level=1; $level -le 4; $level++) {
    $oldVersion = $tx.version
    $tx = Invoke-DemoApi POST "/transactions/$firstId/approve" -User ('approver0' + $level) -Role ('APPROVER_L' + $level) -Body @{version=$oldVersion; comment='Verified in demo'}
    if ($level -eq 1) {
        Expect-HttpStatus 409 {
            Invoke-DemoApi POST "/transactions/$firstId/approve" -User 'approver01' -Role 'APPROVER_L1' -Body @{version=$oldVersion}
        }
    }
}
Assert-That ($tx.status -eq 'APPROVED' -and $tx.version -eq 5) 'four approvals must finish at version 5'
$approvals = @(Invoke-DemoApi GET "/transactions/$firstId/approvals" -User 'auditor01' -Role 'AUDITOR' -Dept 'AUDIT')
$audits = @(Invoke-DemoApi GET "/transactions/$firstId/audits" -User 'auditor01' -Role 'AUDITOR' -Dept 'AUDIT')
Assert-That ($approvals.Count -eq 4 -and $audits.Count -eq 6) 'approval and audit counts'

Write-Host 'Checking reject / edit / resubmit...'
$tx = Invoke-DemoApi POST '/transactions' -Body $payload
$secondId = $tx.id
$tx = Invoke-DemoApi POST "/transactions/$secondId/submit" -Body @{version=$tx.version}
$tx = Invoke-DemoApi POST "/transactions/$secondId/reject" -User 'approver01' -Role 'APPROVER_L1' -Body @{version=$tx.version; reason='Please clarify the contract'}
Assert-That ($tx.status -eq 'REJECTED') 'reject must persist'
$payload.version = $tx.version
$payload.purpose = 'Supplementary teaching purchase contract'
$tx = Invoke-DemoApi POST "/transactions/$secondId/resubmit" -Body $payload
Assert-That ($tx.status -eq 'PENDING_L1' -and $tx.version -eq 3) 'resubmit restarts at L1'
Write-Host "PASS: approved application=$firstId; resubmitted application=$secondId"
