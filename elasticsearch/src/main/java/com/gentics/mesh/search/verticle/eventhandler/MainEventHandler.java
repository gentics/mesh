package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

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

	private final EventHandlerFactory eventHandlerFactory;

	private final GroupHandler groupHandler;
	private final TagHandler tagHandler;
	private final TagFamilyHandler tagFamilyHandler;
	private final NodeHandler nodeHandler;

	private final Map<MeshEvent, EventHandler> handlers;
	private final ClearHandler clearHandler;
	private final SyncHandler syncHandler;
	private final BranchHandler branchHandler;
	private final SchemaMigrationHandler schemaMigrationHandler;

	@Inject
	public MainEventHandler(SyncHandler syncHandler, EventHandlerFactory eventHandlerFactory, GroupHandler groupHandler, TagHandler tagHandler, TagFamilyHandler tagFamilyHandler, NodeHandler nodeHandler, ClearHandler clearHandler, BranchHandler branchHandler, SchemaMigrationHandler schemaMigrationHandler) {
		this.syncHandler = syncHandler;
		this.eventHandlerFactory = eventHandlerFactory;
		this.groupHandler = groupHandler;
		this.tagHandler = tagHandler;
		this.tagFamilyHandler = tagFamilyHandler;
		this.nodeHandler = nodeHandler;
		this.clearHandler = clearHandler;
		this.branchHandler = branchHandler;
		this.schemaMigrationHandler = schemaMigrationHandler;

		handlers = createHandlers();
	}

	private Map<MeshEvent, EventHandler> createHandlers() {
		return Stream.of(
			syncHandler,
			clearHandler,
			forEvent(MeshEvent.SEARCH_FLUSH_REQUEST, MainEventHandler::flushRequest),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getSchema, SchemaContainer.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getMicroschema, MicroschemaContainer.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getUser, User.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getRole, Role.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getProject, Project.composeIndexName()),
			groupHandler,
			tagHandler,
			tagFamilyHandler,
			nodeHandler,
			branchHandler,
			schemaMigrationHandler
		).collect(toListWithMultipleKeys(EventHandler::handledEvents));
	}

	private static Flowable<SearchRequest> flushRequest(MessageEvent event) {
		return Flowable.just(SearchRequest.create(provider -> Completable.complete()));
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return handlers.get(messageEvent.event).handle(messageEvent)
			.onErrorResumeNext(err -> {
				String body = messageEvent.message == null ? null : messageEvent.message.toJson();
				log.error("Error while handling event {} with body {}", messageEvent.event, body, err);
				return Flowable.empty();
			});
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return handlers.keySet();
	}
}
