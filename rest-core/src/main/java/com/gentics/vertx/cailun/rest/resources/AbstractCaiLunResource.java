package com.gentics.vertx.cailun.rest.resources;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public abstract class AbstractCaiLunResource {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	public static final String DEFAULT_ADDRESS = "graph.cypher.query";

	DeliveryOptions optionWithTimeout = new DeliveryOptions(new JsonObject().put("timeout", 100000));

}
