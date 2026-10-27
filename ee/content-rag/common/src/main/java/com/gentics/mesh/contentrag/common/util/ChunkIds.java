package com.gentics.mesh.contentrag.common.util;

import com.gentics.mesh.contentrag.common.model.DocumentGroupId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministische Chunk-IDs (Konzept K5):
 * {@code hash(nodeUuid + branch + language + fieldPath + chunkIndex)}.
 * Gleiche Struktur ergibt stabile IDs; bei Inhaltsaenderung greift Group-Delete-and-Replace.
 */
public final class ChunkIds {

    private ChunkIds() {
    }

    /**
     * Bildet die deterministische Chunk-ID aus Document-Group, Feldpfad und Chunk-Index.
     *
     * @param group      Document-Group (Node, Branch, Sprache)
     * @param fieldPath  Herkunftsfeld im Node
     * @param chunkIndex Position innerhalb der Document-Group
     * @return stabile, kollisionsarme Chunk-ID (SHA-256-Hex)
     */
    public static String chunkId(DocumentGroupId group, String fieldPath, int chunkIndex) {
        String raw = group.nodeUuid() + '|' + group.branch() + '|' + group.language()
                + '|' + fieldPath + '|' + chunkIndex;
        return sha256Hex(raw);
    }

    /** Stabiler Hash des normalisierten Inhalts fuer No-Op-Erkennung (K5/K6). */
    public static String contentHash(String normalizedContent) {
        return sha256Hex(normalizedContent == null ? "" : normalizedContent);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", e);
        }
    }
}
