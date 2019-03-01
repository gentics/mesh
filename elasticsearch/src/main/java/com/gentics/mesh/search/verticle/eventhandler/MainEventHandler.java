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

	private final GroupEventHandler groupEventHandler;
	private final TagEventHandler tagEventHandler;
	private final TagFamilyEventHandler tagFamilyEventHandler;
	private final NodeEventHandler nodeEventHandler;

	private final Map<MeshEvent, EventHandler> handlers;
	private final ClearEventHandler clearEventHandler;
	private final SyncEventHandler syncEventHandler;
	private final BranchEventHandler branchEventHandler;
	private final SchemaMigrationEventHandler schemaMigrationEventHandler;
	private final PermissionChangedEventHandler permissionChangedEventHandler;

	@Inject
	public MainEventHandler(SyncEventHandler syncEventHandler,
							EventHandlerFactory eventHandlerFactory,
							GroupEventHandler groupEventHandler,
							TagEventHandler tagEventHandler,
							TagFamilyEventHandler tagFamilyEventHandler,
							NodeEventHandler nodeEventHandler,
							ClearEventHandler clearEventHandler,
							BranchEventHandler branchEventHandler,
							SchemaMigrationEventHandler schemaMigrationEventHandler,
							PermissionChangedEventHandler permissionChangedEventHandler) {
		this.syncEventHandler = syncEventHandler;
		this.eventHandlerFactory = eventHandlerFactory;
		this.groupEventHandler = groupEventHandler;
		this.tagEventHandler = tagEventHandler;
		this.tagFamilyEventHandler = tagFamilyEventHandler;
		this.nodeEventHandler = nodeEventHandler;
		this.clearEventHandler = clearEventHandler;
		this.branchEventHandler = branchEventHandler;
		this.schemaMigrationEventHandler = schemaMigrationEventHandler;
		this.permissionChangedEventHandler = permissionChangedEventHandler;

		handlers = createHandlers();
	}

	private Map<MeshEvent, EventHandler> createHandlers() {
		return Stream.of(
			syncEventHandler,
			clearEventHandler,
			forEvent(MeshEvent.SEARCH_FLUSH_REQUEST, MainEventHandler::flushRequest),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getSchema, SchemaContainer.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getMicroschema, MicroschemaContainer.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getUser, User.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getRole, Role.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getProject, Project.composeIndexName()),
			groupEventHandler,
			tagEventHandler,
			tagFamilyEventHandler,
			nodeEventHandler,
			branchEventHandler,
			schemaMigrationEventHandler,
			permissionChangedEventHandler
		).collect(toListWithMultipleKeys(EventHandler::handledEvents));
	}

	private static Flowable<SearchRequest> flushRequest(MessageEvent event) {
		return Flowable.just(SearchRequest.create(provider -> Completable.complete()));
	}

	@Override
	public Flowable<? extends SearchRequest> handle(MessageEvent messageEvent) {
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
