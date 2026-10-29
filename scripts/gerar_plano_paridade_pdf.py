#!/usr/bin/env python3
"""Renderiza o roadmap Markdown para PDF usando o Microsoft Edge instalado."""

from __future__ import annotations

import html
import re
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs" / "ROADMAP-PARIDADE-VELOCITY.md"
DEFAULT_OUTPUT = ROOT / "docs" / "ROADMAP-PARIDADE-VELOCITY.pdf"
EDGE_CANDIDATES = [
    Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
    Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
]


def inline_markup(value: str) -> str:
    value = html.escape(value, quote=False)
    value = re.sub(r"`([^`]+)`", r"<code>\1</code>", value)
    value = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", value)
    value = re.sub(r"\*(.+?)\*", r"<em>\1</em>", value)
    value = re.sub(
        r"\[([^\]]+)\]\((https?://[^)]+)\)",
        r'<a href="\2">\1</a>',
        value,
    )
    return value


def render_markdown(lines: list[str]) -> str:
    out: list[str] = []
    paragraph: list[str] = []
    list_kind: str | None = None
    in_code = False
    code_lines: list[str] = []

    def flush_paragraph() -> None:
        if paragraph:
            out.append("<p>" + inline_markup(" ".join(paragraph)) + "</p>")
            paragraph.clear()

    def close_list() -> None:
        nonlocal list_kind
        if list_kind:
            out.append(f"</{list_kind}>")
            list_kind = None

    index = 0
    while index < len(lines):
        line = lines[index]
        stripped = line.strip()

        if stripped.startswith("```"):
            flush_paragraph()
            close_list()
            if in_code:
                out.append("<pre><code>" + html.escape("\n".join(code_lines)) + "</code></pre>")
                code_lines.clear()
                in_code = False
            else:
                in_code = True
            index += 1
            continue
        if in_code:
            code_lines.append(line)
            index += 1
            continue

        if stripped.startswith("|"):
            flush_paragraph()
            close_list()
            rows: list[list[str]] = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                cells = [cell.strip() for cell in lines[index].strip().strip("|").split("|")]
                if not cells or all(re.fullmatch(r":?-{3,}:?", cell) for cell in cells):
                    index += 1
                    continue
                rows.append(cells)
                index += 1
            if rows:
                out.append("<table><thead><tr>" + "".join(f"<th>{inline_markup(c)}</th>" for c in rows[0]) + "</tr></thead><tbody>")
                for row in rows[1:]:
                    out.append("<tr>" + "".join(f"<td>{inline_markup(c)}</td>" for c in row) + "</tr>")
                out.append("</tbody></table>")
            continue

        heading = re.match(r"^(#{1,4})\s+(.+)$", stripped)
        if heading:
            flush_paragraph()
            close_list()
            level = min(len(heading.group(1)), 3)
            out.append(f"<h{level}>{inline_markup(heading.group(2))}</h{level}>")
            index += 1
            continue

        bullet = re.match(r"^\s*[-*]\s+(.+)$", line)
        numbered = re.match(r"^\s*\d+\.\s+(.+)$", line)
        if bullet or numbered:
            flush_paragraph()
            requested_kind = "ul" if bullet else "ol"
            if list_kind != requested_kind:
                close_list()
                list_kind = requested_kind
                out.append(f"<{list_kind}>")
            item = (bullet or numbered).group(1)
            item = re.sub(r"^\[ \]", "☐", item)
            item = re.sub(r"^\[x\]", "☑", item, flags=re.IGNORECASE)
            out.append(f"<li>{inline_markup(item)}</li>")
            index += 1
            continue

        if not stripped:
            flush_paragraph()
            close_list()
            index += 1
            continue

        paragraph.append(stripped)
        index += 1

    flush_paragraph()
    close_list()
    if in_code:
        out.append("<pre><code>" + html.escape("\n".join(code_lines)) + "</code></pre>")
    return "\n".join(out)


def main() -> int:
    output = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else DEFAULT_OUTPUT.resolve()
    edge = next((candidate for candidate in EDGE_CANDIDATES if candidate.exists()), None)
    if not edge:
        print("Microsoft Edge não encontrado; instale-o ou ajuste EDGE_CANDIDATES.", file=sys.stderr)
        return 2

    source_lines = SOURCE.read_text(encoding="utf-8").splitlines()
    body = render_markdown(source_lines[5:])
    document = f"""<!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8"><title>Plano ScanTE — Paridade Velocity</title>
<style>
@page {{ size: A4; margin: 17mm 16mm 18mm; }}
* {{ box-sizing: border-box; }}
body {{ font-family: 'Segoe UI', Arial, sans-serif; color: #1d2939; font-size: 10pt; line-height: 1.48; }}
.cover {{ min-height: 245mm; display: flex; flex-direction: column; justify-content: center; page-break-after: always; }}
.eyebrow {{ color: #127c70; font-size: 11pt; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; }}
.cover h1 {{ font-size: 32pt; line-height: 1.12; color: #102a43; margin: 18px 0 10px; }}
.cover h2 {{ color: #127c70; font-size: 18pt; font-weight: 500; margin: 0 0 28px; }}
.cover p {{ color: #52606d; max-width: 135mm; font-size: 12pt; }}
.cover .meta {{ border-top: 2px solid #127c70; margin-top: 28px; padding-top: 12px; font-size: 9pt; }}
h1 {{ color: #102a43; font-size: 21pt; margin: 0 0 16px; }}
h2 {{ color: #127c70; font-size: 16pt; border-bottom: 1px solid #cbd5df; padding-bottom: 4px; margin: 22px 0 9px; page-break-after: avoid; }}
h3 {{ color: #245b73; font-size: 12pt; margin: 17px 0 6px; page-break-after: avoid; }}
p {{ margin: 5px 0 9px; }}
ul, ol {{ margin: 5px 0 10px; padding-left: 22px; }}
li {{ margin: 3px 0; }}
table {{ width: 100%; border-collapse: collapse; margin: 9px 0 14px; font-size: 8.2pt; page-break-inside: auto; }}
thead {{ display: table-header-group; }}
tr {{ page-break-inside: avoid; }}
th {{ text-align: left; color: white; background: #245b73; padding: 6px; }}
td {{ border-bottom: 1px solid #d9e2ec; padding: 5px 6px; vertical-align: top; }}
tr:nth-child(even) td {{ background: #f2f6f8; }}
code {{ font-family: Consolas, monospace; color: #164e63; font-size: 0.92em; }}
pre {{ background: #f2f6f8; border-left: 3px solid #127c70; padding: 10px; white-space: pre-wrap; font: 8pt Consolas, monospace; page-break-inside: avoid; }}
a {{ color: #127c70; text-decoration: none; }}
strong {{ color: #102a43; }}
</style></head><body>
<section class="cover"><div class="eyebrow">ScanTE · Plano de produto</div>
<h1>Plano de implementação</h1><h2>Paridade funcional com Ivanti Velocity</h2>
<p>Escopo técnico e sequência de desenvolvimento para transformar os recursos existentes do ScanTE em uma plataforma de emulação e operação logística configurável, segura e pronta para frota.</p>
<div class="meta"><strong>Data:</strong> 22 de outubro de 2026<br><strong>Estado:</strong> plano inicial para desenvolvimento<br><strong>Base:</strong> código ScanTE e documentação pública da Ivanti</div></section>
<main>{body}</main></body></html>"""

    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="scante-roadmap-") as temp_dir:
        html_path = Path(temp_dir) / "roadmap.html"
        profile_path = Path(temp_dir) / "edge-profile"
        html_path.write_text(document, encoding="utf-8")
        command = [
            str(edge), "--headless", "--disable-gpu", "--disable-gpu-compositing",
            "--disable-features=VizDisplayCompositor", "--no-sandbox", "--no-first-run",
            "--no-default-browser-check", "--disable-extensions",
            f"--user-data-dir={profile_path}",
            f"--print-to-pdf={output}", "--print-to-pdf-no-header",
            html_path.as_uri(),
        ]
        result = subprocess.run(command, capture_output=True, text=True, timeout=90)
        if result.returncode != 0 or not output.exists():
            print(result.stderr or result.stdout or "Edge não gerou o PDF.", file=sys.stderr)
            return result.returncode or 1

    print(f"PDF gerado: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
