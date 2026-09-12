# Matriks uji sideload lokal: install -> launch -> monkey -> cek crash.
# Contoh: .\qa_matrix.ps1
#          .\qa_matrix.ps1 -Avds @("api26","api27","qatest") -MonkeyEvents 1000
# AVD baru: sdkmanager "system-images;android-26;google_apis;x86_64"
#           avdmanager create avd -n api26 -k "system-images;android-26;google_apis;x86_64" -d "pixel"
param(
    [string[]]$Avds = @("qatest"),
    [int]$MonkeyEvents = 500,
    [switch]$UseAttached,
    [string]$ApkPath = "app\build\outputs\apk\user\debug\app-user-debug.apk"
)

$ErrorActionPreference = "Stop"
$sdk = ($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT | Where-Object { $_ } | Select-Object -First 1)
if (-not $sdk) {
    $m = Select-String -Path "local.properties" -Pattern "^sdk\.dir=(.+)" | Select-Object -First 1
    $sdk = $m.Matches[0].Groups[1].Value.Trim() -replace '\\:', ':' -replace '\\\\', '\'
}
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emu = Join-Path $sdk "emulator\emulator.exe"
$pkg = "com.rumahsehat.user"
$act = "com.rumahsehat.ui.MainActivity"
$fail = 0

function Wait-Boot {
    for ($i = 0; $i -lt 60; $i++) {
        $b = & $adb shell getprop sys.boot_completed 2>$null
        if ("$b".Trim() -eq "1") { return $true }
        Start-Sleep -Seconds 5
    }
    return $false
}

function Test-One {
    $ErrorActionPreference = "Continue"
    Write-Output "=== $pkg di device $(& $adb devices | Select-String 'device$' | Select-Object -First 1) ==="
    & $adb install -r $ApkPath 2>&1 | Select-Object -Last 1
    & $adb logcat -c
    & $adb shell am start -n "$pkg/$act" | Select-Object -Last 1
    Start-Sleep -Seconds 8
    & $adb shell monkey -p $pkg --throttle 100 $MonkeyEvents 2>&1 | Select-Object -Last 2
    Start-Sleep -Seconds 3
    $crash = & $adb logcat -d | Select-String "FATAL EXCEPTION| E AndroidRuntime" | Select-Object -First 5
    if ($crash) { Write-Output "FAIL:"; $crash | ForEach-Object { Write-Output $_ }; $script:tc = 1; return }
    Write-Output "PASS: no crash"
    $script:tc = 0; return
}

if ($UseAttached) {
    Test-One | Write-Output; $fail += $script:tc
} else {
    foreach ($avd in $Avds) {
        Write-Output "### AVD: $avd"
        Start-Process -FilePath $emu -ArgumentList "-avd",$avd,"-no-window","-no-audio","-no-boot-anim" -WindowStyle Hidden
        if (-not (Wait-Boot)) { Write-Output "FAIL: $avd boot timeout"; $fail++; continue }
        Test-One | Write-Output; $fail += $script:tc
        & $adb emu kill
        Start-Sleep -Seconds 5
    }
}

Write-Output $(if ($fail -eq 0) { "MATRIKS HIJAU" } else { "MATRIKS MERAH: $fail gagal" })
exit $fail
