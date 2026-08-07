$repo = "https://repo1.maven.org/maven2"
$libDir = "d:\HK2-2026\PTUD\Hotel_FH\Quan-ly-khach-san-ohno\lib"

$deps = @(
    "org/apache/logging/log4j/log4j-api/2.18.0/log4j-api-2.18.0.jar",
    "commons-io/commons-io/2.11.0/commons-io-2.11.0.jar"
)

foreach ($dep in $deps) {
    $url = "$repo/$dep"
    $filename = Split-Path $dep -Leaf
    $dest = "$libDir\$filename"
    if (-Not (Test-Path $dest)) {
        Write-Host "Downloading $filename..."
        Invoke-WebRequest -Uri $url -OutFile $dest
    }
}
Write-Host "Download complete."
