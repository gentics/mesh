package com.gentics.mesh.search.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;

public class ElasticsearchProcessVerticle extends AbstractVerticle {

	public static final String ADDR = "mesh.search.task";

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		vertx.eventBus().consumer(ADDR, this::handleEvent);
	}

	public <T> void handleEvent(Message<T> handler) {

	}

}
