package com.gentics.mesh.contentrag.meshsync;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feldauswahl (Allow-List je Schema) und Auswertung der No-RAG-Konvention (K4) fuer den
 * Mesh-seitigen RAG-Sync.
 *
 * <p><strong>Warum an dieser Stelle.</strong> Beide Fragen entscheiden dasselbe: welcher Inhalt
 * das CMS ueberhaupt Richtung RAG-Index verlaesst. Sie gehoeren deshalb vor die Normalisierung –
 * ausgeschlossener Inhalt wird gar nicht erst nach Markdown gewandelt, nicht gechunkt und nicht
 * geschrieben. Eine Filterung im rag-service waere die falsche Richtung: dorthin muesste der
 * Inhalt erst gelangen.
 *
 * <p><strong>Woher die Angaben kommen.</strong> Aus den bestehenden {@code elasticsearch}-Settings
 * von Schema und Feld ({@code FieldSchemaContainer#getElasticsearch()},
 * {@code FieldSchema#getElasticsearch()}) sowie aus dem vorhandenen {@code noIndex}-Opt-out. Das
 * ist freies JSON, versioniert mit der Schemaversion, ueber die bestehende Schema-API pflegbar –
 * es braucht also weder eine Modelaenderung noch eine Migration. Eine globale Einstellung in
 * {@code MeshOptions} haette die falsche Granularitaet: eine Allow-List ist pro Schema, nicht pro
 * Installation.
 *
 * <h2>Regeln</h2>
 * <p>Zuerst das Schema, dann das Feld; das erste zutreffende Ergebnis gilt.
 *
 * <ol>
 * <li>Schema {@code "elasticsearch": { "rag": false }} – das ganze Schema bleibt draussen. Harter
 *     Ausschluss: er gewinnt auch gegen eine Freigabe am einzelnen Feld. Ein Opt-out, das eine
 *     vergessene Feldangabe aushebeln koennte, waere kein Opt-out.</li>
 * <li>Feld {@code "elasticsearch": { "rag": false }} – <strong>No-RAG (K4)</strong> auf
 *     Feldebene.</li>
 * <li>Feld mit {@code noIndex} – implizites No-RAG: was nicht suchbar sein soll, ist auch nicht
 *     RAG-bar.</li>
 * <li>Feld {@code "elasticsearch": { "rag": true }} – ausdrueckliche Freigabe, auch wenn das
 *     Schema eine engere Allow-List fuehrt.</li>
 * <li>Schema {@code "elasticsearch": { "rag": { "fields": ["title", "content"] } }} – Allow-List
 *     je Schema; zusaetzlich moeglich: {@code "exclude": [...]} als Ausschlussliste und
 *     {@code "mode": "all" | "none"}.</li>
 * <li>Schema {@code "elasticsearch": { "rag": true }} – alle textfuehrenden Felder.</li>
 * <li>Ohne Angabe gilt {@link RagSyncOptions#defaultMode()}, vorgabegemaess
 *     {@link RagSelectionMode#NONE}.</li>
 * </ol>
 *
 * <p>Ueber alle Regeln hinweg gilt der Typfilter: mitgenommen werden {@code string}, {@code html},
 * {@code list} und {@code micronode}, dazu {@code binary}/{@code s3binary} nur bei
 * {@link RagSyncOptions#includeBinaries()} oder ausdruecklicher Feldfreigabe. {@code number},
 * {@code boolean}, {@code date} und {@code node} tragen keinen Fliesstext; ein Node-Feld waere
 * Referenzaufloesung (V7) und damit ein eigener Schritt.
 *
 * <p><strong>No-RAG auf Node-Ebene</strong> ergibt sich aus derselben Auswertung: liefert die
 * Auswahl kein Feld, entsteht kein {@code SourceContent}, und der Handler raeumt eine eventuell
 * vorhandene Document-Group per {@code documentGroupQuery} ab. Der Ausschluss wirkt damit
 * rueckwirkend, ohne zweiten Codepfad.
 *
 * <p>Die Auswahl wird je {@code schemaVersionUuid} zwischengespeichert – sie wird pro Event
 * gebraucht und kann sich nur mit der Schemaversion aendern.
 *
 * <p>Threadsicher.
 */
public final class RagFieldSelector {

    /** Schluessel der RAG-Angabe innerhalb der {@code elasticsearch}-Settings. */
    public static final String SETTING_KEY = "rag";

    private static final String KEY_FIELDS = "fields";
    private static final String KEY_EXCLUDE = "exclude";
    private static final String KEY_MODE = "mode";

    private static final Set<String> TEXT_TYPES = Set.of("string", "html", "list", "micronode");
    private static final Set<String> BINARY_TYPES = Set.of("binary", "s3binary");

    private final RagSyncOptions options;
    private final ConcurrentHashMap<String, RagSchemaSelection> cache = new ConcurrentHashMap<>();

    /**
     * @param options Betriebsparameter; {@code null} bedeutet {@link RagSyncOptions#DEFAULTS}
     */
    public RagFieldSelector(RagSyncOptions options) {
        this.options = options == null ? RagSyncOptions.DEFAULTS : options;
    }

    /**
     * Liefert die Auswahl fuer eine Schemaversion, gegebenenfalls aus dem Cache.
     *
     * @param schema Schemaversion
     * @return ausgewaehlte Felder in Schemareihenfolge; leer, wenn nichts indexiert werden darf
     */
    public RagSchemaSelection selectionFor(RagSchemaDescriptor schema) {
        if (schema == null) {
            return RagSchemaSelection.empty("", null);
        }
        String key = schema.versionUuid();
        if (key == null || key.isBlank()) {
            // Ohne Version kein Cache-Schluessel, der eine Aenderung ueberleben wuerde.
            return evaluate(schema, options);
        }
        return cache.computeIfAbsent(key, ignored -> evaluate(schema, options));
    }

    /**
     * Verwirft den Cache-Eintrag einer Schemaversion. Im Normalbetrieb nicht noetig, weil eine
     * geaenderte Schemaversion eine neue UUID hat; nuetzlich in Tests und nach einem Restore.
     *
     * @param schemaVersionUuid Schemaversion
     */
    public void invalidate(String schemaVersionUuid) {
        if (schemaVersionUuid != null) {
            cache.remove(schemaVersionUuid);
        }
    }

    /** Leert den Cache vollstaendig. */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Wertet die Regeln ohne Cache aus.
     *
     * @param schema  Schemaversion
     * @param options Betriebsparameter
     * @return Auswahl fuer diese Schemaversion
     */
    static RagSchemaSelection evaluate(RagSchemaDescriptor schema, RagSyncOptions options) {
        SchemaRule rule = SchemaRule.of(schema.settings());
        if (rule.excluded()) {
            return RagSchemaSelection.empty(schema.name(), schema.versionUuid());
        }
        RagSelectionMode mode = rule.mode() == null ? options.defaultMode() : rule.mode();
        List<String> selected = new ArrayList<>();

        for (RagFieldDescriptor field : schema.fields()) {
            Boolean fieldFlag = booleanSetting(field.settings());
            if (Boolean.FALSE.equals(fieldFlag) || field.noIndex()) {
                continue;                                   // Regel 2 und 3 – No-RAG
            }
            boolean explicit = Boolean.TRUE.equals(fieldFlag);
            if (!supportsType(field.type(), options, explicit)) {
                continue;
            }
            if (explicit) {
                selected.add(field.name());                 // Regel 4
                continue;
            }
            if (rule.exclude().contains(field.name())) {
                continue;
            }
            if (rule.allow() != null) {
                if (rule.allow().contains(field.name())) {  // Regel 5
                    selected.add(field.name());
                }
                continue;
            }
            if (mode == RagSelectionMode.ALL) {              // Regel 6 und 7
                selected.add(field.name());
            }
        }
        return new RagSchemaSelection(schema.name(), schema.versionUuid(), selected);
    }

    private static boolean supportsType(String type, RagSyncOptions options, boolean explicit) {
        if (TEXT_TYPES.contains(type)) {
            return true;
        }
        return BINARY_TYPES.contains(type) && (options.includeBinaries() || explicit);
    }

    /**
     * Liest {@code rag} als Boolean aus einem Settings-Block.
     *
     * @param settings {@code elasticsearch}-Settings
     * @return {@code TRUE}/{@code FALSE} bei einer Boolean-Angabe, sonst {@code null}
     */
    private static Boolean booleanSetting(Map<String, Object> settings) {
        return asBoolean(settings.get(SETTING_KEY));
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static Set<String> stringSet(Object value) {
        if (!(value instanceof Iterable<?> items)) {
            return null;
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object item : items) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }

    /**
     * Die RAG-Angabe eines Schemas.
     *
     * @param excluded harter Ausschluss des gesamten Schemas
     * @param allow    Allow-List oder {@code null}, wenn keine gesetzt ist
     * @param exclude  Ausschlussliste, nie {@code null}
     * @param mode     ausdruecklich gesetzter Modus oder {@code null}
     */
    private record SchemaRule(boolean excluded, Set<String> allow, Set<String> exclude, RagSelectionMode mode) {

        static SchemaRule of(Map<String, Object> settings) {
            Object value = settings.get(SETTING_KEY);
            Boolean flag = asBoolean(value);
            if (Boolean.FALSE.equals(flag)) {
                return new SchemaRule(true, null, Set.of(), null);
            }
            if (Boolean.TRUE.equals(flag)) {
                return new SchemaRule(false, null, Set.of(), RagSelectionMode.ALL);
            }
            if (!(value instanceof Map<?, ?> map)) {
                return new SchemaRule(false, null, Set.of(), null);
            }
            Set<String> allow = stringSet(map.get(KEY_FIELDS));
            Set<String> exclude = stringSet(map.get(KEY_EXCLUDE));
            RagSelectionMode mode = mode(map.get(KEY_MODE));
            if (mode == RagSelectionMode.NONE && allow == null) {
                return new SchemaRule(true, null, Set.of(), null);
            }
            if (mode == null && allow == null) {
                // Ein RAG-Objekt ohne Feldangabe ist eine Freigabe des Schemas.
                mode = RagSelectionMode.ALL;
            }
            return new SchemaRule(false, allow, exclude == null ? Set.of() : exclude, mode);
        }

        private static RagSelectionMode mode(Object value) {
            if (value == null) {
                return null;
            }
            String text = value.toString().toUpperCase(Locale.ROOT);
            if (RagSelectionMode.ALL.name().equals(text)) {
                return RagSelectionMode.ALL;
            }
            if (RagSelectionMode.NONE.name().equals(text)) {
                return RagSelectionMode.NONE;
            }
            return null;
        }
    }
}
