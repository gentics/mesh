package com.gentics.mesh.contentrag.meshsync;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagFieldTextsTest {

    private static final RagSchemaSelection SELECTION =
            new RagSchemaSelection("artikel", "v1", List.of("titel", "bausteine", "inhalt"));

    @Test
    void nimmtNurAusgewaehlteFelder() {
        RagFieldTexts texts = new RagFieldTexts(SELECTION);

        assertTrue(texts.add("titel", "Ein Titel"));
        assertFalse(texts.add("intern", "Nicht freigegeben"));
        assertFalse(texts.accepts("intern"));

        assertEquals(Map.of("titel", "Ein Titel"), texts.toMarkdownByField());
    }

    @Test
    void nimmtUnterfelderAusgewaehlterFelder() {
        RagFieldTexts texts = new RagFieldTexts(SELECTION);

        assertTrue(texts.add("bausteine/0/text", "Erster Baustein"));
        assertTrue(texts.add("bausteine/1/text", "Zweiter Baustein"));

        assertEquals(List.of("bausteine/0/text", "bausteine/1/text"),
                List.copyOf(texts.toMarkdownByField().keySet()));
    }

    @Test
    void haeltDieSchemareihenfolgeEin() {
        RagFieldTexts texts = new RagFieldTexts(SELECTION);

        texts.add("inhalt", "Fliesstext");
        texts.add("bausteine/0/text", "Baustein");
        texts.add("titel", "Titel");

        assertEquals(List.of("titel", "bausteine/0/text", "inhalt"),
                List.copyOf(texts.toMarkdownByField().keySet()));
    }

    @Test
    void ignoriertLeereTexte() {
        RagFieldTexts texts = new RagFieldTexts(SELECTION);

        assertFalse(texts.add("titel", "   "));
        assertFalse(texts.add("titel", null));
        assertTrue(texts.isEmpty());
    }

    @Test
    void fasstMehrfacheTexteEinesPfadesZusammen() {
        RagFieldTexts texts = new RagFieldTexts(SELECTION);

        texts.add("titel", "Erster Teil");
        texts.add("titel", "Zweiter Teil");

        assertEquals("Erster Teil\n\nZweiter Teil", texts.toMarkdownByField().get("titel"));
    }
}
