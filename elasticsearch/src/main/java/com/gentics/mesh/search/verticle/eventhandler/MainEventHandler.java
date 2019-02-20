package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CLEAR_REQUEST;
import static com.gentics.mesh.search.verticle.eventhandler.EventHandler.forEvent;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toListWithMultipleKeys;

/**
 * Maps events from mesh to elastic search requests.
 * Call {@link #handledEvents()} to get a collection of all supported events
 * Call {@link #handle(MessageEvent)} to map an event to a list of elastic search requests.
 */
@Singleton
public class MainEventHandler implements EventHandler {
	private static final Logger log = LoggerFactory.getLogger(MainEventHandler.class);

	private final SyncHandler syncHandler;
	private final ElasticSearchProvider elasticSearchProvider;

	private final MeshHelper helper;
	private final GroupHandler groupHandler;
	private final TagHandler tagHandler;
	private final TagFamilyHandler tagFamilyHandler;
	private final MeshEntities entities;

	private final Map<MeshEvent, EventHandler> handlers;
	private final NodeHandler nodeHandler;

	@Inject
	public MainEventHandler(SyncHandler syncHandler, ElasticSearchProvider elasticSearchProvider, MeshHelper helper, GroupHandler groupHandler, TagHandler tagHandler, TagFamilyHandler tagFamilyHandler, MeshEntities entities, NodeHandler nodeHandler) {
		this.syncHandler = syncHandler;
		this.elasticSearchProvider = elasticSearchProvider;
		this.helper = helper;
		this.groupHandler = groupHandler;
		this.tagHandler = tagHandler;
		this.tagFamilyHandler = tagFamilyHandler;
		this.entities = entities;
		this.nodeHandler = nodeHandler;

		handlers = createHandlers();
	}

	private Map<MeshEvent, EventHandler> createHandlers() {
		return Stream.of(
			syncHandler,
			forEvent(INDEX_CLEAR_REQUEST, event -> Flowable.just(client -> elasticSearchProvider.clear())),
			new SimpleEventHandler<>(helper, entities.schema, SchemaContainer.composeIndexName()),
			new SimpleEventHandler<>(helper, entities.microschema, MicroschemaContainer.composeIndexName()),
			new SimpleEventHandler<>(helper, entities.user, User.composeIndexName()),
			new SimpleEventHandler<>(helper, entities.role, Role.composeIndexName()),
			new SimpleEventHandler<>(helper, entities.project, Project.composeIndexName()),
			groupHandler,
			tagHandler,
			tagFamilyHandler,
			nodeHandler
		).collect(toListWithMultipleKeys(EventHandler::handledEvents));
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return handlers.get(messageEvent.event).handle(messageEvent);
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return handlers.keySet();
	}
}
