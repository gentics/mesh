package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.request.ElasticsearchRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.gentics.mesh.search.verticle.eventhandler.Util.toListWithMultipleKeys;

/**
 * Maps events from mesh to elastic search requests.
 * Call {@link #handledEvents()} to get a collection of all supported events
 * Call {@link #handle(MessageEvent)} to map an event to a list of elastic search requests.
 */
@Singleton
public class MainEventhandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(MainEventhandler.class);

	private final MeshHelper helper;
	private final GroupHandler groupHandler;
	private final MeshEntities entities;

	private final Map<MeshEvent, EventHandler> handlers;

	@Inject
	public MainEventhandler(MeshHelper helper, GroupHandler groupHandler, MeshEntities entities) {
		this.helper = helper;
		this.groupHandler = groupHandler;
		this.entities = entities;

		handlers = createHandlers();
	}

	private Map<MeshEvent, EventHandler> createHandlers() {
		return Stream.of(
			new SimpleEventHandler<>(helper, entities.user, User.composeIndexName()),
			new SimpleEventHandler<>(helper, entities.role, Role.composeIndexName()),
			new SimpleEventHandler<>(helper, entities.project, Project.composeIndexName()),
			groupHandler
		).collect(toListWithMultipleKeys(EventHandler::handledEvents));
	}

	@Override
	public List<ElasticsearchRequest> handle(MessageEvent messageEvent) {
		return handlers.get(messageEvent.event).handle(messageEvent);
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return handlers.keySet();
	}
}
