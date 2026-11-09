package com.gentics.mesh.contentrag.meshsync;

import com.gentics.mesh.contentrag.chunking.ChunkingOptions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagSyncConfigTest {

    @Test
    void istOhneAngabeAusgeschaltet() {
        RagSyncOptions options = RagSyncConfig.syncOptions(key -> null);

        assertFalse(options.enabled());
        assertEquals(RagSelectionMode.NONE, options.defaultMode());
        assertFalse(options.includeBinaries());
    }

    @Test
    void liestBetriebsparameter() {
        Map<String, String> values = Map.of(
                "enabled", "true",
                "defaultMode", "all",
                "includeBinaries", "1");

        RagSyncOptions options = RagSyncConfig.syncOptions(values::get);

        assertTrue(options.enabled());
        assertEquals(RagSelectionMode.ALL, options.defaultMode());
        assertTrue(options.includeBinaries());
    }

    @Test
    void faelltBeiUnsinnAufVorgabenZurueck() {
        Map<String, String> values = Map.of(
                "enabled", "vielleicht",
                "defaultMode", "irgendwas",
                "chunk.maxChars", "keine Zahl");

        RagSyncOptions options = RagSyncConfig.syncOptions(values::get);
        ChunkingOptions chunking = RagSyncConfig.chunkingOptions(values::get);

        assertFalse(options.enabled());
        assertEquals(RagSelectionMode.NONE, options.defaultMode());
        assertEquals(ChunkingOptions.DEFAULTS, chunking);
    }

    @Test
    void liestChunkParameter() {
        ChunkingOptions chunking = RagSyncConfig.chunkingOptions(
                Map.of("chunk.maxChars", "800", "chunk.overlapChars", "100")::get);

        assertEquals(800, chunking.maxChars());
        assertEquals(100, chunking.overlapChars());
    }

    @Test
    void verwirftUnbrauchbareUeberlappung() {
        ChunkingOptions chunking = RagSyncConfig.chunkingOptions(
                Map.of("chunk.maxChars", "500", "chunk.overlapChars", "900")::get);

        assertEquals(500, chunking.maxChars());
        assertEquals(0, chunking.overlapChars());
    }

    @Test
    void liefertQuellenVorlageOderNull() {
        assertNull(RagSyncConfig.sourceUrlTemplate(key -> "   "));
        assertEquals("https://example.org{path}",
                RagSyncConfig.sourceUrlTemplate(Map.of("sourceUrlTemplate", " https://example.org{path} ")::get));
    }
}
