package com.gentics.handler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

import java.util.Map;

public class ListDeployments extends DeploymentManager implements Handler<Message<String>> {

	public ListDeployments(Vertx vertx) {
		super(vertx);
	}

	@Override
	public void handle(Message<String> event) {
		Map<String, Integer> modules = getDeployedModules();
		StringBuffer response = new StringBuffer();
		response.append("Deployed Modules\n");
		for (Map.Entry<String, Integer> entry : modules.entrySet()) {
			response.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
		}

		event.reply(response.toString());
	}

}
