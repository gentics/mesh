package com.gentics.mesh.contentrag.meshsync;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.Set;

/**
 * Wandelt den HTML-Inhalt eines Mesh-Feldes in Markdown.
 *
 * <p>Der Node-Index von Mesh macht an dieser Stelle {@code Jsoup.parse(value).text()} – fuer die
 * Volltextsuche genuegt das. Fuer RAG nicht: der {@code StructuralChunker} schneidet entlang von
 * Ueberschriften, Absaetzen und Listen. Mit reinem Fliesstext haette er nichts, woran er sich
 * orientieren kann, und das strukturbasierte Chunking waere faktisch laengenbasiert.
 *
 * <p>Bewusst keine Fremdbibliothek fuer die Wandlung (flexmark-html2md-converter waere der
 * Kandidat): das Parsen uebernimmt jsoup, das im Mesh-Prozess ohnehin liegt, und die Ausgabe ist
 * eine knappe Teilmenge von Markdown – Ueberschriften, Absaetze, Listen, Hervorhebungen, Links,
 * Code, Zitate, Tabellen. Was ein Chunk fuer die Vektorisierung braucht, ist Struktur, nicht
 * vollstaendige Markdown-Treue.
 *
 * <p>Zustandslos und threadsicher.
 */
public final class HtmlToMarkdown {

    private static final Set<String> BLOCKS = Set.of(
            "address", "article", "aside", "blockquote", "div", "dl", "fieldset", "figure",
            "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "li", "main",
            "nav", "ol", "p", "pre", "section", "table", "tbody", "td", "tfoot", "th", "thead",
            "tr", "ul");

    private HtmlToMarkdown() {
    }

    /**
     * Wandelt HTML in Markdown.
     *
     * @param html HTML-Inhalt eines Feldes, darf {@code null} sein
     * @return Markdown ohne fuehrende und abschliessende Leerzeilen; {@code ""} bei leerer Eingabe
     */
    public static String convert(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        block(Jsoup.parseBodyFragment(html).body(), out, "");
        return tidy(out.toString());
    }

    /**
     * Normalisiert reinen Text, der ohne HTML gepflegt wird (String-Felder).
     *
     * @param text Feldinhalt, darf {@code null} sein
     * @return getrimmter Text mit hoechstens einer Leerzeile zwischen Absaetzen
     */
    public static String plain(String text) {
        return text == null ? "" : tidy(text);
    }

    private static void block(Element element, StringBuilder out, String indent) {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode text) {
                String value = collapse(text.getWholeText());
                if (!value.isBlank()) {
                    out.append(indent).append(value.strip()).append("\n\n");
                }
                continue;
            }
            if (!(node instanceof Element child)) {
                continue;
            }
            switch (child.tagName()) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    String hashes = "#".repeat(child.tagName().charAt(1) - '0');
                    append(out, indent, hashes + " " + inline(child));
                }
                case "ul", "ol" -> list(child, out, indent);
                case "pre" -> append(out, indent, "```\n" + child.wholeText().strip() + "\n```");
                case "blockquote" -> {
                    StringBuilder inner = new StringBuilder();
                    block(child, inner, "");
                    for (String line : tidy(inner.toString()).split("\n")) {
                        out.append(indent).append("> ").append(line).append('\n');
                    }
                    out.append('\n');
                }
                case "table" -> table(child, out, indent);
                case "hr" -> append(out, indent, "---");
                case "br" -> out.append('\n');
                default -> {
                    if (hasBlockChild(child)) {
                        block(child, out, indent);
                    } else {
                        append(out, indent, inline(child));
                    }
                }
            }
        }
    }

    private static void list(Element list, StringBuilder out, String indent) {
        boolean ordered = "ol".equals(list.tagName());
        int number = 1;
        for (Element item : list.children()) {
            if (!"li".equals(item.tagName())) {
                continue;
            }
            String marker = ordered ? (number++) + ". " : "- ";
            out.append(indent).append(marker).append(inline(item)).append('\n');
            for (Element nested : item.children()) {
                if ("ul".equals(nested.tagName()) || "ol".equals(nested.tagName())) {
                    list(nested, out, indent + "  ");
                }
            }
        }
        out.append('\n');
    }

    private static void table(Element table, StringBuilder out, String indent) {
        boolean firstRow = true;
        for (Element row : table.select("tr")) {
            StringBuilder cells = new StringBuilder("|");
            int count = 0;
            for (Element cell : row.children()) {
                if (!"td".equals(cell.tagName()) && !"th".equals(cell.tagName())) {
                    continue;
                }
                cells.append(' ').append(inline(cell).replace("|", "\\|")).append(" |");
                count++;
            }
            if (count == 0) {
                continue;
            }
            out.append(indent).append(cells).append('\n');
            if (firstRow) {
                out.append(indent).append("|").append(" --- |".repeat(count)).append('\n');
                firstRow = false;
            }
        }
        out.append('\n');
    }

    private static String inline(Element element) {
        StringBuilder out = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode text) {
                out.append(collapse(text.getWholeText()));
                continue;
            }
            if (!(node instanceof Element child)) {
                continue;
            }
            switch (child.tagName()) {
                case "strong", "b" -> wrap(out, "**", inline(child));
                case "em", "i" -> wrap(out, "*", inline(child));
                case "code" -> wrap(out, "`", inline(child));
                case "br" -> out.append('\n');
                case "a" -> {
                    String label = inline(child).strip();
                    String href = child.attr("href").strip();
                    if (label.isEmpty()) {
                        out.append(href);
                    } else if (href.isEmpty()) {
                        out.append(label);
                    } else {
                        out.append('[').append(label).append("](").append(href).append(')');
                    }
                }
                case "img" -> {
                    String alt = child.attr("alt").strip();
                    if (!alt.isEmpty()) {
                        out.append(alt);
                    }
                }
                case "ul", "ol" -> {
                    // Verschachtelte Liste in einem Listenpunkt: getrennt behandelt.
                }
                default -> out.append(inline(child));
            }
        }
        return out.toString().strip();
    }

    private static void wrap(StringBuilder out, String marker, String content) {
        if (content.isEmpty()) {
            return;
        }
        out.append(marker).append(content).append(marker);
    }

    private static void append(StringBuilder out, String indent, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        out.append(indent).append(text).append("\n\n");
    }

    private static boolean hasBlockChild(Element element) {
        for (Element child : element.children()) {
            if (BLOCKS.contains(child.tagName())) {
                return true;
            }
        }
        return false;
    }

    /** Fasst Whitespace innerhalb eines Textknotens zu einzelnen Leerzeichen zusammen. */
    private static String collapse(String text) {
        return text.replaceAll("\\s+", " ");
    }

    /** Trimmt und laesst hoechstens eine Leerzeile zwischen zwei Bloecken stehen. */
    private static String tidy(String text) {
        return text.replaceAll("[ \\t]+\n", "\n").replaceAll("\n{3,}", "\n\n").strip();
    }
}
