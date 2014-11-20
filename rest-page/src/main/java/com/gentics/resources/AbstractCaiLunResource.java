package com.gentics.resources;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

public abstract class AbstractCaiLunResource {
	DeliveryOptions optionWithTimeout = new DeliveryOptions(new JsonObject().put("timeout", 100000));

}
