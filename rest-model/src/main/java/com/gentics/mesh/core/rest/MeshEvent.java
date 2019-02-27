package com.gentics.mesh.core.rest;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMeshEventModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Central list of used eventbus addresses.
 */
public enum MeshEvent {

	MESH_MIGRATION("mesh.migration", null),

	/**
	 * Schema migration start event.
	 */
	SCHEMA_MIGRATION_START("mesh.schema.migration.start", SchemaMigrationMeshEventModel.class),

	/**
	 * Schema migration finished event (contains status information)
	 */
	SCHEMA_MIGRATION_FINISHED("mesh.schema.migration.finished", SchemaMigrationMeshEventModel.class),

	/**
	 * Event which is send once the schema gets assigned to a branch.
	 */
	SCHEMA_BRANCH_ASSIGN("mesh.schema-branch.assign", null),

	/**
	 * Microschema migration start event.
	 */
	MICROSCHEMA_MIGRATION_START("mesh.microschema.migration.start", MicroschemaMigrationMeshEventModel.class),

	/**
	 * Microschema migration finished event.
	 */
	MICROSCHEMA_MIGRATION_FINISHED("mesh.microschema.migration.finished", MicroschemaMigrationMeshEventModel.class),

	/**
	 * Branch migration start event.
	 */
	BRANCH_MIGRATION_START("mesh.branch.migration.start", BranchMigrationMeshEventModel.class),

	/**
	 * Branch migration finished event.
	 */
	BRANCH_MIGRATION_FINISHED("mesh.branch.migration.finished", BranchMigrationMeshEventModel.class),

	/**
	 * Event which is send once the mesh instance is fully started and ready to accept requests.
	 */
	STARTUP("mesh.startup-complete", null),

	/**
	 * Address for handler which will process registered job.
	 */
	JOB_WORKER_ADDRESS("job.worker", null),

	/**
	 * Event which is send once a new node is joining the cluster.
	 */
	CLUSTER_NODE_JOINING("mesh.cluster.node.joining", null),

	/**
	 * Event which is send once a node finished joining the cluster.
	 */
	CLUSTER_NODE_JOINED("mesh.cluster.node.joined", null),

	/**
	 * Event which is send once a node is about to leave the cluster.
	 */
	CLUSTER_NODE_LEAVING("mesh.cluster.node.leaving", null),

	/**
	 * Event which is send once a node left the cluster.
	 */
	CLUSTER_NODE_LEFT("mesh.cluster.node.left", null),

	/**
	 * Event which is send once the database status (offline, online, not_available, backup, synchronizing) changes.
	 */
	CLUSTER_DATABASE_CHANGE_STATUS("mesh.cluster.db.status", null),

	/**
	 * Event which is send to update the permission stores.
	 */
	CLEAR_PERMISSION_STORE("mesh.clear-permission-store", null),

	/* User */

	USER_CREATED("mesh.user.created", MeshElementEventModelImpl.class),

	USER_UPDATED("mesh.user.updated", MeshElementEventModelImpl.class),

	USER_DELETED("mesh.user.deleted", MeshElementEventModelImpl.class),

	/* Group */

	GROUP_CREATED("mesh.group.created", MeshElementEventModelImpl.class),

	GROUP_UPDATED("mesh.group.updated", MeshElementEventModelImpl.class),

	GROUP_DELETED("mesh.group.deleted", MeshElementEventModelImpl.class),

	/* Role */

	ROLE_CREATED("mesh.role.created", MeshElementEventModelImpl.class),

	ROLE_UPDATED("mesh.role.updated", MeshElementEventModelImpl.class),

	ROLE_DELETED("mesh.role.deleted", MeshElementEventModelImpl.class),

	/* Tag */

	TAG_CREATED("mesh.tag.created", TagMeshEventModel.class),

	TAG_UPDATED("mesh.tag.updated", TagMeshEventModel.class),

	TAG_DELETED("mesh.tag.deleted", TagMeshEventModel.class),

	/* Tag Family */

	TAG_FAMILY_CREATED("mesh.tagfamily.created", TagFamilyMeshEventModel.class),

	TAG_FAMILY_UPDATED("mesh.tagfamily.updated", TagFamilyMeshEventModel.class),

	TAG_FAMILY_DELETED("mesh.tagfamily.deleted", TagFamilyMeshEventModel.class),

	/* Project */

	PROJECT_CREATED("mesh.project.created", MeshElementEventModelImpl.class),

	PROJECT_UPDATED("mesh.project.updated", MeshElementEventModelImpl.class),

	PROJECT_DELETED("mesh.project.deleted", MeshElementEventModelImpl.class),

	/* Node */

	NODE_CREATED("mesh.node.created", NodeMeshEventModel.class),

	NODE_UPDATED("mesh.node.updated", NodeMeshEventModel.class),

	NODE_DELETED("mesh.node.deleted", NodeMeshEventModel.class),

	/* Schema */

	SCHEMA_CREATED("mesh.schema.created", MeshElementEventModelImpl.class),

	SCHEMA_UPDATED("mesh.schema.updated", MeshElementEventModelImpl.class),

	SCHEMA_DELETED("mesh.schema.deleted", MeshElementEventModelImpl.class),

	/* Microschema */

	MICROSCHEMA_CREATED("mesh.microschema.created", MeshElementEventModelImpl.class),

	MICROSCHEMA_UPDATED("mesh.microschema.updated", MeshElementEventModelImpl.class),

	MICROSCHEMA_DELETED("mesh.microschema.deleted", MeshElementEventModelImpl.class),

	/* Branch */

	BRANCH_CREATED("mesh.branch.created", BranchMeshEventModel.class),

	BRANCH_UPDATED("mesh.branch.updated", BranchMeshEventModel.class),

	BRANCH_DELETED("mesh.branch.deleted", BranchMeshEventModel.class),

	/* Search index related */

	/**
	 * Address for the handler which will process index sync requests.
	 */
	INDEX_SYNC_WORKER_ADDRESS("index-sync.worker", null),

	/**
	 * Address to which index sync results will be published (failed, succeeded)
	 */
	INDEX_SYNC("mesh.search.index.sync", null),

	/**
	 * Emitted when an index clear is requested.
	 */
	INDEX_CLEAR_REQUEST("mesh.search.index.clear.request", null),

	/**
	 * Emitted when an index clear has completed.
	 */
	INDEX_CLEAR_COMPLETED("mesh.search.index.clear.completed", null),

	/**
	 * Event that is emitted when the search verticle has been working and is now idle.
	 */
	SEARCH_IDLE("mesh.search.process.idle", null),

	/**
	 * Event that will cause all pending Elasticsearch requests to be sent.
	 */
	SEARCH_FLUSH_REQUEST("mesh.search.flush.request", null);

	public final String address;
	public final Class<? extends MeshEventModel> bodyModel;

	private static final Map<String, MeshEvent> events = createEventMap();

	/**
	 * Gets the event with the given address. Returns an empty optional if the address is invalid.
	 *
	 * @param address
	 * @return
	 */
	public static Optional<MeshEvent> fromAddress(String address) {
		return Optional.ofNullable(events.get(address));
	}

	private static Map<String, MeshEvent> createEventMap() {
		return Stream.of(values())
			.collect(Collectors.toMap(
				MeshEvent::getAddress,
				Function.identity()));
	}

	MeshEvent(String address, Class<? extends MeshEventModel> bodyModel) {
		this.address = address;
		this.bodyModel = bodyModel;
	}

	public static Completable doAndWaitForEvent(MeshEvent event, Action runnable) {
		return Completable.create(sub -> {
			EventBus eventbus = Mesh.mesh().getVertx().eventBus();
			MessageConsumer<Object> consumer = eventbus.consumer(event.address)
				.handler(ev -> sub.onComplete())
				.exceptionHandler(sub::onError);
			consumer.completionHandler(ignore -> {
				try {
					runnable.run();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			sub.setCancellable(consumer::unregister);
		});
	}

	public static Completable waitForEvent(MeshEvent event) {
		return doAndWaitForEvent(event, () -> {});
	}

	@Override
	public String toString() {
		return address;
	}

	// /**
	// * Address to query the index sync status.
	// */
	// INDEX_SYNC_STATUS_EVENT("mesh.search.index.sync.status"),

	public static void triggerJobWorker() {
		Mesh mesh = Mesh.mesh();
		EventBus eb = mesh.getVertx().eventBus();
		String name = mesh.getOptions().getNodeName();
		eb.send(JOB_WORKER_ADDRESS + name, null);
	}

	/**
	 * Returns a list of all events which are publicly exposed via the eventbus websocket bridge.
	 * 
	 * @return
	 */
	public static List<MeshEvent> publicEvents() {
		List<MeshEvent> events = new ArrayList<>();

		events.add(MESH_MIGRATION);

		events.add(BRANCH_MIGRATION_START);
		events.add(BRANCH_MIGRATION_FINISHED);

		events.add(SCHEMA_MIGRATION_START);
		events.add(SCHEMA_MIGRATION_FINISHED);

		events.add(MICROSCHEMA_MIGRATION_START);
		events.add(MICROSCHEMA_MIGRATION_FINISHED);

		events.add(STARTUP);

		events.add(CLUSTER_NODE_JOINING);

		events.add(CLUSTER_NODE_JOINED);

		events.add(CLUSTER_NODE_LEFT);

		events.add(CLUSTER_DATABASE_CHANGE_STATUS);
		events.add(CLEAR_PERMISSION_STORE);

		/* User */

		events.add(USER_CREATED);

		events.add(USER_UPDATED);

		events.add(USER_DELETED);

		/* Group */

		events.add(GROUP_CREATED);

		events.add(GROUP_UPDATED);

		events.add(GROUP_DELETED);

		/* Role */

		events.add(ROLE_CREATED);

		events.add(ROLE_UPDATED);

		events.add(ROLE_DELETED);

		/* Tag */

		events.add(TAG_CREATED);

		events.add(TAG_UPDATED);

		events.add(TAG_DELETED);

		/* Tag */

		events.add(TAG_FAMILY_CREATED);

		events.add(TAG_FAMILY_UPDATED);

		events.add(TAG_FAMILY_DELETED);

		/* Project */

		events.add(PROJECT_CREATED);

		events.add(PROJECT_UPDATED);

		events.add(PROJECT_DELETED);

		/* Node */

		events.add(NODE_CREATED);

		events.add(NODE_UPDATED);

		events.add(NODE_DELETED);

		/* Schema */

		events.add(SCHEMA_CREATED);

		events.add(SCHEMA_UPDATED);

		events.add(SCHEMA_DELETED);

		/* Microschema */

		events.add(MICROSCHEMA_CREATED);

		events.add(MICROSCHEMA_UPDATED);

		events.add(MICROSCHEMA_DELETED);

		/* Branch */

		events.add(BRANCH_CREATED);

		events.add(BRANCH_UPDATED);

		events.add(BRANCH_DELETED);

		/* Search Index */

		events.add(INDEX_SYNC);

		// events.add(INDEX_SYNC_STATUS_EVENT);

		return events;
	}

	public String getAddress() {
		return address;
	}

	public Class<? extends MeshEventModel> getBodyModel() {
		return bodyModel;
	}
}
