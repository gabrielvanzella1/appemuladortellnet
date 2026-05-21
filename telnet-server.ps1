# Servidor Telnet Simples em PowerShell
param([int]$Port = 23)

Write-Host "Iniciando Servidor Telnet na porta $Port..." -ForegroundColor Green
Write-Host "Conecte do emulator com: Host=127.0.0.1, Port=$Port" -ForegroundColor Cyan

$listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, $Port)
$listener.Start()

Write-Host "Aguardando conexoes..." -ForegroundColor Yellow

while ($true) {
    $client = $listener.AcceptTcpClient()
    $stream = $client.GetStream()
    $writer = New-Object System.IO.StreamWriter($stream)
    $reader = New-Object System.IO.StreamReader($stream)
    
    Write-Host "[CONECTADO] $(Get-Date)" -ForegroundColor Green
    
    # Enviar welcome message
    $writer.WriteLine("========================================")
    $writer.WriteLine("Bem-vindo ao Servidor Telnet Teste!")
    $writer.WriteLine("========================================")
    $writer.WriteLine("")
    $writer.WriteLine("Comandos disponiveis:")
    $writer.WriteLine("  HELP   - Mostra esta mensagem")
    $writer.WriteLine("  INFO   - Informacoes do sistema")
    $writer.WriteLine("  HORA   - Data/hora atual")
    $writer.WriteLine("  EXIT   - Sair")
    $writer.WriteLine("")
    $writer.Flush()
    
    $connected = $true
    while ($connected) {
        try {
            $writer.Write("telnet> ")
            $writer.Flush()
            
            $line = $reader.ReadLine()
            if ($null -eq $line) { break }
            
            $cmd = $line.Trim().ToUpper()
            
            switch ($cmd) {
                "HELP" {
                    $writer.WriteLine("Comandos disponiveis:")
                    $writer.WriteLine("  HELP - Mostra esta mensagem")
                    $writer.WriteLine("  INFO - Informacoes do sistema")
                    $writer.WriteLine("  HORA - Data/hora atual")
                    $writer.WriteLine("  EXIT - Sair")
                }
                "INFO" {
                    $writer.WriteLine("Hostname: $env:COMPUTERNAME")
                    $writer.WriteLine("Usuario: $env:USERNAME")
                    $writer.WriteLine("SO: Windows")
                }
                "HORA" {
                    $writer.WriteLine("Data/Hora: $(Get-Date)")
                }
                "EXIT" {
                    $writer.WriteLine("Desconectando...")
                    $connected = $false
                }
                "" {}
                default {
                    $writer.WriteLine("Comando desconhecido: $line")
                }
            }
            $writer.Flush()
        }
        catch {
            $connected = $false
        }
    }
    
    Write-Host "[DESCONECTADO] $(Get-Date)" -ForegroundColor Yellow
    $reader.Dispose()
    $writer.Dispose()
    $client.Close()
}
