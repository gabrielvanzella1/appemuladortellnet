"""
Servidor telnet de demonstracao (NAO e o ERP da Centauro - ainda nao temos acesso).
Simula uma unica tela de WMS em VT100 puro (cursor, video reverso, BEL) pra servir
de prova de conceito da reformatacao de tela no ScanTE.

Uso: python scripts/demo_telnet_server.py [porta]
Padrao: porta 2323 (nao precisa de admin). No app, conectar em <IP da maquina>:2323.
"""
import socket
import sys

ESC = b"\x1b"
CLEAR = ESC + b"[2J" + ESC + b"[H"
REVERSE_ON = ESC + b"[7m"
REVERSE_OFF = ESC + b"[0m"
BEL = b"\x07"
F1_SEQ = ESC + b"OP"  # ESC O P - mesma sequencia que TerminalView.kt manda pro F1

FIELDS = [
    {"label": "CODIGO...: ", "row": 4, "col": 1, "width": 8},
    {"label": "QTDE.....: ", "row": 5, "col": 1, "width": 3},
    {"label": "LOCAL....: ", "row": 6, "col": 1, "width": 6},
]


def goto(row: int, col: int) -> bytes:
    return ESC + f"[{row};{col}H".encode()


def draw_screen() -> bytes:
    out = CLEAR
    out += goto(1, 1) + b"RECEBIMENTO DE MERCADORIA"
    out += goto(2, 1) + b"-" * 26
    for f in FIELDS:
        out += goto(f["row"], f["col"]) + f["label"].encode()
        out += REVERSE_ON + b" " * f["width"] + REVERSE_OFF
    out += goto(8, 1) + b"F1=Confirma  F2=Cancela"
    return out


def field_cursor(field: dict, pos: int) -> bytes:
    return goto(field["row"], field["col"] + len(field["label"]) + pos)


def handle_client(conn: socket.socket) -> None:
    conn.sendall(draw_screen())
    values = ["" for _ in FIELDS]
    cur = 0
    conn.sendall(field_cursor(FIELDS[0], 0))

    buf = b""
    while True:
        chunk = conn.recv(1)
        if not chunk:
            return
        buf += chunk

        # F1 chega como 3 bytes (ESC O P) - so processa quando o buffer bate ou nao bate mais
        if buf == F1_SEQ[: len(buf)] and len(buf) < len(F1_SEQ):
            continue
        if buf == F1_SEQ:
            if all(values) and cur == len(FIELDS):
                conn.sendall(CLEAR + goto(3, 1) + b"RECEBIDO COM SUCESSO")
                return
            conn.sendall(BEL)  # campo faltando -> alarme, igual ao host real (suposicao)
            buf = b""
            continue
        buf = b""

        if cur >= len(FIELDS):
            continue
        field = FIELDS[cur]
        ch = chunk

        if ch in (b"\r", b"\n"):
            cur += 1
            if cur < len(FIELDS):
                conn.sendall(field_cursor(FIELDS[cur], 0))
            continue

        if ch == b"\x7f" or ch == b"\x08":  # backspace
            if values[cur]:
                values[cur] = values[cur][:-1]
                conn.sendall(field_cursor(field, len(values[cur])) + b" " + field_cursor(field, len(values[cur])))
            continue

        if ch.isalnum() and len(values[cur]) < field["width"]:
            values[cur] += ch.decode()
            conn.sendall(REVERSE_ON + ch + REVERSE_OFF)


def main() -> None:
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 2323
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("0.0.0.0", port))
    srv.listen(1)
    print(f"Servidor demo (NAO e o ERP real da Centauro) ouvindo na porta {port}...")
    print("No ScanTE, conecte em <IP desta maquina>:" + str(port))
    while True:
        conn, addr = srv.accept()
        print(f"[conectado] {addr}")
        try:
            handle_client(conn)
        except (ConnectionResetError, BrokenPipeError):
            pass
        finally:
            conn.close()
            print(f"[desconectado] {addr}")


if __name__ == "__main__":
    main()
