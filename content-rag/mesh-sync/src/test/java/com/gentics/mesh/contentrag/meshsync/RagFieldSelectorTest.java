package com.gentics.mesh.contentrag.meshsync;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagFieldSelectorTest {

    private static final RagSyncOptions NONE_MODE =
            new RagSyncOptions(true, RagSelectionMode.NONE, false);
    private static final RagSyncOptions ALL_MODE =
            new RagSyncOptions(true, RagSelectionMode.ALL, false);

    @Test
    void ohneAngabeWirdNichtsIndexiert() {
        RagSchemaSelection selection = select(NONE_MODE, Map.of(),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("inhalt", "html"));

        assertTrue(selection.isEmpty());
    }

    @Test
    void allowListWaehltGenauDieGenanntenFelder() {
        RagSchemaSelection selection = select(NONE_MODE,
                rag(Map.of("fields", List.of("titel", "inhalt"))),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("inhalt", "html"),
                new RagFieldDescriptor("intern", "html"));

        assertEquals(List.of("titel", "inhalt"), selection.fields());
        assertFalse(selection.includes("intern"));
    }

    @Test
    void feldNoRagGewinntGegenAllowList() {
        RagSchemaSelection selection = select(NONE_MODE,
                rag(Map.of("fields", List.of("titel", "inhalt"))),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("inhalt", "html", false, rag(false)));

        assertEquals(List.of("titel"), selection.fields());
    }

    @Test
    void noIndexGiltAlsImplizitesNoRag() {
        RagSchemaSelection selection = select(ALL_MODE, Map.of(),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("technik", "string", true, Map.of()));

        assertEquals(List.of("titel"), selection.fields());
    }

    @Test
    void feldFreigabeSchlaegtEngeAllowList() {
        RagSchemaSelection selection = select(NONE_MODE,
                rag(Map.of("fields", List.of("titel"))),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("nachtrag", "html", false, rag(true)));

        assertEquals(List.of("titel", "nachtrag"), selection.fields());
    }

    @Test
    void schemaAusschlussGewinntGegenFeldFreigabe() {
        RagSchemaSelection selection = select(ALL_MODE, rag(false),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("nachtrag", "html", false, rag(true)));

        assertTrue(selection.isEmpty());
    }

    @Test
    void modeAllNimmtNurTextfuehrendeFelder() {
        RagSchemaSelection selection = select(ALL_MODE, Map.of(),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("inhalt", "html"),
                new RagFieldDescriptor("bausteine", "micronode"),
                new RagFieldDescriptor("stichworte", "list"),
                new RagFieldDescriptor("nummer", "number"),
                new RagFieldDescriptor("datum", "date"),
                new RagFieldDescriptor("verweis", "node"));

        assertEquals(List.of("titel", "inhalt", "bausteine", "stichworte"), selection.fields());
    }

    @Test
    void binariesNurMitOptionOderFeldFreigabe() {
        RagFieldDescriptor anhang = new RagFieldDescriptor("anhang", "binary");
        RagFieldDescriptor gutachten = new RagFieldDescriptor("gutachten", "binary", false, rag(true));

        assertTrue(select(ALL_MODE, Map.of(), anhang).isEmpty());
        assertEquals(List.of("gutachten"), select(ALL_MODE, Map.of(), gutachten).fields());
        assertEquals(List.of("anhang"),
                select(new RagSyncOptions(true, RagSelectionMode.ALL, true), Map.of(), anhang).fields());
    }

    @Test
    void ausschlusslisteNimmtEinzelneFelderHeraus() {
        RagSchemaSelection selection = select(NONE_MODE,
                rag(Map.of("mode", "all", "exclude", List.of("intern"))),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("intern", "html"));

        assertEquals(List.of("titel"), selection.fields());
    }

    @Test
    void schemaFreigabeGiltAuchOhneGlobalesAll() {
        RagSchemaSelection selection = select(NONE_MODE, rag(true),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("inhalt", "html"));

        assertEquals(List.of("titel", "inhalt"), selection.fields());
    }

    @Test
    void reihenfolgeFolgtDemSchemaNichtDerAllowList() {
        RagSchemaSelection selection = select(NONE_MODE,
                rag(Map.of("fields", List.of("inhalt", "titel"))),
                new RagFieldDescriptor("titel", "string"),
                new RagFieldDescriptor("inhalt", "html"));

        assertEquals(List.of("titel", "inhalt"), selection.fields());
    }

    @Test
    void unterfeldErbtDieEntscheidungDesSchemafeldes() {
        RagSchemaSelection selection = select(NONE_MODE,
                rag(Map.of("fields", List.of("bausteine"))),
                new RagFieldDescriptor("bausteine", "micronode"),
                new RagFieldDescriptor("intern", "micronode"));

        assertTrue(selection.includes("bausteine/0/text"));
        assertFalse(selection.includes("intern/0/text"));
    }

    @Test
    void cachetJeSchemaVersion() {
        RagFieldSelector selector = new RagFieldSelector(NONE_MODE);
        RagSchemaDescriptor erste = new RagSchemaDescriptor("artikel", "v1",
                rag(Map.of("fields", List.of("titel"))),
                List.of(new RagFieldDescriptor("titel", "string")));
        RagSchemaDescriptor geaendert = new RagSchemaDescriptor("artikel", "v1", Map.of(),
                List.of(new RagFieldDescriptor("titel", "string")));

        assertEquals(List.of("titel"), selector.selectionFor(erste).fields());
        assertEquals(List.of("titel"), selector.selectionFor(geaendert).fields(), "Cache greift");

        selector.invalidate("v1");
        assertTrue(selector.selectionFor(geaendert).isEmpty());
    }

    private static RagSchemaSelection select(RagSyncOptions options, Map<String, Object> settings,
                                             RagFieldDescriptor... fields) {
        RagSchemaDescriptor schema =
                new RagSchemaDescriptor("artikel", "v1", settings, List.of(fields));
        return new RagFieldSelector(options).selectionFor(schema);
    }

    private static Map<String, Object> rag(Object value) {
        return Map.of(RagFieldSelector.SETTING_KEY, value);
    }
}
