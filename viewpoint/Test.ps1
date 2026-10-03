[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$GameJar,
    [Parameter(Mandatory)][string]$ViewpointJar,
    [Parameter(Mandatory)][string]$ZombieBuddyJar,
    [string]$LwjglLibDirectory
)
$ErrorActionPreference = 'Stop'
& python "$PSScriptRoot/test_contract.py"
if ($LASTEXITCODE -ne 0) { throw 'Contract checker tests failed' }
& python "$PSScriptRoot/inspect_contract.py" --java-home $JavaHome --game $GameJar --viewpoint $ViewpointJar --zombiebuddy $ZombieBuddyJar
if ($LASTEXITCODE -ne 0) { throw 'Unsupported binary or contract mismatch' }
$run = Join-Path "$PSScriptRoot/build/runs" ([guid]::NewGuid().ToString('N'))
$classes = "$run/classes"
$tests = "$run/tests"
$fixtures = "$run/fixtures"
$stageFixtures = "$run/stage-fixtures"
New-Item -ItemType Directory -Force $classes,$tests,$fixtures,$stageFixtures | Out-Null
$gameCopy = "$PSScriptRoot/build/inputs/game.jar"
$loaderCopy = "$PSScriptRoot/build/inputs/zombiebuddy.jar"
$viewpointCopy = "$PSScriptRoot/build/inputs/viewpoint.jar"
$xrJar = (Resolve-Path (Join-Path $LwjglLibDirectory 'lwjgl-openxr-3.4.1.jar')).Path
$xrNative = (Resolve-Path (Join-Path $LwjglLibDirectory 'lwjgl-openxr-3.4.1-natives-windows.jar')).Path
if ((Get-FileHash $xrJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne '184ff11f6140bc48b722b5dfadb1a9611dd9f7b4f18863f627581cdda8a055f2') {throw 'OpenXR binding hash mismatch'}
if ((Get-FileHash $xrNative -Algorithm SHA256).Hash.ToLowerInvariant() -ne '2884e3449ac10e366cf80f9a2676e822d54bda7f50ff9a61d2d2d32821ee7ffd') {throw 'OpenXR native hash mismatch'}
$sources = Get-ChildItem "$PSScriptRoot/src" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& "$JavaHome/bin/javac.exe" -Xlint:all -cp "$gameCopy;$loaderCopy;$xrJar" -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Baseline compilation failed' }
$testSources = Get-ChildItem "$PSScriptRoot/test" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& "$JavaHome/bin/javac.exe" -Xlint:all -cp "$classes;$gameCopy;$loaderCopy;$xrJar" -d $tests $testSources
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
& "$JavaHome/bin/javac.exe" -Xlint:all -d $fixtures "$PSScriptRoot/test-fixtures/viewpoint/SceneDrawer.java"
if ($LASTEXITCODE -ne 0) { throw 'Synthetic fixture compilation failed' }
$stageSources = Get-ChildItem "$PSScriptRoot/stage-fixtures" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
# Related package-private doubles intentionally share StageSupport.java.
& "$JavaHome/bin/javac.exe" -Xlint:all,-auxiliaryclass -cp $gameCopy -d $stageFixtures $stageSources
if ($LASTEXITCODE -ne 0) { throw 'Native stage fixture compilation failed' }
$inputFixtures = "$run/input-fixtures"
New-Item -ItemType Directory -Force $inputFixtures | Out-Null
$inputSources = Get-ChildItem "$PSScriptRoot/input-fixtures" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
& "$JavaHome/bin/javac.exe" -Xlint:all -cp "$classes;$gameCopy" -d $inputFixtures $inputSources
if ($LASTEXITCODE -ne 0) { throw 'Input fixture compilation failed' }
& "$JavaHome/bin/java.exe" -Xverify:all -ea -cp "$inputFixtures;$classes;$gameCopy" sim.InputIntegration
if ($LASTEXITCODE -ne 0) { throw 'Native input integration failed' }
$testClasspath = "$tests;$classes;$gameCopy;$loaderCopy;$xrJar"
foreach ($test in @('viewpointvr.diagnostic.SettingsStoreTest','viewpointvr.input.TurningTest','viewpointvr.input.ControllerTest','viewpointvr.UiBridgeTest','viewpointvr.StereoCameraTest','viewpointvr.StereoFrameTest','viewpointvr.instrument.EntryFixtureTest','viewpointvr.xr.XrCameraTest','viewpointvr.diagnostic.RuntimeDriverTest')) {
    & "$JavaHome/bin/java.exe" -Xverify:all -ea -cp $testClasspath $test $fixtures
    if ($LASTEXITCODE -ne 0) { throw "Failed: $test" }
}
& "$JavaHome/bin/java.exe" -Xverify:all -ea -cp $testClasspath viewpointvr.instrument.StageFixtureTest $stageFixtures
if ($LASTEXITCODE -ne 0) { throw 'Native stage fixture failed' }
& "$JavaHome/bin/java.exe" -Xverify:all -ea -cp $testClasspath viewpointvr.diagnostic.LuaSyntaxTest "$PSScriptRoot/mod/42/media/lua/client/ProjectViewpointVR.lua"
if ($LASTEXITCODE -ne 0) { throw 'Lua controls syntax check failed' }
& "$JavaHome/bin/java.exe" -Xverify:all -ea -cp $testClasspath viewpointvr.diagnostic.CaptureTest $run
if ($LASTEXITCODE -ne 0) { throw 'Capture/controller fixture failed' }
@'
Premain-Class: viewpointvr.instrument.TestAgent
Can-Retransform-Classes: true

'@ | Set-Content "$run/agent.mf" -Encoding ASCII
& "$JavaHome/bin/jar.exe" --create --file "$run/test-agent.jar" --manifest "$run/agent.mf" -C $tests viewpointvr/instrument/TestAgent.class
if ($LASTEXITCODE -ne 0) { throw 'Test agent packaging failed' }
# Use a relative logging path: HotSpot -Xlog parses the colon in a Windows absolute path as an option separator.
Push-Location $run
try {
    & "$JavaHome/bin/java.exe" -Xverify:all -ea '-Xlog:class+init=info:file=class-init.log' "-javaagent:$run/test-agent.jar" -cp "$testClasspath;$viewpointCopy" viewpointvr.instrument.CopiedBinaryTest $viewpointCopy
    if ($LASTEXITCODE -ne 0) { throw 'Copied-binary retransformation failed' }
    & "$JavaHome/bin/java.exe" -Xverify:all -ea '-Xlog:class+init=info:file=stage-init.log' "-javaagent:$run/test-agent.jar" -cp "$testClasspath;$viewpointCopy" viewpointvr.instrument.StageBinaryTest $viewpointCopy
    if ($LASTEXITCODE -ne 0) { throw 'Native stage retransformation failed' }
    & "$JavaHome/bin/java.exe" -Xverify:all -ea '-Xlog:class+init=info:file=install-init.log' "-javaagent:$run/test-agent.jar" -cp "$testClasspath;$viewpointCopy" viewpointvr.instrument.InstallTest $viewpointCopy $gameCopy $loaderCopy
    if ($LASTEXITCODE -ne 0) { throw 'Loader activation/rollback failed' }
} finally { Pop-Location }
& python "$PSScriptRoot/verify_init_log.py" "$run/class-init.log"
if ($LASTEXITCODE -ne 0) { throw 'Initialization boundary violated' }
& python "$PSScriptRoot/verify_init_log.py" "$run/stage-init.log"
if ($LASTEXITCODE -ne 0) { throw 'Native stage initialization boundary violated' }
& python "$PSScriptRoot/verify_init_log.py" "$run/install-init.log"
if ($LASTEXITCODE -ne 0) { throw 'Loader initialization boundary violated' }
$gpuPassed = $false
if ($LwjglLibDirectory) {
    $glJars = @('lwjgl-3.4.1.jar','lwjgl-glfw-3.4.1.jar','lwjgl-opengl-3.4.1.jar','lwjgl-3.4.1-natives-windows.jar','lwjgl-glfw-3.4.1-natives-windows.jar','lwjgl-opengl-3.4.1-natives-windows.jar') | ForEach-Object { (Resolve-Path (Join-Path $LwjglLibDirectory $_)).Path }
    $glClasspath = $glJars -join ';'
    & "$JavaHome/bin/java.exe" --enable-native-access=ALL-UNNAMED -ea -cp "$tests;$classes;$glClasspath;$gameCopy" viewpointvr.diagnostic.GpuOutputTest "$run/gpu"
    if ($LASTEXITCODE -ne 0) { throw 'Standalone hidden OpenGL test failed' }
    $previousRuntime=$env:XR_RUNTIME_JSON
    try {
        $env:XR_RUNTIME_JSON=Join-Path $run 'viewpoint-no-runtime.json'
        & "$JavaHome/bin/java.exe" --enable-native-access=ALL-UNNAMED -ea -cp "$tests;$classes;$glClasspath;$xrJar;$xrNative" viewpointvr.xr.MissingRuntimeTest
        if ($LASTEXITCODE -ne 0) {throw 'OpenXR missing-runtime test failed'}
    } finally {$env:XR_RUNTIME_JSON=$previousRuntime}
    $gpuPassed = $true
}
& "$JavaHome/bin/jar.exe" --create --file "$run/viewpoint-vr-core.jar" -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Core packaging failed' }
Write-Host "Test evidence and non-installable core JAR: $run"
@{ run=$run; classes=$classes; gpuPassed=$gpuPassed; xrJar=$xrJar; xrNative=$xrNative } | ConvertTo-Json | Set-Content "$PSScriptRoot/build/latest-run.json" -Encoding UTF8
Write-Host 'Viewpoint tests passed. No game/mod entry point or SteamVR was launched.'
