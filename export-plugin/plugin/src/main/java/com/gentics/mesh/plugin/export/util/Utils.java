package com.gentics.mesh.plugin.export.util;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.plugin.export.model.BooleanMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class Utils {
    private Utils() {}

    public static List<String> getPathAsList(String path) {
        return Arrays.asList(path.split("\\."));
    }

    public static Optional<Object> getValueByPath(Object obj, List<String> split) {
        for (int i = 0; i < split.size(); i++) {
            if (obj instanceof JsonObject) {
                if (((JsonObject) obj).containsKey(split.get(i))) {
                    obj = ((JsonObject) obj).getValue(split.get(i));
                    continue;
                }
            } else if (obj instanceof JsonArray) {
                if (((JsonArray) obj).isEmpty()) {
                    continue;
                }
                JsonArray out = new JsonArray();

                List<String> innerPath = split.subList(i, split.size());
                ((JsonArray) obj).stream()
                    .map(element -> getValueByPath(element, innerPath))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(found -> {
                       if (found instanceof JsonArray) {
                           addRecursive(out, (JsonArray) found);
                       } else {
                           out.add(found);
                       }
                    });

                return out.isEmpty() ? Optional.empty() : Optional.of(out);
            }

            return Optional.empty();
        }

        return Optional.ofNullable(obj);
    }

    private static void addRecursive(JsonArray out, JsonArray element) {
        for (Object item : element) {
            if (item instanceof JsonArray) {
                addRecursive(out, (JsonArray) item);
            } else {
                out.add(item);
            }
        }
    }

    public static BooleanMapping mergeMappings(BooleanMapping ...mappings) {
        BooleanMapping out = new BooleanMapping();
        for (BooleanMapping item : mappings) {
            if (item == null) {
                continue;
            }
            if (item.getTrueValue() != null) {
                out.setTrueValue(item.getTrueValue());
            }
            if (item.getFalseValue() != null) {
                out.setFalseValue(item.getFalseValue());
            }
            if (item.getNullValue() != null) {
                out.setNullValue(item.getNullValue());
            }
        }

        return out;
    }

    public static void addMessageToContext(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    public static Instant parseIsoDate(String date) {
        try {
            return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date));
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonObject mapThrowableToJsonObject(Throwable t) {
        return mapThrowableToJsonObject(t, false);
    }

    public static JsonObject mapThrowableToJsonObject(Throwable t, boolean shallow) {
        JsonObject obj = new JsonObject()
            .put("type", t.getClass().getCanonicalName())
            .put("message", t.getMessage());

        // Only add the localized message if it's different
        if (!Objects.equals(t.getMessage(), t.getLocalizedMessage())) {
            obj.put("localizedMessage", t.getLocalizedMessage());
        }

        if (!shallow && t.getCause() != null) {
            obj.put("cause", mapThrowableToJsonObject(t, true));
        }

        if (!shallow && t.getStackTrace() != null && t.getStackTrace().length > 0) {
            obj.put("stacktrace", Arrays.stream(t.getStackTrace())
                .map(Utils::mapStacktraceToJsonObject)
                .collect(() -> new JsonArray(), JsonArray::add, JsonArray::addAll));
        }

        return obj;
    }

    public static JsonObject mapStacktraceToJsonObject(StackTraceElement e) {
        JsonObject obj = new JsonObject()
            .put("class", e.getClassName())
            .put("method", e.getMethodName())
            .put("line", e.getLineNumber());

        return obj;
    }

    public static String stripSlashes(String str) {
        return str == null ? "" : str.replaceAll("(^\\/)|(\\/$)", "");
    }

    public static String stripEndingSlashes(String str) {
        return str == null ? "" : str.replaceAll("(\\/$)", "");
    }

    public static Map<String, Map<String, String>> mergeMappings(List<Map<String, Map<String, String>>> mappings) {
        Map<String, Map<String, String>> output = new HashMap<>();

        Stream.of(mappings).forEach(singleMap -> {
            singleMap.forEach(map -> {
                map.forEach((key, value) -> {
                    if (output.containsKey(map)) {
                        Map<String, String> master = output.get(map);
                        master.putAll(value);
                    } else {
                        output.put(key, value);
                    }
                });
            });
        });

        return output;
    }

    @NotNull
    public static Map<String, Map<String, String>> getMappingFromNode(NodeResponse node) {
        Map<String, Map<String, String>> mapping = new HashMap<>();

        if (node == null || node.getFields() == null) {
            return mapping;
        }

        node.getFields().keySet().forEach(fieldName -> {
            try {
                // Will throw an error if the field is not the correct type.
                // Which is fine, as we will just ignore it then.
                MicronodeFieldList list = node.getFields().getMicronodeFieldList(fieldName);
                Map<String, String> items = new HashMap<>();

                for (MicronodeField microNode : list.getItems()) {
                    try {
                        // Same as above
                        String id = microNode.getFields().getStringField("id").getString();
                        String value = null;
                        if (microNode.getFields().hasField("value")) {
                            value = microNode.getFields().getStringField("value").getString();
                        }
                        if (value == null && microNode.getFields().hasField("name")) {
                            value = microNode.getFields().getStringField("name").getString();
                        }
                        if (value == null && microNode.getFields().hasField("label")) {
                            value = microNode.getFields().getStringField("label").getString();
                        }
                        items.put(id, value);
                    } catch (Exception ignored) { }
                }

                if (!items.isEmpty()) {
                    mapping.computeIfAbsent(fieldName, k -> new HashMap<>())
                        .putAll(items);
                }
            } catch (Exception ignored) { }
        });

        return mapping;
    }
}
