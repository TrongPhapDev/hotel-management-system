$repo = "https://repo1.maven.org/maven2"
$libDir = "d:\HK2-2026\PTUD\Hotel_FH\Quan-ly-khach-san-ohno\lib"
if (-Not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
}

$deps = @(
    "org/apache/poi/poi/5.2.3/poi-5.2.3.jar",
    "org/apache/poi/poi-ooxml/5.2.3/poi-ooxml-5.2.3.jar",
    "org/apache/poi/poi-ooxml-lite/5.2.3/poi-ooxml-lite-5.2.3.jar",
    "org/apache/xmlbeans/xmlbeans/5.1.1/xmlbeans-5.1.1.jar",
    "org/apache/commons/commons-compress/1.21/commons-compress-1.21.jar",
    "org/apache/commons/commons-collections4/4.4/commons-collections4-4.4.jar",
    "com/itextpdf/itextpdf/5.5.13.3/itextpdf-5.5.13.3.jar"
)

foreach ($dep in $deps) {
    $url = "$repo/$dep"
    $filename = Split-Path $dep -Leaf
    $dest = "$libDir\$filename"
    if (-Not (Test-Path $dest)) {
        Write-Host "Downloading $filename..."
        Invoke-WebRequest -Uri $url -OutFile $dest
    } else {
        Write-Host "$filename already exists."
    }
}
Write-Host "Download complete."
