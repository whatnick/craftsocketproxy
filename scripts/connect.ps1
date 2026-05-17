$ErrorActionPreference = 'Stop'

$server = Read-Host "Minecraft server [minecraft.farlongmc.com]"
if ([string]::IsNullOrWhiteSpace($server)) { $server = 'minecraft.farlongmc.com' }

$defaultPort = 80
if ($server -match '^(?<host>.+):(?<port>\d+)$') {
	$serverHost = $Matches.host
	$serverPort = [int]$Matches.port
} else {
	$serverHost = $server
	$serverPort = $defaultPort
}

$localPort = if ($env:CRAFTSOCKETPROXY_LOCAL_PORT) { [int]$env:CRAFTSOCKETPROXY_LOCAL_PORT } else { 25565 }
$image = if ($env:CRAFTSOCKETPROXY_IMAGE) { $env:CRAFTSOCKETPROXY_IMAGE } else { 'ghcr.io/whatnick/craftsocketproxy:1.0.2-fabric' }
$fallbackImage = if ($env:CRAFTSOCKETPROXY_FALLBACK_IMAGE) { $env:CRAFTSOCKETPROXY_FALLBACK_IMAGE } else { 'craftsocketproxy-client:local' }
$containerName = if ($env:CRAFTSOCKETPROXY_CONTAINER) { $env:CRAFTSOCKETPROXY_CONTAINER } else { 'craftsocketproxy-client' }

$securePassword = Read-Host 'Server password' -AsSecureString
$passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)

if ([string]::IsNullOrWhiteSpace($password)) {
	throw 'Password is required.'
}

try {
	if (Get-Command docker -ErrorAction SilentlyContinue) {
		docker rm -f $containerName 2>$null | Out-Null
		docker run -d --rm `
			--name $containerName `
			-p "127.0.0.1:$localPort`:$localPort" `
			-e CRAFTSOCKETPROXY_PASSWORD=$password `
			$image `
			--c -host $serverHost -port $serverPort -proxy $localPort | Out-Null
		if ($LASTEXITCODE -ne 0) {
			Write-Host "Could not start $image. Building from the public fork instead..."
			docker build -t $fallbackImage 'https://github.com/whatnick/craftsocketproxy.git#master'
			if ($LASTEXITCODE -ne 0) { throw 'Docker build failed.' }
			docker run -d --rm `
				--name $containerName `
				-p "127.0.0.1:$localPort`:$localPort" `
				-e CRAFTSOCKETPROXY_PASSWORD=$password `
				$fallbackImage `
				--c -host $serverHost -port $serverPort -proxy $localPort | Out-Null
			if ($LASTEXITCODE -ne 0) { throw 'Could not start CraftSocketProxy.' }
		}
	} elseif ((Get-Command java -ErrorAction SilentlyContinue) -and $env:CRAFTSOCKETPROXY_JAR) {
		Start-Process java -ArgumentList @('-jar', $env:CRAFTSOCKETPROXY_JAR, '--c', '-host', $serverHost, '-port', $serverPort, '-proxy', $localPort, '-password', $password) -WindowStyle Minimized
	} else {
		throw 'Install Docker Desktop, or set CRAFTSOCKETPROXY_JAR to a local CraftSocketProxy jar and install Java.'
	}
} finally {
	Remove-Variable password -ErrorAction SilentlyContinue
	Remove-Variable securePassword -ErrorAction SilentlyContinue
}

Write-Host "Local proxy is starting on localhost:$localPort."
Write-Host "Open Minecraft Java Edition and connect to localhost:$localPort."
Start-Sleep -Seconds 3

if (Get-Command docker -ErrorAction SilentlyContinue) {
	docker logs --tail 20 $containerName 2>$null
}

$launcherCandidates = @(
	(Join-Path $env:ProgramFiles 'Minecraft Launcher\MinecraftLauncher.exe')
	(Join-Path ${env:ProgramFiles(x86)} 'Minecraft Launcher\MinecraftLauncher.exe')
	Join-Path $env:LOCALAPPDATA 'Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\game\MinecraftLauncher.exe'
) | Where-Object { $_ -and (Test-Path $_) }

if ($launcherCandidates.Count -gt 0) {
	Start-Process $launcherCandidates[0]
} else {
	Start-Process 'minecraft:' -ErrorAction SilentlyContinue
}
