package com.gentics.mesh.contentrag.index;

import java.util.Locale;
import java.util.Objects;

/**
 * Benennung der RAG-Chunk-Indizes.
 *
 * <p>Der Aufbau lehnt sich an Mesh' Node-Indizes an
 * ({@code node-<projectUuid>-<branchUuid>-<schemaVersionUuid>-<versionType>}), laesst aber
 * die <strong>schemaVersionUuid bewusst weg</strong>:
 *
 * <ul>
 *   <li>Mesh legt bei jeder Schema-Migration einen neuen Node-Index an und befuellt ihn neu.
 *       Traege der RAG-Index dieselbe Kennung, muesste bei jeder Migration der komplette
 *       Vektorbestand neu erzeugt werden – bei grossen Installationen eine mehrtaegige Last,
 *       ausgeloest im Takt der Migration.</li>
 *   <li>Ohne die Schemaversion im Namen bleibt der Index ueber Migrationen hinweg bestehen.
 *       Betroffene Document-Groups werden ueber den normalen Sync-Weg ersetzt; Chunks von
 *       Nodes, deren indexierte Felder die Migration nicht beruehrt, bleiben unveraendert
 *       erhalten und muessen nicht neu vektorisiert werden.</li>
 * </ul>
 *
 * <p>Das Schema bleibt als Filterfeld ({@link RagChunkFields#SCHEMA}) im Dokument erhalten.
 */
public final class RagIndexNaming {

    /** Fester Bestandteil aller RAG-Indexnamen, zur Abgrenzung von den Node-Indizes. */
    public static final String INFIX = "ragchunk";

    /** Version-Typ fuer veroeffentlichte Inhalte (MVP-Default). */
    public static final String PUBLISHED = "published";
    /** Version-Typ fuer Entwuerfe. */
    public static final String DRAFT = "draft";

    private RagIndexNaming() {
    }

    /**
     * Bildet den Indexnamen einer Projekt/Branch/Version-Kombination.
     *
     * @param prefix      Installations-Praefix, identisch zum Praefix der Mesh-Suche
     * @param projectUuid Mandant
     * @param branchUuid  Branch
     * @param versionType {@link #PUBLISHED} oder {@link #DRAFT}
     * @return kleingeschriebener, gueltiger Elasticsearch-Indexname
     */
    public static String indexName(String prefix, String projectUuid, String branchUuid,
                                   String versionType) {
        Objects.requireNonNull(projectUuid, "projectUuid");
        Objects.requireNonNull(branchUuid, "branchUuid");
        Objects.requireNonNull(versionType, "versionType");
        return lower(normalizePrefix(prefix) + INFIX + "-" + projectUuid + "-" + branchUuid
                + "-" + versionType);
    }

    /**
     * Wildcard ueber alle RAG-Chunk-Indizes der Installation.
     *
     * @param prefix Installations-Praefix
     * @return Wildcard-Ausdruck
     */
    public static String allIndices(String prefix) {
        return lower(normalizePrefix(prefix) + INFIX + "-*");
    }

    /**
     * Wildcard ueber alle RAG-Chunk-Indizes eines Projekts (alle Branches, alle Version-Typen).
     *
     * @param prefix      Installations-Praefix
     * @param projectUuid Mandant
     * @return Wildcard-Ausdruck
     */
    public static String projectIndices(String prefix, String projectUuid) {
        Objects.requireNonNull(projectUuid, "projectUuid");
        return lower(normalizePrefix(prefix) + INFIX + "-" + projectUuid + "-*");
    }

    /**
     * Wildcard ueber alle RAG-Chunk-Indizes eines Version-Typs.
     *
     * @param prefix      Installations-Praefix
     * @param versionType {@link #PUBLISHED} oder {@link #DRAFT}
     * @return Wildcard-Ausdruck
     */
    public static String allIndices(String prefix, String versionType) {
        Objects.requireNonNull(versionType, "versionType");
        return lower(normalizePrefix(prefix) + INFIX + "-*-" + versionType);
    }

    /**
     * Wildcard ueber die Indizes eines Projekts und Version-Typs (alle Branches).
     *
     * @param prefix      Installations-Praefix
     * @param projectUuid Mandant
     * @param versionType {@link #PUBLISHED} oder {@link #DRAFT}
     * @return Wildcard-Ausdruck
     */
    public static String projectIndices(String prefix, String projectUuid, String versionType) {
        Objects.requireNonNull(projectUuid, "projectUuid");
        Objects.requireNonNull(versionType, "versionType");
        return lower(normalizePrefix(prefix) + INFIX + "-" + projectUuid + "-*-" + versionType);
    }

    /**
     * Index-Pattern fuer das Composable Index Template, das Mapping und Pipeline traegt.
     *
     * @param prefix Installations-Praefix
     * @return Pattern, das alle RAG-Chunk-Indizes erfasst
     */
    public static String templatePattern(String prefix) {
        return allIndices(prefix);
    }

    /** Name des Index-Templates. */
    public static String templateName(String prefix) {
        return lower(normalizePrefix(prefix) + INFIX);
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        return prefix.endsWith("-") ? prefix : prefix + "-";
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
