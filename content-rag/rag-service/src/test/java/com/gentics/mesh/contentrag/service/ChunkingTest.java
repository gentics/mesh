package com.gentics.mesh.contentrag.service;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.PublishStatus;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.contentrag.common.util.ChunkIds;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import com.gentics.mesh.contentrag.service.scaffold.SimpleStructuralChunker;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkingTest {

    @Test
    void chunkIdsAreDeterministic() {
        DocumentGroupId group = new DocumentGroupId("node-1", "main", "de");
        String a = ChunkIds.chunkId(group, "body", 0);
        String b = ChunkIds.chunkId(group, "body", 0);
        String c = ChunkIds.chunkId(group, "body", 1);
        assertEquals(a, b, "Gleiche Eingabe muss gleiche ID liefern");
        assertFalse(a.equals(c), "Anderer chunkIndex muss andere ID liefern");
    }

    @Test
    void chunkerKeepsFieldBoundariesAndProducesChunks() {
        RagProperties props = new RagProperties();
        SimpleStructuralChunker chunker = new SimpleStructuralChunker(props);

        SourceContent content = new SourceContent(
                new DocumentGroupId("node-1", "main", "de"),
                "proj-1", "Demo", "article", "Titel", "/titel", "http://example/titel",
                Map.of(
                        "title", "Eine Ueberschrift",
                        "body", "Erster Absatz.\n\nZweiter Absatz."),
                "v1", PublishStatus.PUBLISHED, null, null, null, null,
                List.of("role-public"), List.of(), List.of());

        List<Chunk> chunks = chunker.chunk(content);
        assertFalse(chunks.isEmpty(), "Es muessen Chunks entstehen");
        assertTrue(chunks.stream().allMatch(ch -> ch.documentGroup().equals("node-1:main:de")));
        assertTrue(chunks.stream().noneMatch(Chunk::hasEmbedding),
                "Chunker setzt noch kein Embedding");
    }
}
