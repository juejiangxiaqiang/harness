# 文件说明：qwen / OpenAI 兼容 Embedding 连通性检查脚本。
# 读取工程根目录 .env 中的 OPENAI_BASE_URL / OPENAI_API_KEY / OPENAI_EMBEDDING_MODEL，
# 调用 {base-url}/embeddings 并输出响应模型、向量维度与耗时。
# 用法：PowerShell 执行 scripts\check-embedding.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"
if (-not (Test-Path $envFile)) {
    Write-Output "未找到 .env 文件"
    exit 1
}

# 解析 .env（KEY=VALUE 属性格式）
$vars = @{}
Get-Content $envFile -Encoding UTF8 | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $kv = $_ -split '=', 2
    $vars[$kv[0]] = $kv[1]
}

$baseUrl = $vars["OPENAI_BASE_URL"]
$apiKey = $vars["OPENAI_API_KEY"]
$model = $vars["OPENAI_EMBEDDING_MODEL"]
if (-not $baseUrl -or -not $apiKey) {
    Write-Output "缺少 OPENAI_BASE_URL 或 OPENAI_API_KEY"
    exit 1
}

$url = $baseUrl.TrimEnd('/') + "/embeddings"
$body = @{ model = $model; input = @("连通性测试文本") } | ConvertTo-Json -Depth 5
$sw = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $resp = Invoke-RestMethod -Uri $url -Method Post `
        -Headers @{ Authorization = "Bearer $apiKey" } `
        -ContentType "application/json" -Body $body -TimeoutSec 30
    $sw.Stop()
    Write-Output "HTTP 调用成功，耗时 $($sw.ElapsedMilliseconds)ms"
    Write-Output "模型: $($resp.model)"
    Write-Output "向量维度: $($resp.data[0].embedding.Count)"
    Write-Output "前 8 个分量: $($resp.data[0].embedding[0..7] -join ', ')"
} catch {
    $sw.Stop()
    Write-Output "调用失败（$($sw.ElapsedMilliseconds)ms）: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) {
        Write-Output "响应体: $($_.ErrorDetails.Message)"
    }
    exit 1
}
