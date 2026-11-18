param([switch]$Test, [switch]$Run, [switch]$Preview, [switch]$GuiTest)
$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $raizProyecto
$javacRuta = $null
if ($env:JAVA_HOME) {
    $candidatoJdk = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    if (Test-Path -LiteralPath $candidatoJdk) { $javacRuta = $candidatoJdk }
}
if (-not $javacRuta) {
    $comandoJavac = Get-Command javac -ErrorAction SilentlyContinue
    if ($comandoJavac) { $javacRuta = $comandoJavac.Source }
}
if (-not $javacRuta -and (Test-Path -LiteralPath 'tmp\jdk')) {
    $jdkLocal = Get-ChildItem -LiteralPath 'tmp\jdk' -Recurse -Filter javac.exe | Select-Object -First 1
    if ($jdkLocal) { $javacRuta = $jdkLocal.FullName }
}
if (-not $javacRuta) { throw 'Configura JAVA_HOME con un JDK 8 o posterior, o agrega javac al PATH.' }
$binJdk = Split-Path -Parent $javacRuta
$javaRuta = Join-Path $binJdk 'java.exe'
$jarRuta = Join-Path $binJdk 'jar.exe'
New-Item -ItemType Directory -Force -Path 'build\classes','build\test\classes','dist' | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& $javacRuta -encoding UTF-8 -source 8 -target 8 -d 'build\classes' $fuentes
if ($LASTEXITCODE -ne 0) { throw 'La compilacion del programa fallo.' }
& $jarRuta cfm 'dist\DUNAB.jar' 'manifest.mf' -C 'build\classes' .
if ($LASTEXITCODE -ne 0) { throw 'No fue posible generar el JAR.' }
Write-Output 'Compilacion correcta: dist\DUNAB.jar'
if ($Test -or $Preview -or $GuiTest) {
    $pruebas = @(Get-ChildItem -LiteralPath 'test' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    & $javacRuta -encoding UTF-8 -source 8 -target 8 -cp 'build\classes' -d 'build\test\classes' $pruebas
    if ($LASTEXITCODE -ne 0) { throw 'La compilacion de pruebas fallo.' }
}
if ($Test) {
    foreach ($clase in @('PruebasDunab','PruebasAdmin','PruebasPersistencia','PruebasSemestres','PruebasCalculadora','PruebasTema','PruebasNombres','PruebasParticipacion')) {
        & $javaRuta '-Djava.awt.headless=true' -cp 'build\classes;build\test\classes' "javaapplication1.$clase"
        if ($LASTEXITCODE -ne 0) { throw "Fallo la suite $clase." }
    }
}
if ($Preview) {
    & $javaRuta '-Djava.awt.headless=true' -cp 'build\classes;build\test\classes' 'javaapplication1.VistaPrevia'
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la vista previa.' }
}
if ($GuiTest) {
    & $javaRuta '-Djava.awt.headless=false' -cp 'build\classes;build\test\classes' 'javaapplication1.PruebasNavegacion'
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la prueba de navegacion de la ventana.' }
}
if ($Run) { & $javaRuta -cp 'build\classes' 'javaapplication1.MenuDUNAB' }
