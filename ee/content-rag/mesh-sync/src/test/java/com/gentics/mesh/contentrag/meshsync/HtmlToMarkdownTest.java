package com.gentics.mesh.contentrag.meshsync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlToMarkdownTest {

    @Test
    void wandeltUeberschriftenUndAbsaetze() {
        String markdown = HtmlToMarkdown.convert("<h1>Titel</h1><p>Erster Absatz.</p><h2>Kapitel</h2><p>Zweiter.</p>");

        assertEquals("# Titel\n\nErster Absatz.\n\n## Kapitel\n\nZweiter.", markdown);
    }

    @Test
    void erhaeltListenstruktur() {
        String markdown = HtmlToMarkdown.convert("<ul><li>eins</li><li>zwei</li></ul>");

        assertEquals("- eins\n- zwei", markdown);
    }

    @Test
    void nummeriertGeordneteListen() {
        String markdown = HtmlToMarkdown.convert("<ol><li>eins</li><li>zwei</li></ol>");

        assertEquals("1. eins\n2. zwei", markdown);
    }

    @Test
    void wandeltHervorhebungenUndLinks() {
        String markdown = HtmlToMarkdown.convert(
                "<p>Ein <strong>wichtiger</strong> <em>Hinweis</em> mit <a href=\"/impressum\">Link</a>.</p>");

        assertEquals("Ein **wichtiger** *Hinweis* mit [Link](/impressum).", markdown);
    }

    @Test
    void behaeltCodeBloecke() {
        String markdown = HtmlToMarkdown.convert("<pre>mvn clean verify</pre>");

        assertEquals("```\nmvn clean verify\n```", markdown);
    }

    @Test
    void wandeltTabellen() {
        String markdown = HtmlToMarkdown.convert(
                "<table><tr><th>Feld</th><th>Wert</th></tr><tr><td>a</td><td>b</td></tr></table>");

        assertEquals("| Feld | Wert |\n| --- | --- |\n| a | b |", markdown);
    }

    @Test
    void steigtInVerschachtelteBloeckeAb() {
        String markdown = HtmlToMarkdown.convert("<div><div><p>Tief</p></div><p>Flach</p></div>");

        assertEquals("Tief\n\nFlach", markdown);
    }

    @Test
    void fasstWhitespaceZusammen() {
        String markdown = HtmlToMarkdown.convert("<p>Ein\n   Text   mit\tPausen</p>");

        assertEquals("Ein Text mit Pausen", markdown);
    }

    @Test
    void liefertLeerenTextFuerLeereEingabe() {
        assertEquals("", HtmlToMarkdown.convert(null));
        assertEquals("", HtmlToMarkdown.convert("   "));
        assertEquals("", HtmlToMarkdown.convert("<p></p>"));
    }

    @Test
    void behandeltUnbekannteTagsAlsInline() {
        String markdown = HtmlToMarkdown.convert("<p>Text mit <span class=\"x\">Span</span> darin.</p>");

        assertTrue(markdown.contains("Text mit Span darin."), markdown);
    }

    @Test
    void plainNormalisiertNurWhitespace() {
        assertEquals("Zeile eins\n\nZeile zwei", HtmlToMarkdown.plain("  Zeile eins\n\n\n\nZeile zwei  "));
        assertEquals("", HtmlToMarkdown.plain(null));
    }
}
