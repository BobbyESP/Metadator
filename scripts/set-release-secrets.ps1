<#
.SYNOPSIS
    Sets the GitHub secrets the Release workflow signs the app with.

.DESCRIPTION
    Checks the keystore and its password with keytool, reads the key's alias from it, and sets
    SIGNING_KEY_STORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS and SIGNING_KEY_PASSWORD
    with the GitHub CLI. Nothing is written to disk.

.PARAMETER KeystorePath
    The keystore (.jks or .keystore).

.PARAMETER StorePassword
    The keystore's password.

.PARAMETER KeyPassword
    The key's password, when it is not the keystore's.

.PARAMETER Alias
    The key's alias. Only needed when the keystore holds more than one key, or without keytool.

.PARAMETER GoogleServicesJson
    app/google-services.json, to also set GOOGLE_SERVICES_JSON_BASE64 (the playstore flavor).

.PARAMETER Repo
    owner/name. Defaults to the repository of the current directory.

.PARAMETER PrintOnly
    Print the secrets instead of setting them.

.EXAMPLE
    ./scripts/set-release-secrets.ps1 C:\keys\metadator_keystore.jks 'the password'
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory, Position = 0)] [string] $KeystorePath,
    [Parameter(Mandatory, Position = 1)] [string] $StorePassword,
    [string] $KeyPassword = $StorePassword,
    [string] $Alias,
    [string] $GoogleServicesJson,
    [string] $Repo,
    [switch] $PrintOnly
)

$ErrorActionPreference = 'Stop'

function Find-Keytool {
    $command = Get-Command keytool -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin\keytool.exe' }
    $candidates += 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) { return $candidate }
    }
    return $null
}

function ConvertTo-Base64([string] $Path) {
    [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path $Path).ProviderPath))
}

if (-not (Test-Path $KeystorePath -PathType Leaf)) { throw "No keystore at '$KeystorePath'." }
if ($GoogleServicesJson -and -not (Test-Path $GoogleServicesJson -PathType Leaf)) {
    throw "No file at '$GoogleServicesJson'."
}

# A wrong password or alias would only show up as a failed release build, so check them here.
$keytool = Find-Keytool
if ($keytool) {
    # Passed through the environment: an argument is visible to every process on the machine.
    $env:METADATOR_STORE_PASSWORD = $StorePassword
    try {
        # English output, whatever the system's language: the entry type is matched below.
        $listing =
            & $keytool '-J-Duser.language=en' -list -keystore $KeystorePath `
                -storepass:env METADATOR_STORE_PASSWORD
        if ($LASTEXITCODE -ne 0) {
            throw "keytool could not open the keystore. Wrong password?`n$($listing -join "`n")"
        }
    } finally {
        Remove-Item Env:METADATOR_STORE_PASSWORD
    }

    $aliases =
        @($listing | ForEach-Object { if ($_ -match '^(.+?),.*PrivateKeyEntry') { $Matches[1] } })
    if ($aliases.Count -eq 0) { throw 'The keystore holds no private key.' }

    if (-not $Alias) {
        if ($aliases.Count -gt 1) {
            throw "The keystore holds several keys ($($aliases -join ', ')). Choose one with -Alias."
        }
        $Alias = $aliases[0]
    } elseif ($aliases -notcontains $Alias) {
        throw "No key '$Alias' in the keystore. It holds: $($aliases -join ', ')."
    }
} elseif (-not $Alias) {
    throw 'keytool was not found, so the alias cannot be read from the keystore. Pass -Alias.'
} else {
    Write-Warning 'keytool was not found: the password and the alias are not checked.'
}

$secrets = [ordered]@{
    SIGNING_KEY_STORE_BASE64 = ConvertTo-Base64 $KeystorePath
    SIGNING_STORE_PASSWORD   = $StorePassword
    SIGNING_KEY_ALIAS        = $Alias
    SIGNING_KEY_PASSWORD     = $KeyPassword
}

if ($GoogleServicesJson) {
    $secrets.GOOGLE_SERVICES_JSON_BASE64 = ConvertTo-Base64 $GoogleServicesJson
}

if ($PrintOnly) {
    foreach ($name in $secrets.Keys) { "$name=$($secrets[$name])" }
    return
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'The GitHub CLI (gh) was not found. Install it, or use -PrintOnly and paste the values.'
}

$repoArguments = @()
if ($Repo) { $repoArguments = @('--repo', $Repo) }

# Through stdin, as UTF-8: an argument would be visible to other processes, and Windows
# PowerShell's default encoding would turn a non-ASCII password into question marks.
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
foreach ($name in $secrets.Keys) {
    $secrets[$name] | & gh secret set $name @repoArguments
    if ($LASTEXITCODE -ne 0) { throw "Could not set $name." }
}

Write-Host "Set $($secrets.Count) secrets (key alias: $Alias)."
if (-not $GoogleServicesJson) {
    Write-Host 'GOOGLE_SERVICES_JSON_BASE64 was not set: pass -GoogleServicesJson for Crashlytics.'
}
