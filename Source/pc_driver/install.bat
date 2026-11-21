@echo off
setlocal
REM Instala o driver no SteamVR. Passe o caminho do SteamVR como argumento para usar uma biblioteca personalizada.
set "STEAMVR=%~1"

if not defined STEAMVR (
  for /f "tokens=2,*" %%A in ('reg query "HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\Steam App 250820" /v InstallLocation 2^>nul ^| findstr /i "InstallLocation"') do set "STEAMVR=%%B"
)
if not defined STEAMVR (
  for /f "tokens=2,*" %%A in ('reg query "HKLM\SOFTWARE\Wow6432Node\Microsoft\Windows\CurrentVersion\Uninstall\Steam App 250820" /v InstallLocation 2^>nul ^| findstr /i "InstallLocation"') do set "STEAMVR=%%B"
)
if not defined STEAMVR (
  for /f "tokens=2,*" %%A in ('reg query "HKCU\Software\Valve\Steam" /v SteamPath 2^>nul ^| findstr /i "SteamPath"') do set "STEAMROOT=%%B"
  if defined STEAMROOT set "STEAMVR=%STEAMROOT%\steamapps\common\SteamVR"
)
if not exist "%STEAMVR%\drivers" set "STEAMVR=%ProgramFiles(x86)%\Steam\steamapps\common\SteamVR"
if not exist "%STEAMVR%\drivers" set "STEAMVR=%ProgramFiles%\Steam\steamapps\common\SteamVR"
if not exist "%STEAMVR%\drivers" goto :nosteam

if not exist "%~dp0package\stormvr\driver.vrdrivermanifest" goto :nobuild
if not exist "%~dp0package\stormvr\bin\win64\driver_stormvr.dll" goto :nobuild
if not exist "%~dp0package\stormvr\bin\win64\openvr_api.dll" goto :nobuild

echo Instalando StormVR em "%STEAMVR%\drivers\stormvr"...
xcopy /E /I /Y "%~dp0package\stormvr" "%STEAMVR%\drivers\stormvr" >nul
if errorlevel 1 goto :copyerror
xcopy /E /I /Y "%~dp0web" "%STEAMVR%\drivers\stormvr\web" >nul
xcopy /E /I /Y "%~dp0painel_python" "%STEAMVR%\drivers\stormvr\painel_python" >nul

if not exist "%STEAMVR%\drivers\stormvr\driver.vrdrivermanifest" goto :copyerror
if not exist "%STEAMVR%\drivers\stormvr\bin\win64\driver_stormvr.dll" goto :copyerror
echo.
echo Driver instalado. Feche e abra novamente o SteamVR para carregar os arquivos atualizados.
echo O rastreamento do app usa UDP 5556 (cabeca) e UDP 5557 (maos); permita essas portas no firewall do Windows.
echo Apollo/Moonlight e uma conexao separada: a imagem e o pareamento usam as portas do Apollo.
echo Se ainda faltar o headset, veja SteamVR\logs\vrserver.txt e procure por "stormvr".
echo Se existir a pasta antiga drivers\keyboardtouch, remova-a manualmente apenas se estiver gerando controles duplicados.
exit /b 0

:nosteam
echo Pasta do SteamVR nao encontrada automaticamente.
echo Use: install.bat "D:\SteamLibrary\steamapps\common\SteamVR"
exit /b 1

:nobuild
echo Arquivos do driver ausentes ou incompletos em package\stormvr.
echo Rode build.bat antes de instalar.
exit /b 1

:copyerror
echo A copia do driver nao foi concluida. Feche o SteamVR e tente novamente.
exit /b 1
