package com.gentics.mesh.core.verticle.git;

import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.git.GitUtils;

public class GitWebHookVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(GitWebHookVerticle.class);

	public static final String GIT_WEBHOOK_EVENT_ADDRESS = "mesh.git.webhook";

	MessageConsumer<String> gitWebHookConsumer;

	public GitWebHookVerticle() {
		super("admin");
	}

	@Override
	public void start() throws Exception {
		super.start();
		addGitWebhookHandler();

	}

	@Override
	public void stop() throws Exception {

	}

	@Override
	public void registerEndPoints() throws Exception {
		if (gitWebHookConsumer != null) {
			gitWebHookConsumer.unregister();
		}
	}

	private void addGitWebhookHandler() {

		gitWebHookConsumer = vertx.eventBus().consumer(GIT_WEBHOOK_EVENT_ADDRESS, (Message<String> msg) -> {
			// TODO don't accept our own message
				if ("pull".equalsIgnoreCase(msg.body())) {
					try {
						GitUtils.pull();
					} catch (Exception e) {
						log.error("Error while handling git event from address {" + msg.address() + "}.", e);
					}
				}
			});

		route("/git/webhook").method(GET).handler(ctx -> {
			try {
				GitUtils.pull();
				vertx.eventBus().send(GIT_WEBHOOK_EVENT_ADDRESS, "pull");
			} catch (Exception e) {
				log.error("Error while handling webhook pull request.", e);
			}
		});
	}

}
