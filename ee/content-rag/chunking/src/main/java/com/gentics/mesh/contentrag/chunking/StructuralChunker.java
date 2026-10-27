package com.gentics.mesh.contentrag.chunking;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.contentrag.common.spi.Chunker;
import com.gentics.mesh.contentrag.common.util.ChunkIds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strukturbasiertes Chunking (Konzept 5.4).
 *
 * <p>Geschnitten wird entlang der vorhandenen Struktur statt nach reiner Zeichenzahl: zuerst an
 * Markdown-Ueberschriften, dann an Absatzgrenzen, und erst als letzte Stufe innerhalb eines
 * ueberlangen Absatzes an einer Wortgrenze. Feld-, Sprach- und Node-Grenzen werden nie
 * ueberschritten – ein Chunk mischt also nie Inhalte, die im CMS getrennt gepflegt werden.
 *
 * <p>Genau dieser Punkt ist der Grund, das Chunking selbst zu machen und nicht Elasticsearch
 * ueber {@code semantic_text} chunken zu lassen: dessen wort- bzw. satzbasierte Aufteilung kennt
 * weder Ueberschriften noch Feldgrenzen.
 *
 * <p>Die Klasse ist zustandslos und framework-frei, damit sie im Mesh-Prozess neben dem
 * Standard-Sync laufen kann.
 */
public class StructuralChunker implements Chunker {

    private final ChunkingOptions options;

    /** Erzeugt einen Chunker mit den Vorgabewerten aus {@link ChunkingOptions#DEFAULTS}. */
    public StructuralChunker() {
        this(ChunkingOptions.DEFAULTS);
    }

    /**
     * @param options Chunking-Parameter
     */
    public StructuralChunker(ChunkingOptions options) {
        this.options = options == null ? ChunkingOptions.DEFAULTS : options;
    }

    @Override
    public List<Chunk> chunk(SourceContent content) {
        List<Chunk> chunks = new ArrayList<>();
        if (content == null) {
            return chunks;
        }
        DocumentGroupId group = content.groupId();
        int chunkIndex = 0;

        for (Map.Entry<String, String> field : content.markdownByField().entrySet()) {
            String fieldPath = field.getKey();
            for (String text : split(field.getValue())) {
                chunks.add(build(content, group, fieldPath, chunkIndex++, text));
            }
        }
        return chunks;
    }

    /**
     * Zerlegt einen Feldinhalt in Chunk-Texte.
     *
     * @param text normalisierter Markdown-Text eines Feldes
     * @return Chunk-Texte in Dokumentreihenfolge; leer bei leerem Eingabetext
     */
    List<String> split(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        for (String block : blocks(text)) {
            for (String part : hardSplit(block)) {
                if (current.length() > 0
                        && current.length() + part.length() + 2 > options.maxChars()) {
                    result.add(current.toString());
                    current = new StringBuilder(overlapOf(current.toString()));
                }
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(part);
            }
        }
        if (current.length() > 0) {
            String tail = current.toString();
            if (!tail.isBlank()) {
                result.add(tail);
            }
        }
        return result;
    }

    /**
     * Zerlegt in Struktur-Bloecke: eine Ueberschrift beginnt immer einen neuen Block und bleibt
     * mit dem folgenden Absatz zusammen, ansonsten trennen Leerzeilen.
     */
    private static List<String> blocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        for (String paragraph : text.split("\n\\s*\n")) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isHeading(trimmed)) {
                // Ueberschrift zurueckhalten, damit sie den Kontext des naechsten Absatzes traegt.
                flush(blocks, pending);
                pending.append(trimmed);
                continue;
            }
            if (pending.length() > 0) {
                pending.append("\n\n").append(trimmed);
                flush(blocks, pending);
            } else {
                blocks.add(trimmed);
            }
        }
        flush(blocks, pending);
        return blocks;
    }

    private static void flush(List<String> blocks, StringBuilder pending) {
        if (pending.length() > 0) {
            blocks.add(pending.toString());
            pending.setLength(0);
        }
    }

    private static boolean isHeading(String paragraph) {
        return paragraph.startsWith("#") && !paragraph.contains("\n");
    }

    /** Teilt einen Block, der fuer sich genommen zu lang ist, an Wortgrenzen. */
    private List<String> hardSplit(String block) {
        List<String> parts = new ArrayList<>();
        int max = options.maxChars();
        String rest = block;
        while (rest.length() > max) {
            int cut = rest.lastIndexOf(' ', max);
            if (cut <= 0) {
                cut = max;
            }
            parts.add(rest.substring(0, cut).strip());
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }

    /** Liefert das Ende des vorigen Chunks als Ueberlappung, auf eine Wortgrenze gerundet. */
    private String overlapOf(String previous) {
        int overlap = options.overlapChars();
        if (overlap <= 0 || previous.length() <= overlap) {
            return "";
        }
        String tail = previous.substring(previous.length() - overlap);
        int space = tail.indexOf(' ');
        return space < 0 ? "" : tail.substring(space + 1).strip();
    }

    private static Chunk build(SourceContent c, DocumentGroupId group, String fieldPath,
                               int index, String text) {
        return new Chunk(
                ChunkIds.chunkId(group, fieldPath, index),
                group.asKey(),
                group.nodeUuid(),
                c.projectUuid(),
                c.project(),
                c.branchUuid(),
                group.branch(),
                group.language(),
                c.schema(),
                c.path(),
                c.title(),
                text,
                index,
                fieldPath,
                c.meshVersion(),
                ChunkIds.contentHash(text),
                c.publishStatus(),
                c.validFrom(),
                c.validTo(),
                c.createdDate(),
                c.editedDate(),
                c.roleUuids(),
                c.sourceUrl(),
                c.referencedNodes(),
                c.editorialTags());
    }
}
