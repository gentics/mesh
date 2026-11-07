package com.gentics.mesh.contentrag.meshsync;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ergebnis der Feldauswahl fuer eine Schemaversion: welche Schemafelder in den RAG-Index gehen,
 * in welcher Reihenfolge.
 *
 * <p>Die Reihenfolge ist Teil des Ergebnisses und nicht nur Kosmetik: der
 * {@code StructuralChunker} zaehlt {@code chunkIndex} feldweise hoch, und die Chunk-ID haengt an
 * {@code fieldPath + chunkIndex}. Eine andere Feldreihenfolge ergaebe andere IDs fuer denselben
 * Inhalt – und damit Dubletten im Index statt eines Upserts.
 *
 * <p>Eine leere Auswahl ist der Normalfall und kein Fehler: sie bedeutet „dieses Schema liefert
 * keinen RAG-Inhalt" und fuehrt im Handler zum {@code documentGroupQuery}-Delete. Genau darueber
 * wirkt auch die No-RAG-Konvention auf Node-Ebene.
 */
public final class RagSchemaSelection {

    private final String schema;
    private final String schemaVersionUuid;
    private final List<String> fields;
    private final Set<String> lookup;

    /**
     * @param schema            Schemaname
     * @param schemaVersionUuid Schemaversion, zu der diese Auswahl gehoert
     * @param fields            ausgewaehlte Schemafelder in Schemareihenfolge
     */
    public RagSchemaSelection(String schema, String schemaVersionUuid, List<String> fields) {
        this.schema = schema;
        this.schemaVersionUuid = schemaVersionUuid;
        this.fields = fields == null ? List.of() : List.copyOf(fields);
        this.lookup = Set.copyOf(new LinkedHashSet<>(this.fields));
    }

    /**
     * Leere Auswahl fuer ein ausgeschlossenes Schema.
     *
     * @param schema            Schemaname
     * @param schemaVersionUuid Schemaversion
     * @return Auswahl ohne Felder
     */
    public static RagSchemaSelection empty(String schema, String schemaVersionUuid) {
        return new RagSchemaSelection(schema, schemaVersionUuid, List.of());
    }

    /** @return Schemaname */
    public String schema() {
        return schema;
    }

    /** @return Schemaversion, zu der diese Auswahl gehoert */
    public String schemaVersionUuid() {
        return schemaVersionUuid;
    }

    /** @return ausgewaehlte Schemafelder in Schemareihenfolge */
    public List<String> fields() {
        return fields;
    }

    /** @return {@code true}, wenn kein Feld ausgewaehlt ist – der Node liefert keinen RAG-Inhalt */
    public boolean isEmpty() {
        return fields.isEmpty();
    }

    /**
     * Prueft einen Feldpfad gegen die Auswahl. Unterfelder von Micronodes und Listen erben die
     * Entscheidung ihres Schemafeldes (siehe {@link RagFieldPaths}).
     *
     * @param fieldPath Feldpfad, etwa {@code inhalt/2/text}
     * @return {@code true}, wenn der Pfad indexiert werden darf
     */
    public boolean includes(String fieldPath) {
        return lookup.contains(RagFieldPaths.rootOf(fieldPath));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RagSchemaSelection other)) {
            return false;
        }
        return Objects.equals(schema, other.schema)
                && Objects.equals(schemaVersionUuid, other.schemaVersionUuid)
                && fields.equals(other.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, schemaVersionUuid, fields);
    }

    @Override
    public String toString() {
        return "RagSchemaSelection[" + schema + "@" + schemaVersionUuid + " -> " + fields + "]";
    }
}
