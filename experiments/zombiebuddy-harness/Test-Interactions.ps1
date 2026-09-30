$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot\..\..").Path
$javac=Get-ChildItem "$root\external\tools\jdk25" -Filter javac.exe -Recurse | Select-Object -First 1 -ExpandProperty FullName
$java=Join-Path (Split-Path $javac) java.exe
$game="$root\reference\project-zomboid\42.21.0\projectzomboid.jar"
$zb="$root\reference\zombiebuddy\binaries\ZombieBuddy.jar"
$classes="$PSScriptRoot\build\classes"
$fixtures="$PSScriptRoot\build\use-fixtures"
New-Item -ItemType Directory -Force $fixtures | Out-Null
$shared=Get-ChildItem "$PSScriptRoot\tests\turn-fixtures" -Recurse -Filter *.java | Where-Object { $_.Name -notin @('Main.java','IsoPlayer.java') } | Select-Object -ExpandProperty FullName
$sources=@($shared)+@(Get-ChildItem "$PSScriptRoot\tests\use-fixtures" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
$sources+="$PSScriptRoot\tests\HandUseTest.java","$PSScriptRoot\tests\InstrumentationAgent.java"
& $javac -cp "$classes;$game;$zb" -d $fixtures $sources
if($LASTEXITCODE -ne 0){throw 'Interaction fixture compilation failed'}
& $java '-Xverify:all' "-javaagent:$PSScriptRoot\build\test-agent.jar" -cp "$fixtures;$classes;$game;$zb" HandUseTest | Tee-Object "$PSScriptRoot\build\interaction-test.log"
if($LASTEXITCODE -ne 0){throw 'Interaction fixture checks failed'}
