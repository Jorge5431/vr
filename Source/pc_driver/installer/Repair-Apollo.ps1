param([string]$DeviceName = 'RMX3562')
$ErrorActionPreference = 'Stop'
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$identity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Abra este reparo como administrador para reiniciar o Apollo.'
}
$config = Join-Path $env:ProgramFiles 'Apollo\config'
$statePath = Join-Path $config 'sunshine_state.json'
if (-not (Test-Path -LiteralPath $statePath)) { throw 'Configuração Apollo não encontrada.' }
$service = Get-Service -Name ApolloService
$wasRunning = $service.Status -eq 'Running'
$changed = 0
try {
    if ($wasRunning) { Stop-Service -Name ApolloService; (Get-Service ApolloService).WaitForStatus('Stopped', '00:00:20') }
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $clients = @($state.root.named_devices | Where-Object { $_.name -eq $DeviceName -or $_.name -match ('^' + [regex]::Escape($DeviceName) + ' \(\d+\)$') })
    if ($clients.Count -eq 0) { throw 'Celular não cadastrado. Pareie o aparelho antes de executar o reparo.' }
    $backup = Join-Path $config ('stormvr-state-backup-' + [guid]::NewGuid().ToString('N') + '.json')
    Copy-Item -LiteralPath $statePath -Destination $backup
    foreach ($client in $clients) {
        # Add launch and game inputs only; no clipboard, file transfer or server command permissions.
        $client.perm = [int]$client.perm -bor 0x04001F00
        $changed++
    }
    $temp = Join-Path $config ('stormvr-state-' + [guid]::NewGuid().ToString('N') + '.tmp')
    $json = $state | ConvertTo-Json -Depth 100
    [IO.File]::WriteAllText($temp, $json, (New-Object Text.UTF8Encoding($false)))
    [IO.File]::Replace($temp, $statePath, $backup)
    Write-Output "Permissão de iniciar aplicativos e controles corrigida para $changed cadastro(s) do celular. Backup preservado na pasta de configuração Apollo."
} finally {
    if ($wasRunning) { Start-Service -Name ApolloService }
}
