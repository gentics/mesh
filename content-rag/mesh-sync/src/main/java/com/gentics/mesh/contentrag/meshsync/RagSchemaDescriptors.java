package com.gentics.mesh.contentrag.meshsync;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter von der Mesh-Schema-API auf die Deskriptoren der Feldauswahl.
 *
 * <p>Die einzige Klasse dieses Pakets, die Mesh-Typen kennt. Sie haelt {@link RagFieldSelector}
 * frei von der Schema-API und von Vert.x-JSON: dadurch bleiben die Auswahlregeln ohne laufendes
 * Mesh testbar.
 */
public final class RagSchemaDescriptors {

    private RagSchemaDescriptors() {
    }

    /**
     * Beschreibt eine Schema- oder Microschema-Version.
     *
     * @param name        Schemaname ({@code HibSchemaVersion#getName()})
     * @param versionUuid {@code HibSchemaVersion#getUuid()} – die UUID der <em>Version</em>, nicht
     *                    die des Schemas; sie ist der Cache-Schluessel der Auswahl
     * @param model       Schemamodell ({@code HibSchemaVersion#getSchema()})
     * @return Deskriptor fuer {@link RagFieldSelector#selectionFor(RagSchemaDescriptor)}
     */
    public static RagSchemaDescriptor describe(String name, String versionUuid, FieldSchemaContainer model) {
        List<RagFieldDescriptor> fields = new ArrayList<>();
        if (model != null) {
            for (FieldSchema field : model.getFields()) {
                fields.add(describe(field));
            }
        }
        Map<String, Object> settings = model == null ? Map.of() : toMap(model.getElasticsearch());
        return new RagSchemaDescriptor(name, versionUuid, settings, fields);
    }

    /**
     * Beschreibt ein Schemafeld.
     *
     * @param field Feld der Schemaversion
     * @return Deskriptor mit Name, Typ, {@code noIndex} und den {@code elasticsearch}-Settings
     */
    public static RagFieldDescriptor describe(FieldSchema field) {
        return new RagFieldDescriptor(field.getName(), field.getType(), field.isNoIndex(),
                toMap(field.getElasticsearch()));
    }

    /**
     * Wandelt einen Vert.x-JSON-Block in einfache Sammlungen.
     *
     * <p>Bewusst rekursiv: aus JSON dekodierte Objekte liefert {@link JsonObject#getMap()} bereits
     * als {@code Map}, programmatisch gesetzte Werte koennen aber verschachtelte
     * {@code JsonObject}/{@code JsonArray} enthalten. Die Auswahlregeln sollen beide Faelle gleich
     * sehen.
     *
     * @param json Settings-Block, darf {@code null} sein
     * @return unveraenderliche Sicht als {@code Map}; leer bei {@code null}
     */
    static Map<String, Object> toMap(JsonObject json) {
        if (json == null) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : json.getMap().entrySet()) {
            result.put(entry.getKey(), normalize(entry.getValue()));
        }
        return result;
    }

    private static Object normalize(Object value) {
        if (value instanceof JsonObject json) {
            return toMap(json);
        }
        if (value instanceof JsonArray array) {
            return normalizeList(array.getList());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List<?> list) {
            return normalizeList(list);
        }
        return value;
    }

    private static List<Object> normalizeList(List<?> values) {
        List<Object> result = new ArrayList<>(values.size());
        for (Object value : values) {
            result.add(normalize(value));
        }
        return result;
    }
}
