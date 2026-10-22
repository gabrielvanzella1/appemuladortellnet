"""Host Telnet de TESTE que imita um WMS de coleta (estilo SHARK). Nao e o sistema real da Centauro.

Uso:  python scripts/shark_simulado.py [porta] [ip-de-escuta]
  - emulador Android no mesmo PC:   python scripts/shark_simulado.py            -> host 10.0.2.2  porta 2300
  - coletor real na mesma rede Wi-Fi: python scripts/shark_simulado.py 2300 0.0.0.0
        -> no coletor use o IP deste PC (ipconfig) e a porta 2300; libere a porta no firewall do Windows
Login: qualquer usuario/senha. Menu: 1=Conferencia 2=Inventario 9=Sair.
Codigo valido comeca com 789 (ex.: 7891000100103); qualquer outro = PRODUTO INVALIDO + BEL (bipe de erro).
"""
import socket, sys, threading
BIND = (sys.argv[2] if len(sys.argv) > 2 else '127.0.0.1', int(sys.argv[1]) if len(sys.argv) > 1 else 2300)
IAC, DONT, DO, WONT, WILL, SB, SE = 255, 254, 253, 252, 251, 250, 240
BEL, CLS = b'\x07', b'\x1b[2J\x1b[H'
REV, NORM, VERDE, VERM = b'\x1b[7m', b'\x1b[0m', b'\x1b[32m', b'\x1b[31m'

def ler_linha(c, eco=True):
    buf = b''
    while True:
        d = c.recv(1)
        if not d: raise ConnectionError
        b = d[0]
        if b == IAC:
            cmd = c.recv(1)[0]
            if cmd in (DO, DONT, WILL, WONT): c.recv(1)
            elif cmd == SB:
                while c.recv(1)[0] != SE: pass
            continue
        if b in (13, 10):
            if b == 13:
                c.settimeout(0.05)
                try: c.recv(1)
                except socket.timeout: pass
                c.settimeout(None)
            c.sendall(b'\r\n'); return buf.decode('latin-1').strip()
        if b in (8, 127):
            if buf: buf = buf[:-1]; c.sendall(b'\b \b')
            continue
        if b >= 32:
            buf += d; c.sendall(d if eco else b'*')

def tela(c, titulo, *linhas):
    c.sendall(CLS + REV + titulo.center(20).encode()[:20] + NORM + b'\r\n')
    for l in linhas: c.sendall(l.encode('latin-1') + b'\r\n')

def atender(c, end):
    print('conexao', end, flush=True)
    try:
        c.sendall(bytes([IAC, WILL, 1, IAC, WILL, 3]))
        tela(c, 'SHARK ACD (TESTE)', 'Automacao de', 'Coleta de Dados', '')
        c.sendall(b'Usuario: '); usuario = ler_linha(c)
        c.sendall(b'Senha: '); ler_linha(c, eco=False)
        while True:
            tela(c, 'MENU', f'Ola {usuario[:12]}', '', '1 - Conferencia', '2 - Inventario', '9 - Sair', '')
            c.sendall(b'Opcao: '); op = ler_linha(c)
            if op == '9': c.sendall(b'Ate logo!\r\n'); return
            if op not in ('1', '2'):
                c.sendall(VERM + b'OPCAO INVALIDA' + NORM + BEL + b'\r\n'); continue
            nome = 'CONFERENCIA' if op == '1' else 'INVENTARIO'; lidos = 0
            while True:
                tela(c, nome, f'Itens lidos: {lidos}', '', 'Bipe o produto', '(0 = voltar)', '')
                c.sendall(b'> '); cod = ler_linha(c)
                if cod == '0': break
                if cod.startswith('789') and cod.isdigit():
                    lidos += 1; c.sendall(VERDE + b'OK ' + cod[-6:].encode() + NORM + b'\r\n')
                else:
                    c.sendall(VERM + REV + b' PRODUTO INVALIDO! ' + NORM + BEL + b'\r\n')
                    c.sendall(b'Enter p/ continuar'); ler_linha(c)
    except (ConnectionError, OSError): pass
    finally: c.close(); print('desconectou', end, flush=True)

srv = socket.socket(); srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
srv.bind(BIND); srv.listen(5); print('host de teste em', BIND, flush=True)
while True:
    c, end = srv.accept(); threading.Thread(target=atender, args=(c, end), daemon=True).start()
