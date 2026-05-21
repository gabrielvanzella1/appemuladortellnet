# Servidor Telnet simples que envia ANSI colors
$port = 2323
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Any, $port)
$listener.Start()
Write-Host "Servidor Telnet rodando em 127.0.0.1:$port com ANSI colors!" -ForegroundColor Green
Write-Host "Conecte com: telnet 127.0.0.1 2323" -ForegroundColor Cyan

while ($true) {
    $client = $listener.AcceptTcpClient()
    $stream = $client.GetStream()
    $writer = [System.IO.StreamWriter]::new($stream)
    $writer.AutoFlush = $true

    Write-Host "Cliente conectado!" -ForegroundColor Yellow

    # Enviar ANSI codes
    $writer.WriteLine([char]27 + "[31mVermelho (Red)[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[32mVerde (Green)[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[33mAmarelo (Yellow)[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[34mAzul (Blue)[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[35mMagenta[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[36mCyan[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[1;31mVermelho Bold[" + [char]27 + "[0m")
    $writer.WriteLine([char]27 + "[1;32mVerde Bold[" + [char]27 + "[0m")
    $writer.WriteLine()
    $writer.WriteLine([char]27 + "[1;33mTítulo com Cores" + [char]27 + "[0m")
    $writer.WriteLine("Teste concluído!")
    $writer.WriteLine("Digite 'sair' para desconectar")
    
    # Ler input do cliente
    $reader = [System.IO.StreamReader]::new($stream)
    while ($true) {
        if ($stream.DataAvailable) {
            $line = $reader.ReadLine()
            if ($line -eq "sair" -or $null -eq $line) { break }
            $writer.WriteLine("Echo: $line")
        }
        Start-Sleep -Milliseconds 100
    }

    $writer.Close()
    $reader.Close()
    $client.Close()
    Write-Host "Cliente desconectado!" -ForegroundColor Yellow
}
