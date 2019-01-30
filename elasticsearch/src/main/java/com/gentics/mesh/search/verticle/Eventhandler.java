package com.gentics.mesh.search.verticle;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.event.CreatedMeshEventModel;
import com.gentics.mesh.event.MeshEventModel;
import com.gentics.mesh.graphdb.spi.Database;
import io.reactivex.functions.Function;

import javax.inject.Inject;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gentics.mesh.Events.EVENT_USER_CREATED;

public class Eventhandler {
	private final BootstrapInitializer boot;
	private final Database db;

	private final Map<String, Function<MeshEventModel, List<ElasticSearchRequest>>> handlers = new HashMap<>();

	@Inject
	public Eventhandler(BootstrapInitializer boot, Database db) {
		this.boot = boot;
		this.db = db;

		createHandler(EVENT_USER_CREATED, CreatedMeshEventModel.class, event -> {
			return null;
		});
	}

	private String event;
	private Function<MeshEventModel, List<ElasticSearchRequest>> generator;

	private <T> void createHandler(String event, Class<T> clazz, Function<T, List<ElasticSearchRequest>> generator) {
		handlers.put(event, msg -> {
			if (msg.getClass().isAssignableFrom(clazz)) {
				return generator.apply((T) msg);
			} else {
				throw new InvalidParameterException("Could not apply event message to handler");
			}
		});
	}

	public List<ElasticSearchRequest> handle(MessageEvent messageEvent) throws Exception {
		return handlers.get(messageEvent.event).apply(messageEvent.message);
	}
}
