# 文件说明：工程基线规范检查脚本。
# 检查项：
#   1. 源码中不得出现 System.out 与 printStackTrace（日志统一使用 SLF4J）
#   2. 每个 Java 源文件头部必须包含"文件说明"（docs/DEVELOPMENT.md 文件头规范）
#   3. 单文件不超过 300 行
# 用法：PowerShell 执行 scripts\check-baseline.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root "src"
$javaFiles = Get-ChildItem -Path $src -Recurse -Include *.java
$failed = $false

# 检查 1：禁止 System.out 与 printStackTrace
Write-Output "[检查] 扫描 System.out 与 printStackTrace ..."
$bad = $javaFiles | Select-String -Pattern "System\.out|printStackTrace"
if ($bad) {
    $failed = $true
    $bad | ForEach-Object { Write-Output ("  FAIL: {0}:{1}" -f $_.Path, $_.LineNumber) }
} else {
    Write-Output "  通过"
}

# 检查 2：Java 文件头必须包含文件说明
Write-Output "[检查] 扫描 Java 文件头文件说明 ..."
foreach ($file in $javaFiles) {
    # 指定 UTF-8 读取，避免中文文件说明匹配失败；窗口 60 行覆盖长 import 段
    $head = Get-Content -Path $file.FullName -TotalCount 60 -Encoding UTF8
    if (($head -join "`n") -notmatch "文件说明") {
        $failed = $true
        Write-Output ("  FAIL: {0} 缺少文件说明头" -f $file.FullName)
    }
}
if (-not $failed) { Write-Output "  通过" }

# 检查 3：单文件不超过 300 行
Write-Output "[检查] 单文件行数（<=300）..."
foreach ($file in $javaFiles) {
    $count = (Get-Content -Path $file.FullName -Encoding UTF8).Count
    if ($count -gt 300) {
        $failed = $true
        Write-Output ("  FAIL: {0} 共 {1} 行，超过 300 行" -f $file.FullName, $count)
    }
}
if (-not ($javaFiles | Where-Object { (Get-Content $_.FullName -Encoding UTF8).Count -gt 300 })) {
    Write-Output "  通过"
}

if ($failed) {
    Write-Output "== 基线检查未通过 =="
    exit 1
} else {
    Write-Output "== 基线检查全部通过 =="
    exit 0
}
