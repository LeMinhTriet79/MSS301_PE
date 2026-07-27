param(
    [string]$MavenCommand = "mvn"
)

$ErrorActionPreference = "Stop"
$answerRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$projects = @(
    "01-department-employee/SE180211DepartmentService",
    "01-department-employee/SE180211EmployeeService",
    "01-department-employee/SE180211EmployeeGateway",
    "02-restaurant-food/SE180211RestaurantService",
    "02-restaurant-food/SE180211FoodService",
    "02-restaurant-food/SE180211FoodyGateway"
)

function Assert-Contains {
    param([string]$RelativePath, [string[]]$Tokens)
    $path = Join-Path $answerRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing required file: $path"
    }
    $content = Get-Content -LiteralPath $path -Raw
    foreach ($token in $Tokens) {
        if (-not $content.Contains($token)) {
            throw "Compliance check failed: '$token' is missing from $path"
        }
    }
}

function Assert-NotContains {
    param([string]$RelativePath, [string[]]$Tokens)
    $path = Join-Path $answerRoot $RelativePath
    $content = Get-Content -LiteralPath $path -Raw
    foreach ($token in $Tokens) {
        if ($content.Contains($token)) {
            throw "Compliance check failed: forbidden token '$token' appears in $path"
        }
    }
}

Assert-Contains "01-department-employee/SE180211DepartmentService/src/main/resources/application.properties" @(
    "server.port=8081",
    "jdbc:sqlserver://localhost:1433;databaseName=MSS301_2026_PE;encrypt=false;",
    "spring.jpa.hibernate.ddl-auto=none"
)
Assert-Contains "01-department-employee/SE180211EmployeeService/src/main/resources/application.properties" @(
    "server.port=8082",
    "department.service.url=http://localhost:8081"
)
Assert-Contains "01-department-employee/SE180211EmployeeGateway/src/main/resources/application.properties" @("server.port=8080")

Assert-Contains "02-restaurant-food/SE180211RestaurantService/src/main/resources/application.properties" @(
    "server.port=8081",
    "jdbc:sqlserver://localhost:1433;databaseName=MSS301_2026_PE;encrypt=false;"
)
Assert-Contains "02-restaurant-food/SE180211FoodService/src/main/resources/application.properties" @(
    "server.port=8082",
    "restaurant.service.url=http://localhost:8081"
)
Assert-Contains "02-restaurant-food/SE180211FoodyGateway/src/main/resources/application.properties" @("server.port=8080")

foreach ($properties in Get-ChildItem -LiteralPath $answerRoot -Recurse -Filter "application.properties") {
    $relative = $properties.FullName.Substring($answerRoot.Length).TrimStart('\', '/')
    Assert-NotContains $relative @("trustServerCertificate")
}

foreach ($project in $projects) {
    $pom = Join-Path (Join-Path $answerRoot $project) "pom.xml"
    if (-not (Test-Path -LiteralPath $pom -PathType Leaf)) {
        throw "Missing Maven project: $project"
    }
    Write-Host "Testing and compiling $project ..."
    & $MavenCommand -q test -f $pom
    if ($LASTEXITCODE -ne 0) {
        throw "Maven test/compile failed: $project"
    }
}

Write-Host "PASS: six projects compile, tests pass, and mandatory configuration checks passed."
