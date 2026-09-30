[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$GameJar,
    [Parameter(Mandatory)][string]$ViewpointJar,
    [Parameter(Mandatory)][string]$ZombieBuddyJar
)
$ErrorActionPreference = 'Stop'
& python "$PSScriptRoot/test_contract.py"
if ($LASTEXITCODE -ne 0) { throw 'Contract checker tests failed' }
& python "$PSScriptRoot/inspect_contract.py" --java-home $JavaHome --game $GameJar --viewpoint $ViewpointJar --zombiebuddy $ZombieBuddyJar
if ($LASTEXITCODE -ne 0) { throw 'Unsupported binary or contract mismatch' }
$classes = "$PSScriptRoot/build/classes"
New-Item -ItemType Directory -Force $classes | Out-Null
$gameCopy = "$PSScriptRoot/build/inputs/game.jar"
# Explicit source list prevents inherited harness code from entering this build.
& "$JavaHome/bin/javac.exe" -Xlint:all -cp $gameCopy -d $classes "$PSScriptRoot/src/viewpointvr/StereoCamera.java" "$PSScriptRoot/test/viewpointvr/StereoCameraTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Camera compilation failed' }
& "$JavaHome/bin/java.exe" -ea -cp "$classes;$gameCopy" viewpointvr.StereoCameraTest
if ($LASTEXITCODE -ne 0) { throw 'Camera tests failed' }
Write-Host 'Offline Viewpoint baseline passed. No game/mod entry point or OpenGL context was run.'
