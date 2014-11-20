package com.gentics;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import com.gentics.data.Page;

public class DataVerticle extends AbstractVerticle {

	public static final Logger log = LoggerFactory.getLogger(MainVerticle.class);


	@Override
	public void start() {
		getVertx().eventBus().consumer("data-load").handler(ar -> {
			//TODO Fix me
			//long id = ar.body().getLong("id");
			long id = 0;

			Page page = MainVerticle.pages.get(id);
			if (page != null) {
//				ar.reply(new JsonObject(gson.toJson(page)));
			} else {
				ar.reply(null);
			}
		});

	}

	@Override
	public void stop() {
		log.info("Stopped " + getClass().getName());
	}
}
