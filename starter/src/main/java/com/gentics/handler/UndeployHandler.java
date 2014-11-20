package com.gentics.handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndeployHandler extends DeploymentManager implements Handler<Message<String>> {

	public static final Logger LOG = LoggerFactory.getLogger(UndeployHandler.class);

	public UndeployHandler(Vertx vertx) {
		super(vertx);
	}

	@Override
	public void handle(final Message<String> message) {
		String name = message.body();

		if (name.startsWith("module:")) {
			final String moduleName = name.substring("module:".length());

			Set<String> deployedModules = getDeployedModules(moduleName, false);
			if (deployedModules == null) {
				message.fail(0, "Module " + moduleName + " is not deployed");
			} else {
				final String deploymentId = deployedModules.iterator().next();
				vertx.undeployVerticle(deploymentId, new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> result) {
						if (result.succeeded()) {
							LOG.info("Successfully undeployed module " + moduleName);
							unregisterModule(moduleName, deploymentId);
							message.reply("Succeeded");
						} else {
							LOG.error("Could not undeploy module " + moduleName, result.cause());
							message.fail(0, "failed");
						}
					}
				});
			}
		} else if (name.startsWith("verticle:")) {
			final String deploymentId = name.substring("verticle:".length());

			// container.undeployVerticle(deploymentId, new Handler<AsyncResult<Void>>() {
			// @Override
			// public void handle(AsyncResult<Void> result) {
			// if (result.succeeded()) {
			// vertx.sharedData().getMap(DeployHandler.MODULE_DEPLOYMENT_MAP).remove(deploymentId);
			// message.reply("Succeeded");
			// } else {
			// message.reply("failed");
			// }
			// }
			// });
		} else {
			message.reply(name + " does not start with module: or verticle:");
		}
	}
}
