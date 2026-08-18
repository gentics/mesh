package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.contentrag.common.spi.Chunker;
import com.gentics.mesh.contentrag.common.util.ChunkIds;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Einfaches strukturbasiertes Chunking (Konzept 5.4) als Erst-Implementierung:
 * teilt je Feld an Absatzgrenzen und respektiert die konfigurierte Maximalgroesse.
 * Harte Grenzen an Feld-/Sprach-/Node-Grenzen bleiben erhalten (kein felduebergreifendes Mischen).
 *
 * <p>AUSBAU: echtes semantisches Chunking entlang Markdown-Ueberschriften/Listen inkl. Overlap.
 */
@Component
public class SimpleStructuralChunker implements Chunker {

    private final RagProperties properties;

    /**
     * @param properties RAG-Konfiguration (Chunk-Maximalgroesse/Overlap)
     */
    public SimpleStructuralChunker(RagProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<Chunk> chunk(SourceContent content) {
        int maxChars = properties.getChunking().getMaxChars();
        List<Chunk> chunks = new ArrayList<>();
        DocumentGroupId group = content.groupId();
        int globalIndex = 0;

        for (Map.Entry<String, String> field : content.markdownByField().entrySet()) {
            String fieldPath = field.getKey();
            for (String part : splitToSize(field.getValue(), maxChars)) {
                if (part.isBlank()) {
                    continue;
                }
                chunks.add(buildChunk(content, group, fieldPath, globalIndex++, part.trim()));
            }
        }
        return chunks;
    }

    private static List<String> splitToSize(String text, int maxChars) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\n\\s*\n")) {
            if (current.length() + paragraph.length() > maxChars && current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private Chunk buildChunk(SourceContent c, DocumentGroupId group, String fieldPath,
                             int index, String text) {
        return new Chunk(
                ChunkIds.chunkId(group, fieldPath, index),
                group.asKey(),
                group.nodeUuid(),
                c.projectUuid(),
                c.project(),
                group.branch(),
                group.language(),
                c.schema(),
                c.path(),
                c.title(),
                text,
                index,
                fieldPath,
                null, // Embedding folgt im EmbeddingProvider-Schritt
                null,
                null,
                c.meshVersion(),
                ChunkIds.contentHash(text),
                c.publishStatus(),
                c.validFrom(),
                c.validTo(),
                c.createdDate(),
                c.editedDate(),
                c.readRoleUuids(),
                c.sourceUrl(),
                c.referencedNodes(),
                c.editorialTags());
    }
}
