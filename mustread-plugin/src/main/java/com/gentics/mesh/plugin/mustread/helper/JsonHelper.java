package com.gentics.mesh.plugin.mustread.helper;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


abstract public class JsonHelper {

    public static void fetchKeys(JsonObject jsonObject, String searchKey, Consumer<String> call) {
        jsonObject.forEach(item -> {
            String key = item.getKey();
            Object value = item.getValue();

            if (value instanceof String && key.equals(searchKey)) {
                try {
                    call.accept((String) value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            fetchKeys(value, searchKey, call);
        });
    }

    public static void fetchKeys(JsonArray jsonArray, String searchKey, Consumer<String> call) {
        jsonArray.forEach(item -> {
            fetchKeys(item, searchKey, call);
        });
    }

    public static void fetchKeys(Object object, String searchKey, Consumer<String> call) {
        if (object instanceof JsonArray) {
            fetchKeys((JsonArray) object, searchKey, call);
        }
        if (object instanceof JsonObject) {
            fetchKeys((JsonObject) object, searchKey, call);
        }
    }

    public static void fetchKeys(GraphQLResponse graphql, String searchKey, Consumer<String> call) {
        fetchKeys(graphql.getData(), searchKey, call);
    }

}
