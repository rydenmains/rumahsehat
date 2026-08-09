# Buat GitHub Release untuk tag v1.2.0 + upload asset (APK + checksums).
# Cara pakai:
#   1. Buat PAT di github.com/settings/tokens (scope: repo)
#   2. Jalankan:  .\scripts\release.ps1 -Token <PAT>
param(
    [Parameter(Mandatory = $true)][string]$Token
)

$ErrorActionPreference = "Stop"
$repo = "rydenmains/rumahsehat"
$tag = "v1.2.0"
$headers = @{
    Authorization = "Bearer $Token"
    Accept        = "application/vnd.github+json"
}

$releaseBody = @"
Security hardening release.

- API token tidak lagi hardcoded di source (BuildConfig.API_TOKEN via local.properties/env)
- Endpoint baca data (?action=data) kini wajib token
- Foto Drive privat (tanpa link publik ANYONE_WITH_LINK)
- Signing release opsional utk CI/F-Droid; fastlane metadata siap

Verifikasi: Get-FileHash -Algorithm SHA256 RumahSehat-User.apk
"@

$payload = @{
    tag_name   = $tag
    name       = $tag
    body       = $releaseBody
    draft      = $false
    prerelease = $false
} | ConvertTo-Json

$release = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases" -Method Post -Headers $headers -Body $payload
Write-Output "Release dibuat: $($release.html_url)"

foreach ($file in @("RumahSehat-User.apk", "checksums.txt")) {
    if (-not (Test-Path $file)) { Write-Warning "Asset tidak ada: $file"; continue }
    $assetUrl = "https://uploads.github.com/repos/$repo/releases/$($release.id)/assets?name=$file"
    Invoke-RestMethod -Uri $assetUrl -Method Post -Headers $headers -ContentType "application/octet-stream" -InFile $file | Out-Null
    Write-Output "Uploaded: $file"
}
Write-Output "Selesai. Lihat release di https://github.com/$repo/releases"
