# Usage: .\verifyall.ps1 [forge|fabric|whatever to put before ":classes"]

param (
    [string]$prefix
)

# Clear the screen
Clear-Host

# Define an array to hold completed builds with color information
$completedBuilds = @()

# Get all version properties files
$versionFiles = Get-ChildItem -Path "./versionProperties/" -Filter "*.properties"

foreach ($versionFile in $versionFiles) {
    $version = [System.IO.Path]::GetFileNameWithoutExtension($versionFile.Name)

    # Run the gradle command
    $gradleCommand = ".\gradlew $($prefix)classes -PmcVer=$version"
    $process = Start-Process -FilePath "cmd.exe" -ArgumentList "/c $gradleCommand" -NoNewWindow -PassThru -Wait

    # Determine the result color
    if ($process.ExitCode -eq 0) {
        $color = "Green"
    } else {
        $color = "Red"
    }
    
    # Print the result with formatting
    $versionLength = $version.Length
    $topChars = ("^" * $versionLength)
    $bottomChars = ("=" * $versionLength)

    Write-Host "# $topChars" -ForegroundColor $color
    Write-Host "# $version" -ForegroundColor $color
    Write-Host "# $bottomChars" -ForegroundColor $color
    Write-Host

    # Add result to completed builds with color
    $completedBuilds += @{ Version = $version; Color = $color }
}

# Run clean and classes gradle tasks
Start-Process -FilePath "cmd.exe" -ArgumentList "/c .\gradlew clean" -NoNewWindow -Wait
Start-Process -FilePath "cmd.exe" -ArgumentList "/c .\gradlew classes" -NoNewWindow -Wait

# Print build results
Write-Host
Write-Host "Build results:"

foreach ($build in $completedBuilds) {
    Write-Host $build.Version -ForegroundColor $build.Color -NoNewline
    Write-Host " " -NoNewline  # Add a space between versions
}

Write-Host  # End the line after all versions are printed
