package com.gentics.mesh.plugin.export.model;

import io.vertx.core.json.JsonObject;

public interface IFilter {

    boolean isAllowed(JsonObject value);
}
