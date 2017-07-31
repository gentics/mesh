package com.gentics.mesh.event;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InvalidNameException;

import com.gentics.mesh.Events;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for common mesh events
 */
@Singleton
public class MeshEventHandler {

	private static final Logger log = LoggerFactory.getLogger(MeshEventHandler.class);

	@Inject
	public MeshEventHandler() {
	}

	public void registerHandlers() {
		addProjectHandlers();
	}

	private void addProjectHandlers() {
		Mesh.vertx().eventBus().consumer(Events.EVENT_PROJECT_CREATED, (Message<JsonObject> rh) -> {
			JsonObject json = rh.body();
			String name = json.getString("name");
			try {
				RouterStorage.getIntance().addProjectRouter(name);
				if (log.isInfoEnabled()) {
					log.info("Registered project {" + name + "}");
				}
			} catch (InvalidNameException e) {
				// TODO should we really fail here?
				throw error(BAD_REQUEST, "Error while adding project to router storage", e);
			}
		});

	}

}
