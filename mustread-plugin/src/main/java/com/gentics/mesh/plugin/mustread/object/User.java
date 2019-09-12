package com.gentics.mesh.plugin.mustread.object;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class User {
    private String uuid;

    public User(String uuid) {
        this.uuid = uuid;
    }

    public static User parse(RoutingContext rc) {
        return parse(rc.user().principal());
    }

    public static User parse(JsonObject json) {
        String uuid = json.getString("uuid");

        User user = new User(uuid);

        return user;
    }

    public String getUUID() {
        return uuid;
    }

    @Override
    public String toString() {
        return "User{" +
                "uuid='" + uuid + '\'' +
                '}';
    }
}
