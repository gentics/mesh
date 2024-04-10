package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.search.verticle.eventhandler.EventHandler.forEvent;
import static com.gentics.mesh.search.verticle.eventhandler.Util.logElasticSearchError;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toMultiMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.eventhandler.node.NodeContentEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.node.NodeMoveEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.node.NodeTagEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.project.ProjectCreateEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.project.ProjectDeleteEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.project.ProjectUpdateEventHandler;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

	private final NodeContentEventHandler nodeContentEventHandler;
	private final NodeTagEventHandler nodeTagEventHandler;
	private final NodeMoveEventHandler nodeMoveEventHandler;

	private final Map<MeshEvent, List<EventHandler>> handlers;
	private final ClearEventHandler clearEventHandler;
	private final SyncEventHandler syncEventHandler;
	private final BranchEventHandler branchEventHandler;
	private final SchemaMigrationEventHandler schemaMigrationEventHandler;
	private final MicroschemaMigrationEventHandler microschemaMigrationEventHandler;
	private final PermissionChangedEventHandler permissionChangedEventHandler;
	private final GroupUserAssignmentHandler userGroupAssignmentHandler;
	private final RoleDeletedEventHandler roleDeletedEventHandler;

	private final ProjectUpdateEventHandler projectUpdateEventHandler;
	private final ProjectCreateEventHandler projectCreateEventHandler;
	private final ProjectDeleteEventHandler projectDeleteEventHandler;

	private final CheckIndicesHandler checkIndicesHandler;

	@Inject
	public MainEventHandler(SyncEventHandler syncEventHandler,
							EventHandlerFactory eventHandlerFactory,
							GroupEventHandler groupEventHandler,
							TagEventHandler tagEventHandler,
							TagFamilyEventHandler tagFamilyEventHandler,
							NodeContentEventHandler nodeContentEventHandler,
							NodeTagEventHandler nodeTagEventHandler, NodeMoveEventHandler nodeMoveEventHandler, RoleDeletedEventHandler roleDeletedEventHandler, ProjectDeleteEventHandler projectDeleteEventHandler,
							ClearEventHandler clearEventHandler,
							BranchEventHandler branchEventHandler,
							SchemaMigrationEventHandler schemaMigrationEventHandler,
							MicroschemaMigrationEventHandler microschemaMigrationEventHandler,
							PermissionChangedEventHandler permissionChangedEventHandler,
							GroupUserAssignmentHandler userGroupAssignmentHandler,
							ProjectUpdateEventHandler projectUpdateEventHandler, ProjectCreateEventHandler projectCreateEventHandler, CheckIndicesHandler checkIndicesHandler) {
		this.syncEventHandler = syncEventHandler;
		this.eventHandlerFactory = eventHandlerFactory;
		this.groupEventHandler = groupEventHandler;
		this.tagEventHandler = tagEventHandler;
		this.tagFamilyEventHandler = tagFamilyEventHandler;
		this.nodeContentEventHandler = nodeContentEventHandler;
		this.nodeTagEventHandler = nodeTagEventHandler;
		this.nodeMoveEventHandler = nodeMoveEventHandler;
		this.roleDeletedEventHandler = roleDeletedEventHandler;
		this.projectDeleteEventHandler = projectDeleteEventHandler;
		this.clearEventHandler = clearEventHandler;
		this.branchEventHandler = branchEventHandler;
		this.schemaMigrationEventHandler = schemaMigrationEventHandler;
		this.microschemaMigrationEventHandler = microschemaMigrationEventHandler;
		this.permissionChangedEventHandler = permissionChangedEventHandler;
		this.userGroupAssignmentHandler = userGroupAssignmentHandler;
		this.projectUpdateEventHandler = projectUpdateEventHandler;
		this.projectCreateEventHandler = projectCreateEventHandler;
		this.checkIndicesHandler = checkIndicesHandler;

		handlers = createHandlers();
	}

	/**
	 * Creates a map of event handlers that will be used to delegate events to the correct handler.
	 * @return
	 */
	private Map<MeshEvent, List<EventHandler>> createHandlers() {
		return Stream.of(
			syncEventHandler,
			clearEventHandler,
			forEvent(MeshEvent.SEARCH_FLUSH_REQUEST, MainEventHandler::flushRequest),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getSchema, HibSchema.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getMicroschema, HibMicroschema.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getUser, HibUser.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getRole, HibRole.composeIndexName()),
			eventHandlerFactory.createSimpleEventHandler(MeshEntities::getProject, HibProject.composeIndexName()),
			groupEventHandler,
			tagEventHandler,
			tagFamilyEventHandler,
			nodeContentEventHandler,
			nodeTagEventHandler,
			nodeMoveEventHandler,
			roleDeletedEventHandler,
			projectDeleteEventHandler,
			projectUpdateEventHandler,
			projectCreateEventHandler,
			branchEventHandler,
			schemaMigrationEventHandler,
			microschemaMigrationEventHandler,
			permissionChangedEventHandler,
			userGroupAssignmentHandler,
			checkIndicesHandler
		).collect(toMultiMap(EventHandler::handledEvents));
	}

	/**
	 * Creates a fake elasticsearch request that is not bulkable. This will cause the bulk operator to flush
	 * pending requests.
	 * @param event
	 * @return
	 */
	private static Flowable<SearchRequest> flushRequest(MessageEvent event) {
		log.info("Flush request received");
		return Flowable.just(SearchRequest.create(provider -> Completable.complete()
			.doOnComplete(() -> log.info("Flushing requests"))
		));
	}

	@Override
	public Flowable<? extends SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.fromIterable(handlers.get(messageEvent.event))
			.flatMap(handler -> handler.handle(messageEvent), 1)
			.doOnError(err -> {
				String body = messageEvent.message == null ? null : messageEvent.message.toJson(false);
				logElasticSearchError(err, () -> log.error("Error while handling event {} with body {}", messageEvent.event, body, err));
			});
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return handlers.keySet();
	}
}
