# ==============================================================================
# 高效Todo (纯净离线版) - 正式版 Release 构建脚本
# ==============================================================================

$proj = $PSScriptRoot
$tc = "$proj\..\android_toolchain"
if (-not (Test-Path "$tc\aapt2.exe")) {
    $tc = "C:\Users\Administrator\.gemini\antigravity\scratch\android_toolchain"
}

$java = "D:\Program Files\IntelliJ IDEA 2024.3.4\jbr\bin\java.exe"
if (-not (Test-Path $java)) { $java = "java.exe" }

$javac = "D:\Program Files\jdk1.8\bin\javac.exe"
if (-not (Test-Path $javac)) { $javac = "javac.exe" }

$ks = "$proj\keystore\release.jks"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ">>> 开始构建 Release 正式发布包 (v1.0.0)..." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. 清理目录
$buildDir = "$proj\build-release"
$releaseDir = "$proj\release"
if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
if (-not (Test-Path $releaseDir)) { New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null }
New-Item -ItemType Directory -Path "$buildDir\gen" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\obj" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\dex" -Force | Out-Null
New-Item -ItemType Directory -Path "$buildDir\apk" -Force | Out-Null

# 2. 编译与链接资源
Write-Host ">>> [1/5] 编译与链接资源 (aapt2)..." -ForegroundColor Green
& "$tc\aapt2.exe" compile --dir "$proj\res" -o "$buildDir\compiled_res.zip"
if ($LASTEXITCODE -ne 0) { Write-Error "aapt2 compile failed"; exit 1 }

& "$tc\aapt2.exe" link -o "$buildDir\base_res.apk" -I "$tc\android.jar" --manifest "$proj\AndroidManifest.xml" "$buildDir\compiled_res.zip" --java "$buildDir\gen" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { Write-Error "aapt2 link failed"; exit 1 }

# 3. 编译 Java (Release 优化)
Write-Host ">>> [2/5] 编译 Java 源代码 (javac -g:none)..." -ForegroundColor Green
$javaFiles = Get-ChildItem -Path "$proj\src", "$buildDir\gen" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 -source 1.8 -target 1.8 -g:none -bootclasspath "$tc\android.jar" -cp "$tc\android.jar" -d "$buildDir\obj" $javaFiles
if ($LASTEXITCODE -ne 0) { Write-Error "javac failed"; exit 1 }

# 4. D8 编译 DEX (Release 模式优化)
Write-Host ">>> [3/5] D8 Release 模式编译 DEX 字节码..." -ForegroundColor Green
$classFiles = Get-ChildItem -Path "$buildDir\obj" -Filter "*.class" -Recurse | Select-Object -ExpandProperty FullName
& $java -cp "$tc\r8.jar" com.android.tools.r8.D8 --release --output "$buildDir\dex" --lib "$tc\android.jar" --min-api 21 $classFiles
if ($LASTEXITCODE -ne 0) { Write-Error "D8 dex failed"; exit 1 }

# 5. 打包 APK
Write-Host ">>> [4/5] 封装 APK 安装包..." -ForegroundColor Green
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression") | Out-Null
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem") | Out-Null
$unsignedApk = "$buildDir\apk\app-release-unsigned.apk"
Copy-Item "$buildDir\base_res.apk" $unsignedApk -Force

$zip = [System.IO.Compression.ZipFile]::Open($unsignedApk, [System.IO.Compression.ZipArchiveMode]::Update)
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$buildDir\dex\classes.dex", "classes.dex")
$zip.Dispose()

# 6. 正式发布签名 (使用专属 release.jks 签名，注入 v1, v2, v3 签名)
Write-Host ">>> [5/5] 使用正式签名证书进行 Release 签名与 4 字节对齐..." -ForegroundColor Green
& $java -jar "$tc\uber-apk-signer.jar" `
  -a "$unsignedApk" `
  -o "$buildDir\apk\signed" `
  --ks "$ks" `
  --ksAlias "todo" `
  --ksPass "todokey123456" `
  --ksKeyPass "todokey123456" `
  --allowResign `
  --verbose
if ($LASTEXITCODE -ne 0) { Write-Error "Release signing failed"; exit 1 }

# 7. 产物输出与哈希计算
$signedApk = Get-ChildItem -Path "$buildDir\apk\signed" -Filter "*aligned-signed.apk" | Select-Object -First 1 -ExpandProperty FullName
if (-not $signedApk) {
    $signedApk = Get-ChildItem -Path "$buildDir\apk\signed" -Filter "*.apk" | Select-Object -First 1 -ExpandProperty FullName
}

$releaseTarget = "$releaseDir\todo-v1.0.0-release.apk"
Copy-Item $signedApk $releaseTarget -Force
Copy-Item $signedApk "D:\todo-v1.0.0-release.apk" -Force
Copy-Item $signedApk "D:\clean_todo_offline.apk" -Force

$sha256 = (Get-FileHash -Path $releaseTarget -Algorithm SHA256).Hash
$md5 = (Get-FileHash -Path $releaseTarget -Algorithm MD5).Hash
$size = (Get-Item $releaseTarget).Length

$checksumContent = @"
# Todo Offline Release Checksums (v1.0.0)
File: todo-v1.0.0-release.apk
Size: $size bytes ($([Math]::Round($size / 1024, 2)) KB)
MD5: $md5
SHA256: $sha256
"@
Set-Content -Path "$releaseDir\checksums.txt" -Value $checksumContent -Encoding UTF8

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "✅ Release 正式发布包生成完毕！" -ForegroundColor Green
Write-Host "产物文件: $releaseTarget" -ForegroundColor Green
Write-Host "大小: $([Math]::Round($size / 1024, 2)) KB" -ForegroundColor Green
Write-Host "SHA256: $sha256" -ForegroundColor Green
Write-Host "MD5:    $md5" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
