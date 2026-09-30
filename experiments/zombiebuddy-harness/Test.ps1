[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
& "$PSScriptRoot\Build.ps1"
$root=(Resolve-Path "$PSScriptRoot\..\..").Path
$javac=Get-ChildItem "$root\external\tools\jdk25" -Filter javac.exe -Recurse | Select-Object -First 1 -ExpandProperty FullName
$java=Join-Path (Split-Path $javac) java.exe
$jar=Join-Path (Split-Path $javac) jar.exe
$game="$root\reference\project-zomboid\42.21.0\projectzomboid.jar"
$pz="$root\reference\pz3d\0.3.0\PZ3D-0.3.0.jar"
$zb="$root\reference\zombiebuddy\binaries\ZombieBuddy.jar"
$adapter="$root\experiments\pz3d-adapter\build\classes"
$classes="$PSScriptRoot\build\classes"
$fixtures="$PSScriptRoot\build\fixtures"
$gpu="$PSScriptRoot\build\gpu-test"
$run="$PSScriptRoot\build\runs\$(Get-Date -Format yyyyMMdd-HHmmss-fff)"
New-Item -ItemType Directory -Force $fixtures,$gpu,$run | Out-Null
$sources=@(Get-ChildItem "$PSScriptRoot\tests\fixtures","$root\experiments\pz3d-adapter\tests\fixtures" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
$sources+=@("$PSScriptRoot\tests\InstrumentationAgent.java","$PSScriptRoot\tests\HarnessLifecycleTest.java","$PSScriptRoot\tests\LiveMirrorLifecycleTest.java","$PSScriptRoot\tests\XrHarnessLifecycleTest.java")
& $javac -Xlint:all -cp "$classes;$adapter;$game;$zb" -d $fixtures $sources
if($LASTEXITCODE -ne 0) { throw 'Lifecycle test compilation failed.' }
"Premain-Class: InstrumentationAgent`nCan-Retransform-Classes: true`n" | Set-Content "$PSScriptRoot\build\test-agent.mf" -Encoding ASCII
& $jar --create --file "$PSScriptRoot\build\test-agent.jar" --manifest "$PSScriptRoot\build\test-agent.mf" -C $fixtures InstrumentationAgent.class
if($LASTEXITCODE -ne 0) { throw 'Test agent packaging failed.' }
& $java '-Xverify:all' "-javaagent:$PSScriptRoot\build\test-agent.jar" -cp "$fixtures;$classes;$adapter;$game;$zb" HarnessLifecycleTest $game $pz $zb $fixtures "$run\lifecycle" | Tee-Object "$run\lifecycle.log"
if($LASTEXITCODE -ne 0) { throw 'Lifecycle tests failed.' }
& $java '-Xverify:all' "-javaagent:$PSScriptRoot\build\test-agent.jar" -cp "$fixtures;$classes;$adapter;$game;$zb" LiveMirrorLifecycleTest $game $pz $zb $fixtures "$run\mirror" | Tee-Object "$run\mirror.log"
if($LASTEXITCODE -ne 0) { throw 'Live mirror lifecycle tests failed.' }
& $java '-Xverify:all' "-javaagent:$PSScriptRoot\build\test-agent.jar" -cp "$fixtures;$classes;$adapter;$game;$zb" XrHarnessLifecycleTest $game $pz $zb $fixtures "$run\xr-lifecycle" | Tee-Object "$run\xr-lifecycle.log"
if($LASTEXITCODE -ne 0) { throw 'XR harness lifecycle tests failed.' }
$libs="$root\experiments\openxr-diagnostic\lib\*"
$gpuSources=@(Get-ChildItem "$PSScriptRoot\tests\gpu" -Filter *.java | Select-Object -ExpandProperty FullName)
$gpuSources+="$PSScriptRoot\tests\InstrumentationAgent.java"
& $javac -Xlint:all -cp "$classes;$adapter;$libs;$game;$zb" -d $gpu $gpuSources
if($LASTEXITCODE -ne 0) { throw 'GPU test compilation failed.' }
& $java '-Xverify:all' "-javaagent:$PSScriptRoot\build\test-agent.jar" -cp "$gpu;$classes;$adapter;$game;$pz;$zb" RealBinaryTransformTest $game $pz $zb | Tee-Object "$run\real-binary.log"
if($LASTEXITCODE -ne 0) { throw 'Actual copied-binary retransformation failed.' }
& $java '-Xverify:all' -cp "$gpu;$classes;$game;$pz;$zb" MeleeTransformTest $game $pz | Tee-Object "$run\melee-transform.log"
if($LASTEXITCODE -ne 0) { throw 'Melee transformation contract failed.' }
& $java -cp "$gpu;$classes" pzvr.input.GamepadMapperTest | Tee-Object "$run\gamepad-mapping.log"
if($LASTEXITCODE -ne 0) { throw "Gamepad mapping tests failed." }
& $java --enable-native-access=ALL-UNNAMED -cp "$gpu;$classes;$adapter;$libs;$game" GpuCaptureTest "$run\gpu" | Tee-Object "$run\gpu.log"
if($LASTEXITCODE -ne 0) { throw 'GPU capture checks failed.' }
& $java -cp "$gpu;$classes;$adapter;$game" XrCameraTest | Tee-Object "$run\xr-camera.log"
if($LASTEXITCODE -ne 0) { throw 'XR camera tests failed.' }
& $java -cp "$gpu;$classes" FrameTimingTest | Tee-Object "$run\xr-timing.log"
if($LASTEXITCODE -ne 0) { throw 'XR timing statistics failed.' }
& $java -cp "$gpu;$classes;$game" HotkeysTest | Tee-Object "$run\hotkeys.log"
if($LASTEXITCODE -ne 0) { throw 'Configurable hotkey checks failed.' }
& $java -cp "$gpu;$classes" RecenterCountdownTest | Tee-Object "$run\recenter-countdown.log"
if($LASTEXITCODE -ne 0) { throw 'Recenter countdown checks failed.' }
& $java -cp "$gpu;$classes;$game" SwingDetectorTest | Tee-Object "$run\melee-detector.log"
if($LASTEXITCODE -ne 0) { throw 'Motion melee detector checks failed.' }
$meleeFixtures="$PSScriptRoot\build\melee-fixtures"
New-Item -ItemType Directory -Force $meleeFixtures | Out-Null
$meleeSources=@(Get-ChildItem "$PSScriptRoot\tests\melee-fixtures" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
$meleeSources+="$PSScriptRoot\tests\MeleeRuntimeTest.java"
$meleeSources+="$PSScriptRoot\tests\ContactRuntimeTest.java"
$meleeSources+="$PSScriptRoot\tests\InstrumentationAgent.java"
& $javac -cp "$classes;$game;$zb" -d $meleeFixtures $meleeSources
if($LASTEXITCODE -ne 0) { throw 'Melee fixture compilation failed.' }
& $java '-Xverify:all' -cp "$meleeFixtures;$classes;$game" MeleeRuntimeTest | Tee-Object "$run\melee-runtime.log"
if($LASTEXITCODE -ne 0) { throw 'Melee runtime fixture checks failed.' }
& $java "-Xverify:all" "-javaagent:$PSScriptRoot\build\test-agent.jar" -cp "$meleeFixtures;$classes;$game;$zb" ContactRuntimeTest | Tee-Object "$run\contact-runtime.log"
if($LASTEXITCODE -ne 0) { throw "Contact runtime tests failed." }
& $java -cp "$gpu;$classes;$adapter;$libs;$game" ContactGeometryTest | Tee-Object "$run\contact-geometry.log"
if($LASTEXITCODE -ne 0) { throw "Contact geometry tests failed." }
& $java -cp "$gpu;$classes;$adapter;$libs;$game" ArmTrackingTest | Tee-Object "$run\arm-tracking.log"
if($LASTEXITCODE -ne 0) { throw 'Arm tracking checks failed.' }
& "$PSScriptRoot\Test-Controllers.ps1"
Write-Host "Harness evidence: $run"
& "$PSScriptRoot\Test-Turning.ps1"
& "$PSScriptRoot\Test-Interactions.ps1"
Write-Host 'All harness suites passed.'
