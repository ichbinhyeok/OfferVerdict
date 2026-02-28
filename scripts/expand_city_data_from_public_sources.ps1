param(
    [int]$AddCount = 40,
    [int]$MinPopulation = 150000,
    [string]$CityCostPath = "src/main/resources/data/CityCost.json",
    [string]$EvidenceCsvPath = "docs/city-data/city-expansion-sources.csv",
    [switch]$ForceRefresh
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $CityCostPath)) {
    throw "City data file not found: $CityCostPath"
}

function Get-LatestDateColumnName {
    param([string[]]$ColumnNames)
    for ($i = $ColumnNames.Count - 1; $i -ge 0; $i--) {
        if ($ColumnNames[$i] -match "^\d{4}-\d{2}-\d{2}$") {
            return $ColumnNames[$i]
        }
    }
    return $null
}

function Get-LatestMonthlyValue {
    param($Row)
    $props = $Row.PSObject.Properties.Name
    for ($i = $props.Count - 1; $i -ge 0; $i--) {
        $name = $props[$i]
        if ($name -match "^\d{4}-\d{2}-\d{2}$") {
            $v = $Row.$name
            if ($null -ne $v -and "$v".Trim() -ne "") {
                try { return [double]$v } catch { }
            }
        }
    }
    return $null
}

function Get-OrDownloadFile {
    param(
        [string]$Uri,
        [string]$OutFile
    )
    if ($ForceRefresh -or -not (Test-Path $OutFile)) {
        Invoke-WebRequest -Uri $Uri -OutFile $OutFile -TimeoutSec 180
    }
}

function New-CitySlug {
    param(
        [string]$City,
        [string]$StateAbbr
    )
    $normalized = $City.ToLowerInvariant()
    $normalized = $normalized -replace "[\.']", ""
    $normalized = $normalized -replace "&", " and "
    $normalized = $normalized -replace "[^a-z0-9]+", "-"
    $normalized = $normalized.Trim("-")
    if ([string]::IsNullOrWhiteSpace($normalized)) {
        throw "Could not create slug from city: $City"
    }
    return "$normalized-$($StateAbbr.ToLowerInvariant())"
}

function Round-ToNearest10 {
    param([double]$Value)
    return [int]([math]::Round($Value / 10.0) * 10)
}

function Clamp {
    param(
        [double]$Value,
        [double]$Min,
        [double]$Max
    )
    return [math]::Min($Max, [math]::Max($Min, $Value))
}

function Get-ColIndex {
    param(
        [double]$Rent,
        [double]$HomeValue
    )
    $rentAdj = ($Rent - 1400.0) / 40.0
    $homeAdj = (($HomeValue - 325000.0) / 275000.0) * 4.5
    $raw = 95.0 + $rentAdj + $homeAdj
    return [int][math]::Round((Clamp -Value $raw -Min 88.0 -Max 185.0))
}

function Get-Tier {
    param([double]$Population)
    if ($Population -ge 900000) { return 1 }
    if ($Population -ge 350000) { return 2 }
    return 3
}

function Parse-NumberOrNull {
    param($Value)
    if ($null -eq $Value) { return $null }
    $text = "$Value".Trim()
    if ($text -eq "" -or $text -eq "-666666666") { return $null }
    try {
        return [double]$text
    } catch {
        return $null
    }
}

$nameToAbbr = @{
    "Alabama" = "AL"; "Alaska" = "AK"; "Arizona" = "AZ"; "Arkansas" = "AR"; "California" = "CA";
    "Colorado" = "CO"; "Connecticut" = "CT"; "Delaware" = "DE"; "District of Columbia" = "DC";
    "Florida" = "FL"; "Georgia" = "GA"; "Hawaii" = "HI"; "Idaho" = "ID"; "Illinois" = "IL";
    "Indiana" = "IN"; "Iowa" = "IA"; "Kansas" = "KS"; "Kentucky" = "KY"; "Louisiana" = "LA";
    "Maine" = "ME"; "Maryland" = "MD"; "Massachusetts" = "MA"; "Michigan" = "MI"; "Minnesota" = "MN";
    "Mississippi" = "MS"; "Missouri" = "MO"; "Montana" = "MT"; "Nebraska" = "NE"; "Nevada" = "NV";
    "New Hampshire" = "NH"; "New Jersey" = "NJ"; "New Mexico" = "NM"; "New York" = "NY";
    "North Carolina" = "NC"; "North Dakota" = "ND"; "Ohio" = "OH"; "Oklahoma" = "OK"; "Oregon" = "OR";
    "Pennsylvania" = "PA"; "Rhode Island" = "RI"; "South Carolina" = "SC"; "South Dakota" = "SD";
    "Tennessee" = "TN"; "Texas" = "TX"; "Utah" = "UT"; "Vermont" = "VT"; "Virginia" = "VA";
    "Washington" = "WA"; "West Virginia" = "WV"; "Wisconsin" = "WI"; "Wyoming" = "WY"
}

$tmpDir = "data/tmp-city-expansion"
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null

$zoriPath = Join-Path $tmpDir "City_zori_uc_sfrcondomfr_sm_month.csv"
$zhviPath = Join-Path $tmpDir "City_zhvi_uc_sfrcondo_tier_0.33_0.67_sm_sa_month.csv"

Get-OrDownloadFile -Uri "https://files.zillowstatic.com/research/public_csvs/zori/City_zori_uc_sfrcondomfr_sm_month.csv" -OutFile $zoriPath
Get-OrDownloadFile -Uri "https://files.zillowstatic.com/research/public_csvs/zhvi/City_zhvi_uc_sfrcondo_tier_0.33_0.67_sm_sa_month.csv" -OutFile $zhviPath

$zori = Import-Csv $zoriPath
$zhvi = Import-Csv $zhviPath

$zoriLatestMonth = Get-LatestDateColumnName -ColumnNames $zori[0].PSObject.Properties.Name
$zhviLatestMonth = Get-LatestDateColumnName -ColumnNames $zhvi[0].PSObject.Properties.Name

$zoriMap = [System.Collections.Generic.Dictionary[string, double]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($r in $zori) {
    if ($r.RegionType -ne "city") { continue }
    $key = ("{0}|{1}" -f $r.RegionName.Trim().ToLowerInvariant(), $r.State.Trim().ToUpperInvariant())
    if (-not $zoriMap.ContainsKey($key)) {
        $latest = Get-LatestMonthlyValue -Row $r
        if ($null -ne $latest) {
            $zoriMap[$key] = [double]$latest
        }
    }
}

$zhviMap = [System.Collections.Generic.Dictionary[string, double]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($r in $zhvi) {
    if ($r.RegionType -ne "city") { continue }
    $key = ("{0}|{1}" -f $r.RegionName.Trim().ToLowerInvariant(), $r.State.Trim().ToUpperInvariant())
    if (-not $zhviMap.ContainsKey($key)) {
        $latest = Get-LatestMonthlyValue -Row $r
        if ($null -ne $latest) {
            $zhviMap[$key] = [double]$latest
        }
    }
}

$existingData = Get-Content -Raw -Encoding UTF8 $CityCostPath | ConvertFrom-Json
$existingCities = @($existingData.cities)
$existingSlugs = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($c in $existingCities) {
    [void]$existingSlugs.Add($c.slug)
}

$censusUrl = "https://api.census.gov/data/2023/acs/acs1?get=NAME,B01003_001E,B19013_001E&for=place:*"
$census = (Invoke-WebRequest -Uri $censusUrl -UseBasicParsing -TimeoutSec 120).Content | ConvertFrom-Json

$candidates = @()
for ($i = 1; $i -lt $census.Count; $i++) {
    $row = $census[$i]
    $label = $row[0]
    if ($label -notmatch "^(.*) city, (.*)$") { continue }

    $cityName = $Matches[1].Trim()
    $stateName = $Matches[2].Trim()
    if (-not $nameToAbbr.ContainsKey($stateName)) { continue }

    $population = Parse-NumberOrNull -Value $row[1]
    $income = Parse-NumberOrNull -Value $row[2]
    if ($null -eq $population -or $null -eq $income) { continue }
    if ($population -lt $MinPopulation -or $income -le 0) { continue }

    $state = $nameToAbbr[$stateName]
    $lookupKey = ("{0}|{1}" -f $cityName.ToLowerInvariant(), $state)
    if (-not $zoriMap.ContainsKey($lookupKey)) { continue }
    if (-not $zhviMap.ContainsKey($lookupKey)) { continue }

    $slug = New-CitySlug -City $cityName -StateAbbr $state
    if ($existingSlugs.Contains($slug)) { continue }

    $rent = [double]$zoriMap[$lookupKey]
    $homeValue = [double]$zhviMap[$lookupKey]
    if ($rent -lt 900 -or $rent -gt 6500) { continue }
    if ($homeValue -lt 120000 -or $homeValue -gt 2500000) { continue }

    $colIndex = Get-ColIndex -Rent $rent -HomeValue $homeValue
    $tier = Get-Tier -Population $population

    $groceries = Round-ToNearest10 (Clamp -Value (320 + (($colIndex - 90) * 3.0)) -Min 260 -Max 780)
    $transport = Round-ToNearest10 (Clamp -Value (90 + (($colIndex - 90) * 1.4)) -Min 80 -Max 240)
    $utilities = Round-ToNearest10 (Clamp -Value (95 + (($colIndex - 90) * 1.1)) -Min 85 -Max 240)
    $misc = Round-ToNearest10 (Clamp -Value (160 + (($colIndex - 90) * 2.1)) -Min 130 -Max 400)

    $candidate = [ordered]@{
        city = $cityName
        state = $state
        slug = $slug
        priority = $tier
        tier = $tier
        avgRent = [int][math]::Round($rent)
        colIndex = [int]$colIndex
        medianIncome = [int][math]::Round($income)
        avgHousePrice = [int][math]::Round($homeValue)
        details = [ordered]@{
            groceries = [int]$groceries
            transport = [int]$transport
            utilities = [int]$utilities
            misc = [int]$misc
        }
        population = [int][math]::Round($population)
    }
    $candidates += [pscustomobject]$candidate
}

$selected = $candidates |
    Sort-Object @{ Expression = "population"; Descending = $true }, @{ Expression = "medianIncome"; Descending = $true }, @{ Expression = "city"; Descending = $false } |
    Select-Object -First $AddCount

if ($selected.Count -eq 0) {
    throw "No candidate cities selected. Check source joins or thresholds."
}

foreach ($item in $selected) {
    $cityObj = [ordered]@{
        city = $item.city
        state = $item.state
        slug = $item.slug
        priority = $item.priority
        tier = $item.tier
        avgRent = $item.avgRent
        colIndex = $item.colIndex
        medianIncome = $item.medianIncome
        avgHousePrice = $item.avgHousePrice
        details = [ordered]@{
            groceries = $item.details.groceries
            transport = $item.details.transport
            utilities = $item.details.utilities
            misc = $item.details.misc
        }
    }
    $existingCities += [pscustomobject]$cityObj
    [void]$existingSlugs.Add($item.slug)
}

$existingData.metadata.source = "Composite Data (legacy baseline + US Census ACS1 2023 + Zillow ZORI $zoriLatestMonth + Zillow ZHVI $zhviLatestMonth)"
$existingData.metadata.lastUpdated = (Get-Date).ToString("yyyy-MM-dd")
$existingData.metadata.version = "2.3"
$existingData.cities = $existingCities

$cityCostResolvedPath = (Resolve-Path $CityCostPath).Path
[System.IO.File]::WriteAllText(
    $cityCostResolvedPath,
    ($existingData | ConvertTo-Json -Depth 12),
    [System.Text.UTF8Encoding]::new($false)
)

$evidenceDir = Split-Path -Parent $EvidenceCsvPath
if ($evidenceDir -and -not (Test-Path $evidenceDir)) {
    New-Item -ItemType Directory -Path $evidenceDir -Force | Out-Null
}

$selected |
    Select-Object city, state, slug, population, avgRent, avgHousePrice, medianIncome, colIndex,
        @{ Name = "zori_latest_month"; Expression = { $zoriLatestMonth } },
        @{ Name = "zhvi_latest_month"; Expression = { $zhviLatestMonth } },
        @{ Name = "census_dataset"; Expression = { "acs/acs1 2023" } } |
    Export-Csv -Path $EvidenceCsvPath -NoTypeInformation -Encoding UTF8

Write-Output ("Added cities: {0}" -f $selected.Count)
Write-Output ("Total cities now: {0}" -f $existingCities.Count)
Write-Output ("Evidence CSV: {0}" -f $EvidenceCsvPath)
$selected | Select-Object city, state, slug, population, avgRent, medianIncome, avgHousePrice, colIndex | Format-Table -AutoSize
