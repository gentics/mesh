package com.gentics.handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

public class DeployHandler extends DeploymentManager implements
		Handler<AsyncResult<Message<String>>> {

	Logger LOG = LoggerFactory.getLogger(DeployHandler.class);

	public DeployHandler(Vertx vertx) {
		super(vertx);
	}

	@Override
	public void handle(AsyncResult<Message<String>> message) {
		String name = message.result().body();
		if (name.startsWith("module:")) {
			final String moduleName = name.substring("module:".length());

			JsonObject appConfig = Vertx.currentContext().config();
			JsonObject moduleConfig = appConfig.getJsonObject(moduleName);
			// TODO migration maybe the module must be wrapped in order to work
			Vertx.vertx().deployVerticle(
					moduleName,
					new DeploymentOptions(moduleConfig),
					result -> {
						if (result.succeeded()) {
							LOG.info("Deployment of module " + moduleName
									+ " succeeded");
							registerModule(moduleName, result.result());
							message.result().reply(result.result());
						} else {
							LOG.error("Deployment of module " + moduleName
									+ " failed", result.cause());
							message.result().fail(0, "Deployment failed");
						}
					});
		} else if (name.startsWith("verticle:")) {
			final String verticleName = name.substring("verticle:".length());

			JsonObject appConfig = Vertx.currentContext().config();
			JsonObject verticleConfig = appConfig.getJsonObject(verticleName);

		} else {
			message.result().reply(name + " does not start with module: or verticle:");
		}
	}

}
