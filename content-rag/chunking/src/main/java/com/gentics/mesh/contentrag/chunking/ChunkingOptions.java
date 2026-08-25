package com.gentics.mesh.contentrag.chunking;

/**
 * Parameter des strukturbasierten Chunkings.
 *
 * @param maxChars     Obergrenze je Chunk in Zeichen. Ein einzelner Absatz, der laenger ist,
 *                     wird an einer Wortgrenze geteilt.
 * @param overlapChars Ueberlappung zum vorhergehenden Chunk in Zeichen. Verhindert, dass eine
 *                     Aussage genau an der Schnittkante zerfaellt und dadurch in keinem Chunk
 *                     mehr vollstaendig auffindbar ist. 0 schaltet die Ueberlappung ab.
 */
public record ChunkingOptions(int maxChars, int overlapChars) {

    /** Vorgabewerte aus dem Konzept (Abschnitt 5.4). */
    public static final ChunkingOptions DEFAULTS = new ChunkingOptions(1200, 150);

    /** Prueft die Wertebereiche. */
    public ChunkingOptions {
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars muss positiv sein, war: " + maxChars);
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException(
                    "overlapChars muss zwischen 0 und maxChars liegen, war: " + overlapChars);
        }
    }
}
