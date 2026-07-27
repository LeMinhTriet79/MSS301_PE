$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$answerRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$excludedDirectories = @("target", ".idea", ".git", ".gradle")
$excludedFiles = @(".mss301-pegen-project")

function New-CleanProjectArchive {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectPath,
        [Parameter(Mandatory = $true)][string]$DestinationZip
    )

    $resolvedProject = (Resolve-Path -LiteralPath $ProjectPath).Path
    $projectParent = Split-Path -Parent $resolvedProject
    if (Test-Path -LiteralPath $DestinationZip) {
        Remove-Item -LiteralPath $DestinationZip -Force
    }

    $archive = [System.IO.Compression.ZipFile]::Open(
        $DestinationZip,
        [System.IO.Compression.ZipArchiveMode]::Create
    )
    try {
        foreach ($file in Get-ChildItem -LiteralPath $resolvedProject -Recurse -Force -File) {
            $relativeInsideProject = $file.FullName.Substring($resolvedProject.Length).TrimStart('\', '/')
            $segments = $relativeInsideProject -split '[\\/]'
            if ($segments | Where-Object { $excludedDirectories -contains $_ }) {
                continue
            }
            if ($excludedFiles -contains $file.Name) {
                continue
            }
            $entryName = $file.FullName.Substring($projectParent.Length).TrimStart('\', '/').Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                $archive,
                $file.FullName,
                $entryName,
                [System.IO.Compression.CompressionLevel]::Optimal
            ) | Out-Null
        }
    }
    finally {
        $archive.Dispose()
    }
}

$sets = @(
    @{
        Folder = "01-department-employee"
        Total = "SE180211_DepartmentEmployee_Submission.zip"
        Projects = @("SE180211DepartmentService", "SE180211EmployeeService", "SE180211EmployeeGateway")
    },
    @{
        Folder = "02-restaurant-food"
        Total = "SE180211_RestaurantFood_Submission.zip"
        Projects = @("SE180211RestaurantService", "SE180211FoodService", "SE180211FoodyGateway")
    }
)

foreach ($set in $sets) {
    $setRoot = Join-Path $answerRoot $set.Folder
    $submission = Join-Path $setRoot "submission"
    New-Item -ItemType Directory -Force -Path $submission | Out-Null

    $projectZips = @()
    foreach ($project in $set.Projects) {
        $projectPath = Join-Path $setRoot $project
        $projectZip = Join-Path $submission ($project + ".zip")
        New-CleanProjectArchive -ProjectPath $projectPath -DestinationZip $projectZip
        $projectZips += $projectZip
        Write-Host "Created $projectZip"
    }

    $totalZip = Join-Path $submission $set.Total
    if (Test-Path -LiteralPath $totalZip) {
        Remove-Item -LiteralPath $totalZip -Force
    }
    Compress-Archive -LiteralPath $projectZips -DestinationPath $totalZip -CompressionLevel Optimal
    Write-Host "Created $totalZip"
}

Write-Host "PASS: clean per-project ZIP files and two combined submission ZIP files were created."
