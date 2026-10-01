[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$GameJar,
    [Parameter(Mandatory)][string]$ViewpointJar,
    [Parameter(Mandatory)][string]$ZombieBuddyJar,
    [Parameter(Mandatory)][string]$LwjglLibDirectory
)
$ErrorActionPreference='Stop'
& "$PSScriptRoot/Test.ps1" -JavaHome $JavaHome -GameJar $GameJar -ViewpointJar $ViewpointJar -ZombieBuddyJar $ZombieBuddyJar -LwjglLibDirectory $LwjglLibDirectory
& python "$PSScriptRoot/package.py"
if ($LASTEXITCODE -ne 0) { throw 'Diagnostic packaging failed' }
