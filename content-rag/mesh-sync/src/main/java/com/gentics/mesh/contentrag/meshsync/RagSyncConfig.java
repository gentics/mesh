package com.gentics.mesh.contentrag.meshsync;

import com.gentics.mesh.contentrag.chunking.ChunkingOptions;

import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Liest die Betriebsparameter des RAG-Syncs aus Umgebung und System-Properties.
 *
 * <p>Bewusst nicht aus {@code MeshOptions}: der Konfigurationsbaum von Mesh liegt im OSS-Modell,
 * und ein {@code search.rag}-Abschnitt dort waere eine Modelaenderung samt Dokumentation und
 * Migration. Bis das entschieden ist, reichen Umgebungsvariablen – dieselbe Form, in der der
 * Helm-Chart die uebrigen Suchparameter setzt.
 *
 * <table>
 * <caption>Erkannte Schluessel</caption>
 * <tr><th>Umgebung</th><th>System-Property</th><th>Vorgabe</th></tr>
 * <tr><td>MESH_SEARCH_RAG_ENABLED</td><td>mesh.search.rag.enabled</td><td>false</td></tr>
 * <tr><td>MESH_SEARCH_RAG_DEFAULT_MODE</td><td>mesh.search.rag.defaultMode</td><td>NONE</td></tr>
 * <tr><td>MESH_SEARCH_RAG_INCLUDE_BINARIES</td><td>mesh.search.rag.includeBinaries</td><td>false</td></tr>
 * <tr><td>MESH_SEARCH_RAG_CHUNK_MAX_CHARS</td><td>mesh.search.rag.chunk.maxChars</td><td>1200</td></tr>
 * <tr><td>MESH_SEARCH_RAG_CHUNK_OVERLAP_CHARS</td><td>mesh.search.rag.chunk.overlapChars</td><td>150</td></tr>
 * <tr><td>MESH_SEARCH_RAG_SOURCE_URL_TEMPLATE</td><td>mesh.search.rag.sourceUrlTemplate</td><td>leer</td></tr>
 * </table>
 */
public final class RagSyncConfig {

    /** Praefix der Umgebungsvariablen. */
    public static final String ENV_PREFIX = "MESH_SEARCH_RAG_";

    /** Praefix der System-Properties. */
    public static final String PROPERTY_PREFIX = "mesh.search.rag.";

    private RagSyncConfig() {
    }

    /**
     * Betriebsparameter aus der Umgebung.
     *
     * @return Parameter mit den Vorgabewerten fuer alles, was nicht gesetzt ist
     */
    public static RagSyncOptions syncOptions() {
        return syncOptions(RagSyncConfig::lookup);
    }

    /**
     * Chunk-Parameter aus der Umgebung.
     *
     * @return Parameter mit den Vorgabewerten fuer alles, was nicht gesetzt ist
     */
    public static ChunkingOptions chunkingOptions() {
        return chunkingOptions(RagSyncConfig::lookup);
    }

    /**
     * Vorlage der Quellen-URL.
     *
     * @return Vorlage oder {@code null}, wenn keine gesetzt ist
     */
    public static String sourceUrlTemplate() {
        return sourceUrlTemplate(RagSyncConfig::lookup);
    }

    /**
     * Testbare Variante mit eigener Wertquelle.
     *
     * @param source Zuordnung Schluesselname (ohne Praefix, etwa {@code enabled}) auf Wert
     * @return Betriebsparameter
     */
    static RagSyncOptions syncOptions(UnaryOperator<String> source) {
        return new RagSyncOptions(
                bool(source.apply("enabled"), false),
                mode(source.apply("defaultMode")),
                bool(source.apply("includeBinaries"), false));
    }

    /**
     * Testbare Variante mit eigener Wertquelle.
     *
     * @param source Zuordnung Schluesselname auf Wert
     * @return Chunk-Parameter
     */
    static ChunkingOptions chunkingOptions(UnaryOperator<String> source) {
        int maxChars = number(source.apply("chunk.maxChars"), ChunkingOptions.DEFAULTS.maxChars());
        int overlap = number(source.apply("chunk.overlapChars"), ChunkingOptions.DEFAULTS.overlapChars());
        if (overlap >= maxChars) {
            overlap = 0;
        }
        return new ChunkingOptions(maxChars, overlap);
    }

    /**
     * Testbare Variante mit eigener Wertquelle.
     *
     * @param source Zuordnung Schluesselname auf Wert
     * @return Vorlage der Quellen-URL oder {@code null}
     */
    static String sourceUrlTemplate(UnaryOperator<String> source) {
        String value = source.apply("sourceUrlTemplate");
        return value == null || value.isBlank() ? null : value.strip();
    }

    /**
     * Sucht einen Wert erst als System-Property, dann als Umgebungsvariable.
     *
     * @param key Schluesselname ohne Praefix, etwa {@code chunk.maxChars}
     * @return Wert oder {@code null}
     */
    private static String lookup(String key) {
        String property = System.getProperty(PROPERTY_PREFIX + key);
        if (property != null) {
            return property;
        }
        return System.getenv(ENV_PREFIX + key.replace('.', '_')
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT));
    }

    private static boolean bool(String value, boolean fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(value.strip()) || "1".equals(value.strip());
    }

    private static int number(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static RagSelectionMode mode(String value) {
        if (value == null || value.isBlank()) {
            return RagSelectionMode.NONE;
        }
        return "ALL".equalsIgnoreCase(value.strip()) ? RagSelectionMode.ALL : RagSelectionMode.NONE;
    }
}
