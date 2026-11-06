package com.gentics.mesh.contentrag.chunking;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.PublishStatus;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralChunkerTest {

    private static final DocumentGroupId GROUP =
            new DocumentGroupId("node-1", "main", "de");

    @Test
    void haeltMaximalgroesseEin() {
        StructuralChunker chunker = new StructuralChunker(new ChunkingOptions(200, 0));
        String text = ("Ein Absatz mit etwas Text. ").repeat(40);

        for (String part : chunker.split(text)) {
            assertTrue(part.length() <= 200, "Chunk zu lang: " + part.length());
        }
    }

    @Test
    void mischtFelderNicht() {
        StructuralChunker chunker = new StructuralChunker(ChunkingOptions.DEFAULTS);
        SourceContent content = content(Map.of("titel", "Kurzer Titeltext", "body", "Inhalt"));

        List<Chunk> chunks = chunker.chunk(content);

        assertEquals(2, chunks.size());
        assertNotEquals(chunks.get(0).fieldPath(), chunks.get(1).fieldPath());
    }

    @Test
    void haeltUeberschriftBeimFolgendenAbsatz() {
        StructuralChunker chunker = new StructuralChunker(new ChunkingOptions(400, 0));
        String text = "# Titel\n\nErster Absatz.\n\n## Unterkapitel\n\nZweiter Absatz.";

        List<String> parts = chunker.split(text);

        assertEquals(1, parts.size());
        assertTrue(parts.get(0).contains("# Titel"));
        assertTrue(parts.get(0).contains("## Unterkapitel"));
    }

    @Test
    void erzeugtUeberlappung() {
        StructuralChunker chunker = new StructuralChunker(new ChunkingOptions(120, 40));
        String text = ("Satz nummer eins mit genug Text. ").repeat(10);

        List<String> parts = chunker.split(text);

        assertTrue(parts.size() > 1, "Erwartet mehrere Chunks");
        assertFalse(parts.get(1).isBlank());
    }

    @Test
    void chunkIdsSindDeterministisch() {
        StructuralChunker chunker = new StructuralChunker(ChunkingOptions.DEFAULTS);
        SourceContent content = content(Map.of("body", "Ein stabiler Inhalt."));

        List<Chunk> first = chunker.chunk(content);
        List<Chunk> second = chunker.chunk(content);

        assertEquals(first.get(0).chunkId(), second.get(0).chunkId());
    }

    @Test
    void leererInhaltErgibtKeineChunks() {
        StructuralChunker chunker = new StructuralChunker(ChunkingOptions.DEFAULTS);

        assertTrue(chunker.chunk(content(Map.of("body", "   "))).isEmpty());
        assertTrue(chunker.chunk(null).isEmpty());
    }

    private static SourceContent content(Map<String, String> fields) {
        return new SourceContent(GROUP, "project-uuid", "Demo", "branch-uuid", "Article",
                "Titel", "/pfad", "https://example.org/pfad", fields, "1.0",
                PublishStatus.PUBLISHED, null, null, null, null,
                List.of("role-a"), List.of(), List.of());
    }
}
