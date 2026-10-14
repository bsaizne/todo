# ==============================================================================
# 高效Todo (纯净离线版) - 自动化构建与强签名脚本
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

Write-Host ">>> 1. 清理并初始化构建目录..." -ForegroundColor Cyan
if (Test-Path "$proj\build") { Remove-Item "$proj\build" -Recurse -Force }
New-Item -ItemType Directory -Path "$proj\build\gen" -Force | Out-Null
New-Item -ItemType Directory -Path "$proj\build\obj" -Force | Out-Null
New-Item -ItemType Directory -Path "$proj\build\dex" -Force | Out-Null
New-Item -ItemType Directory -Path "$proj\build\apk" -Force | Out-Null

Write-Host ">>> 2. 编译 Android 资源 (aapt2 compile)..." -ForegroundColor Cyan
& "$tc\aapt2.exe" compile --dir "$proj\res" -o "$proj\build\compiled_res.zip"
if ($LASTEXITCODE -ne 0) { Write-Error "aapt2 compile failed"; exit 1 }

Write-Host ">>> 3. 链接资源并生成 R.java (aapt2 link)..." -ForegroundColor Cyan
& "$tc\aapt2.exe" link -o "$proj\build\base_res.apk" -I "$tc\android.jar" --manifest "$proj\AndroidManifest.xml" "$proj\build\compiled_res.zip" --java "$proj\build\gen" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { Write-Error "aapt2 link failed"; exit 1 }

Write-Host ">>> 4. 编译 Java 源代码 (javac)..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path "$proj\src", "$proj\build\gen" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& $javac -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$tc\android.jar" -cp "$tc\android.jar" -d "$proj\build\obj" $javaFiles
if ($LASTEXITCODE -ne 0) { Write-Error "javac failed"; exit 1 }

Write-Host ">>> 5. 编译 DEX 字节码 (D8)..." -ForegroundColor Cyan
$classFiles = Get-ChildItem -Path "$proj\build\obj" -Filter "*.class" -Recurse | Select-Object -ExpandProperty FullName
& $java -cp "$tc\r8.jar" com.android.tools.r8.D8 --output "$proj\build\dex" --lib "$tc\android.jar" --min-api 21 $classFiles
if ($LASTEXITCODE -ne 0) { Write-Error "D8 dex failed"; exit 1 }

Write-Host ">>> 6. 打包 classes.dex 到 APK..." -ForegroundColor Cyan
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression") | Out-Null
[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem") | Out-Null
$unsignedApk = "$proj\build\apk\app-unsigned.apk"
Copy-Item "$proj\build\base_res.apk" $unsignedApk -Force

$zip = [System.IO.Compression.ZipFile]::Open($unsignedApk, [System.IO.Compression.ZipArchiveMode]::Update)
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$proj\build\dex\classes.dex", "classes.dex")
$zip.Dispose()

Write-Host ">>> 7. 对齐并签名 APK (uber-apk-signer, v1/v2/v3 强签名)..." -ForegroundColor Cyan
& $java -jar "$tc\uber-apk-signer.jar" -a "$proj\build\apk\app-unsigned.apk" -o "$proj\build\apk\signed" --allowResign --verbose
if ($LASTEXITCODE -ne 0) { Write-Error "APK signing failed"; exit 1 }

$signedApk = Get-ChildItem -Path "$proj\build\apk\signed" -Filter "*aligned-debugSigned.apk" | Select-Object -First 1 -ExpandProperty FullName
$targetOutput = "D:\clean_todo_offline.apk"
Copy-Item $signedApk $targetOutput -Force

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "构建成功！" -ForegroundColor Green
Write-Host "输出路径: $targetOutput" -ForegroundColor Green
Write-Host "大小: $((Get-Item $targetOutput).Length / 1KB) KB" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
