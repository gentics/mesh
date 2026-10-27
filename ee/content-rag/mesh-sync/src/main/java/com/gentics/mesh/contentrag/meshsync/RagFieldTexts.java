package com.gentics.mesh.contentrag.meshsync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sammelt die normalisierten Feldtexte eines Node-Containers und setzt dabei die Auswahl durch.
 *
 * <p>Der Sammler ist die eine Stelle, an der Text in den RAG-Pfad gelangt: was hier nicht
 * angenommen wird, wird nicht gechunkt und nicht geschrieben. Der Aufrufer – der Feld-Durchlauf
 * ueber den Node-Container – braucht die Auswahlregeln damit nicht zu kennen und kann sich auf
 * die Textgewinnung beschraenken.
 *
 * <p>Die Ausgabereihenfolge folgt der Schemareihenfolge aus {@link RagSchemaSelection}, innerhalb
 * eines Feldes der Reihenfolge der Aufrufe. Das ist keine Kosmetik: {@code chunkIndex} und damit
 * die Chunk-ID haengen an dieser Reihenfolge, und nur eine stabile Reihenfolge macht das
 * Bulk-Upsert idempotent.
 *
 * <p>Nicht threadsicher – eine Instanz je Node-Container.
 */
public final class RagFieldTexts {

    private final RagSchemaSelection selection;
    private final Map<String, Map<String, String>> byRootField = new LinkedHashMap<>();

    /**
     * @param selection Auswahl der Schemaversion des Containers
     */
    public RagFieldTexts(RagSchemaSelection selection) {
        this.selection = selection == null ? RagSchemaSelection.empty("", null) : selection;
    }

    /**
     * Nimmt einen Feldtext auf, sofern das zugehoerige Schemafeld ausgewaehlt ist.
     *
     * @param fieldPath Feldpfad, etwa {@code inhalt} oder {@code inhalt/2/text}
     * @param markdown  bereits nach Markdown gewandelter, normalisierter Text
     * @return {@code true}, wenn der Text aufgenommen wurde
     */
    public boolean add(String fieldPath, String markdown) {
        if (markdown == null || markdown.isBlank() || fieldPath == null || fieldPath.isEmpty()) {
            return false;
        }
        if (!selection.includes(fieldPath)) {
            return false;
        }
        byRootField.computeIfAbsent(RagFieldPaths.rootOf(fieldPath), key -> new LinkedHashMap<>())
                .merge(fieldPath, markdown, (existing, added) -> existing + "\n\n" + added);
        return true;
    }

    /**
     * Prueft, ob ein Feld ueberhaupt betrachtet werden muss. Erspart dem Aufrufer die
     * HTML-nach-Markdown-Wandlung fuer Felder, die ohnehin draussen bleiben.
     *
     * @param fieldPath Feldpfad
     * @return {@code true}, wenn ein Text zu diesem Pfad angenommen wuerde
     */
    public boolean accepts(String fieldPath) {
        return selection.includes(fieldPath);
    }

    /** @return {@code true}, wenn kein Text aufgenommen wurde */
    public boolean isEmpty() {
        return byRootField.isEmpty();
    }

    /**
     * Ergebnis fuer {@code SourceContent#markdownByField}.
     *
     * @return Feldtexte in Schemareihenfolge; unveraenderliche Sicht mit stabiler Iteration
     */
    public Map<String, String> toMarkdownByField() {
        Map<String, String> result = new LinkedHashMap<>();
        List<String> order = new ArrayList<>(selection.fields());
        for (String rootField : order) {
            Map<String, String> texts = byRootField.get(rootField);
            if (texts != null) {
                result.putAll(texts);
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
