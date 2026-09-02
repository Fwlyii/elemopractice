#!/usr/bin/env python3
"""Convert the project's appendix-style DOCX SRS into the LaTeX body.

Only the main document story is converted. Headers and footers stay under the
control of the surrounding LaTeX template.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZipFile


W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"


def text_of(element: ET.Element) -> str:
    return "".join(node.text or "" for node in element.iter(f"{W}t")).strip()


def escape_latex(value: str) -> str:
    replacements = {
        "\\": r"\textbackslash{}",
        "&": r"\&",
        "%": r"\%",
        "$": r"\$",
        "#": r"\#",
        "_": r"\_",
        "{": r"\{",
        "}": r"\}",
        "~": r"\textasciitilde{}",
        "^": r"\textasciicircum{}",
        "≤": r"$\leq$",
        "≥": r"$\geq$",
        "→": r"$\rightarrow$",
    }
    return "".join(replacements.get(char, char) for char in value)


def paragraph_is_bold(paragraph: ET.Element) -> bool:
    runs = [run for run in paragraph.findall(f"{W}r") if text_of(run)]
    return bool(runs) and all(run.find(f"{W}rPr/{W}b") is not None for run in runs)


def paragraph_text(paragraph: ET.Element) -> str:
    value = escape_latex(text_of(paragraph))
    if value and paragraph_is_bold(paragraph):
        return rf"\textbf{{{value}}}"
    return value


def grid_span(cell: ET.Element) -> int:
    node = cell.find(f"{W}tcPr/{W}gridSpan")
    return int(node.get(f"{W}val", "1")) if node is not None else 1


def cell_text(cell: ET.Element, bold: bool = False) -> str:
    # Word's generated table markup applies bold to many complete data rows.
    # Keep semantic table headers bold, while rendering data rows uniformly.
    parts = [escape_latex(text_of(paragraph)) for paragraph in cell.findall(f"{W}p")]
    value = r"\par ".join(part for part in parts if part)
    if bold and value and not value.startswith(r"\textbf{"):
        value = rf"\textbf{{{value}}}"
    return value or "~"


def column_spec(columns: int, span: int = 1) -> str:
    # The small safety subtraction accounts for the outer rules in longtable.
    width = rf"\dimexpr {span}\linewidth/{columns}-2\tabcolsep-\arrayrulewidth-0.3pt\relax"
    return rf">{{\raggedright\arraybackslash}}p{{{width}}}"


def convert_table(table: ET.Element) -> list[str]:
    rows = table.findall(f"{W}tr")
    grid_columns = len(table.findall(f"{W}tblGrid/{W}gridCol"))
    if grid_columns == 1 and len(rows) == 1:
        placeholder = text_of(rows[0])
        if re.fullmatch(r"（此处插入图\s*\d+-\d+）", placeholder):
            return [
                r"\begin{center}",
                rf"\fbox{{\parbox[c][3.2cm][c]{{0.88\linewidth}}{{\centering\hei {escape_latex(placeholder)}}}}}",
                r"\end{center}",
            ]

    columns = max(grid_columns, 1)
    preamble = "|" + "|".join(column_spec(columns) for _ in range(columns)) + "|"
    output = [rf"\begin{{longtable}}{{{preamble}}}", r"\hline"]
    for row_index, row in enumerate(rows):
        cells = row.findall(f"{W}tc")
        rendered: list[str] = []
        for cell in cells:
            span = grid_span(cell)
            value = cell_text(cell, bold=row_index == 0)
            if span > 1:
                spec = "|" + column_spec(columns, span) + "|"
                value = rf"\multicolumn{{{span}}}{{{spec}}}{{{value}}}"
            rendered.append(value)
        output.append(" & ".join(rendered) + r" \\ \hline")
    output.append(r"\end{longtable}")
    return output


def heading(paragraph: str) -> tuple[int, str] | None:
    match = re.match(r"^(\d+(?:\.\d+){0,2})\.?\s+(.+)$", paragraph)
    if not match:
        return None
    level = match.group(1).count(".") + 1
    return level, match.group(2).strip()


def convert(docx_path: Path) -> str:
    with ZipFile(docx_path) as archive:
        document = ET.fromstring(archive.read("word/document.xml"))
    body = document.find(f"{W}body")
    if body is None:
        raise ValueError("DOCX does not contain a document body")

    output = [
        "% Generated from the appendix-style DOCX by docx_to_srs.py.",
        "% The DOCX contains figure placeholders but no embedded body images.",
        r"\renewcommand{\thesection}{\arabic{section}}",
        r"\renewcommand{\thesubsection}{\thesection.\arabic{subsection}}",
        r"\renewcommand{\thesubsubsection}{\thesubsection.\arabic{subsubsection}}",
        r"\setcounter{section}{0}",
        r"\setcounter{figure}{0}",
        r"\setcounter{table}{0}",
        "",
    ]
    in_bullets = False
    title_paragraphs = 0

    def close_bullets() -> None:
        nonlocal in_bullets
        if in_bullets:
            output.extend([r"\end{itemize}", ""])
            in_bullets = False

    for child in body:
        kind = child.tag.split("}")[-1]
        if kind == "sectPr":
            continue
        if kind == "tbl":
            close_bullets()
            output.extend(convert_table(child))
            output.append("")
            continue
        if kind != "p":
            continue

        raw = text_of(child)
        if not raw:
            close_bullets()
            continue

        if title_paragraphs == 0 and raw == "附录 1":
            close_bullets()
            output.extend([
                r"\begin{flushright}",
                r"{\hei\yihao\textbf{附录 1}}",
                r"\end{flushright}",
                "",
            ])
            title_paragraphs += 1
            continue
        if title_paragraphs == 1 and "需求规格说明书" in raw:
            output.extend([
                r"\begin{flushright}",
                rf"{{\hei\sanhao\textbf{{{escape_latex(raw)}}}}}",
                r"\end{flushright}",
                r"\vspace{0.8em}",
                "",
            ])
            title_paragraphs += 1
            continue

        found_heading = heading(raw)
        if found_heading:
            close_bullets()
            level, title = found_heading
            command = {1: "section", 2: "subsection", 3: "subsubsection"}[level]
            output.extend([rf"\{command}{{{escape_latex(title)}}}", ""])
            continue

        if raw.startswith("●"):
            if not in_bullets:
                output.append(r"\begin{itemize}")
                in_bullets = True
            output.append(rf"\item {escape_latex(raw[1:].strip())}")
            continue

        close_bullets()
        rendered = paragraph_text(child)
        output.extend([rendered, ""])

    close_bullets()
    return "\n".join(output).rstrip() + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("docx", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.write_text(convert(args.docx), encoding="utf-8")


if __name__ == "__main__":
    main()
