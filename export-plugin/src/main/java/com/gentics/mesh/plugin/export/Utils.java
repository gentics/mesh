package com.gentics.mesh.plugin.export;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.validation.ConstraintValidatorContext;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class Utils {

    public static Optional<Object> getValueByPath(Object obj, List<String> split) {
        for (int i = 0; i < split.size(); i++) {
            if (obj instanceof JsonObject) {
                if (((JsonObject) obj).containsKey(split.get(i))) {
                    obj = ((JsonObject) obj).getValue(split.get(i));
                    continue;
                }
            } else if (obj instanceof JsonArray) {
                if (!((JsonArray) obj).isEmpty()) {
                    // TODO: Add support for arrays?
                    obj = ((JsonArray) obj).getValue(0);
                    continue;
                }
            }

            return Optional.empty();
        }

        return Optional.ofNullable(obj);
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
}
