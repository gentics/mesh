package com.gentics.mesh.contentrag.meshsync;

/**
 * Feldpfade im Chunk-Dokument: {@code feld}, bei Listen und Micronodes
 * {@code feld/index/unterfeld}. Der erste Abschnitt ist immer das Feld des Schemas und
 * entscheidet ueber die Auswahl – ein Micronode-Unterfeld erbt die Entscheidung des aeusseren
 * Feldes, weil nur dieses im Schema steht und dort freigegeben werden kann.
 */
public final class RagFieldPaths {

    /** Trennzeichen zwischen den Abschnitten eines Feldpfades. */
    public static final char SEPARATOR = '/';

    private RagFieldPaths() {
    }

    /**
     * Liefert das Schemafeld eines Feldpfades.
     *
     * @param fieldPath Feldpfad, etwa {@code inhalt/2/text}
     * @return erster Abschnitt, hier {@code inhalt}; {@code ""} bei {@code null} oder leerem Pfad
     */
    public static String rootOf(String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) {
            return "";
        }
        int end = fieldPath.indexOf(SEPARATOR);
        return end < 0 ? fieldPath : fieldPath.substring(0, end);
    }

    /**
     * Haengt einen Abschnitt an einen Feldpfad an.
     *
     * @param parent  bisheriger Pfad, darf leer sein
     * @param segment anzuhaengender Abschnitt (Feldname oder Listenindex)
     * @return zusammengesetzter Pfad
     */
    public static String append(String parent, String segment) {
        if (parent == null || parent.isEmpty()) {
            return segment;
        }
        return parent + SEPARATOR + segment;
    }
}
