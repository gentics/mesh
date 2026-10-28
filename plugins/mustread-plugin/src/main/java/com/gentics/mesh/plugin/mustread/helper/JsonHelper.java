package com.gentics.mesh.plugin.mustread.helper;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

abstract public class JsonHelper {

    public static void fetchKeys(JsonObject jsonObject, String searchKey, Boolean publishedOnly, String publishedField, Consumer<String> call) {
        boolean tmpPublishValue = false;

        if (jsonObject.containsKey(publishedField)) {
            tmpPublishValue = jsonObject.getBoolean(publishedField);
        }

        // Must be final
        final boolean publishValue = tmpPublishValue;

        jsonObject.forEach(item -> {
            String key = item.getKey();
            Object value = item.getValue();

            if (value instanceof String && key.equals(searchKey) && (!publishedOnly || publishValue)) {
                try {
                    call.accept((String) value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            fetchKeys(value, searchKey, publishedOnly, publishedField, call);
        });
    }

    public static void fetchKeys(JsonArray jsonArray, String searchKey, Boolean publishedOnly, String publishedField, Consumer<String> call) {
        jsonArray.forEach(item -> {
            fetchKeys(item, searchKey, publishedOnly, publishedField, call);
        });
    }

    public static void fetchKeys(Object object, String searchKey, Boolean publishedOnly, String publishedField, Consumer<String> call) {
        if (object instanceof JsonArray) {
            fetchKeys((JsonArray) object, searchKey, publishedOnly, publishedField, call);
        }
        if (object instanceof JsonObject) {
            fetchKeys((JsonObject) object, searchKey, publishedOnly, publishedField, call);
        }
    }

    public static void fetchKeys(GraphQLResponse graphql, String searchKey, Boolean publishedOnly, String publishedField, Consumer<String> call) {
        fetchKeys(graphql.getData(), searchKey, publishedOnly, publishedField, call);
    }

}
