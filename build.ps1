param([switch]$CheckOnly)
$ErrorActionPreference='Stop'
if(-not $env:JAVA_HOME){throw 'Set JAVA_HOME to JDK 21.'}
if(-not $env:ANDROID_HOME){throw 'Set ANDROID_HOME to an Android SDK with Platform 36.'}
Push-Location $PSScriptRoot
try {
    if(-not $CheckOnly){
        python tools/fetch_assets.py --all --verify-only
        if($LASTEXITCODE -ne 0){throw 'Assets missing or changed. Run: python tools/fetch_assets.py --all'}
    }
    $tasks=@(':vision:testDebugUnitTest',':offlinevoice:testDebugUnitTest',':app:lintDebug')
    if(-not $CheckOnly){$tasks+= ':app:assembleDebug'}
    & (Join-Path $PSScriptRoot 'gradlew.bat') @tasks --no-daemon --console=plain
    if($LASTEXITCODE -ne 0){throw 'Gradle checks failed.'}
} finally {Pop-Location}
