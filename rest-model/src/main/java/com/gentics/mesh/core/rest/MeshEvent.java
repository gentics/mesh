package com.gentics.mesh.core.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMeshEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;

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
	SCHEMA_BRANCH_ASSIGN("mesh.schema-branch.assign", BranchSchemaAssignEventModel.class),

	/**
	 * Event which is send once the schema gets assigned to a branch.
	 */
	SCHEMA_BRANCH_UNASSIGN("mesh.schema-branch.unassign", BranchSchemaAssignEventModel.class),

	/**
	 * Event which is send once the microschema gets assigned to a branch.
	 */
	MICROSCHEMA_BRANCH_ASSIGN("mesh.microschema-branch.assign", BranchMicroschemaAssignModel.class),

	/**
	 * Event which is send once the microschema gets assigned to a branch.
	 */
	MICROSCHEMA_BRANCH_UNASSIGN("mesh.microschema-branch.unassign", BranchMicroschemaAssignModel.class),

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

	GROUP_USER_ASSIGNED("mesh.group-user.assigned", GroupUserAssignModel.class),

	GROUP_USER_UNASSIGNED("mesh.group-user.unassigned", GroupUserAssignModel.class),

	GROUP_ROLE_ASSIGNED("mesh.group-role.assigned", GroupRoleAssignModel.class),

	GROUP_ROLE_UNASSIGNED("mesh.group-role.unassigned", GroupRoleAssignModel.class),

	/* Role */

	ROLE_CREATED("mesh.role.created", MeshElementEventModelImpl.class),

	ROLE_UPDATED("mesh.role.updated", MeshElementEventModelImpl.class),

	ROLE_DELETED("mesh.role.deleted", MeshElementEventModelImpl.class),

	ROLE_PERMISSIONS_CHANGED("mesh.role.permissions.changed", PermissionChangedEventModel.class),

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

	PROJECT_SCHEMA_ASSIGNED("mesh.project-schema.assigned", ProjectSchemaEventModel.class),

	PROJECT_SCHEMA_UNASSIGNED("mesh.project-schema.unassigned", ProjectSchemaEventModel.class),

	PROJECT_MICROSCHEMA_ASSIGNED("mesh.project-microschema.assigned", ProjectMicroschemaEventModel.class),

	PROJECT_MICROSCHEMA_UNASSIGNED("mesh.project-microschema.unassigned", ProjectMicroschemaEventModel.class),

	PROJECT_LATEST_BRANCH_UPDATED("mesh.project-latest-branch.updated", ProjectBranchEventModel.class),

	/* Node */

	NODE_CREATED("mesh.node.created", NodeMeshEventModel.class),

	NODE_UPDATED("mesh.node.updated", NodeMeshEventModel.class),

	NODE_DELETED("mesh.node.deleted", NodeMeshEventModel.class),

	NODE_TAGGED("mesh.node.tagged", NodeTaggedEventModel.class),

	NODE_UNTAGGED("mesh.node.untagged", NodeTaggedEventModel.class),

	NODE_PUBLISHED("mesh.node.published", NodeMeshEventModel.class),

	NODE_UNPUBLISHED("mesh.node.unpublished", NodeMeshEventModel.class),

	NODE_MOVED("mesh.node.moved", MeshElementEventModelImpl.class),

	NODE_CONTENT_DELETED("mesh.node-content.deleted", NodeMeshEventModel.class),

	NODE_CONTENT_CREATED("mesh.node-content.created", NodeMeshEventModel.class),

	NODE_REFERENCE_UPDATED("mesh.node-reference.updated", NodeMeshEventModel.class),

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

	BRANCH_TAGGED("mesh.branch.tagged", BranchTaggedEventModel.class),

	BRANCH_UNTAGGED("mesh.branch.untagged", BranchTaggedEventModel.class),

	/* Search index related (SYNC) */

	/**
	 * Address for the handler which will process index sync requests.
	 */
	INDEX_SYNC_REQUEST("mesh.search.index.sync.request", null),

	/**
	 * Emitted when an index sync process starts.
	 */
	INDEX_SYNC_START("mesh.search.index.sync.start", null),

	/**
	 * Address to which index sync results will be published (failed, succeeded)
	 */
	INDEX_SYNC_FINISHED("mesh.search.index.sync.finished", null),

	/* Search index related (CLEAR) */

	/**
	 * Event which will trigger the index clear process.
	 */
	INDEX_CLEAR_REQUEST("mesh.search.index.clear.request", null),

	/**
	 * Emitted when an index clear is starting.
	 */
	INDEX_CLEAR_START("mesh.search.index.clear.start", null),

	/**
	 * Emitted when an index clear has finished.
	 */
	INDEX_CLEAR_FINISHED("mesh.search.index.clear.finished", null),

	/**
	 * Event that is emitted when the search verticle has been working and is now idle.
	 */
	SEARCH_IDLE("mesh.search.process.idle", null),

	/**
	 * Event that will cause all pending Elasticsearch requests to be sent.
	 */
	SEARCH_FLUSH_REQUEST("mesh.search.flush.request", null),

	// Backup & Restore Events

	GRAPH_BACKUP_START("mesh.graph.backup.start", null),

	GRAPH_BACKUP_FINISHED("mesh.graph.backup.finished", null),

	GRAPH_RESTORE_START("mesh.graph.restore.start", null),

	GRAPH_RESTORE_FINISHED("mesh.graph.restore.finished", null),

	GRAPH_EXPORT_START("mesh.graph.export.start", null),

	GRAPH_EXPORT_FINISHED("mesh.graph.export.finished", null),

	GRAPH_IMPORT_START("mesh.graph.import.start", null),

	GRAPH_IMPORT_FINISHED("mesh.graph.import.finished", null),

	REPAIR_START("mesh.graph.repair.start", null),

	REPAIR_FINISHED("mesh.graph.repair.finished", null),

	// Plugin Events

	PLUGIN_DEPLOYING("mesh.plugin.deploying", null),

	PLUGIN_DEPLOYED("mesh.plugin.deployed", null),

	PLUGIN_UNDEPLOYING("mesh.plugin.undeploying", null),

	PLUGIN_UNDEPLOYED("mesh.plugin.undeployed", null);

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

	/**
	 * Invoke the given runnable and wait for the event.
	 * 
	 * @param event
	 * @param runnable 
	 * @return
	 */
	public static Completable doAndWaitForEvent(MeshEvent event, Action runnable) {
		return Completable.create(sub -> {
			EventBus eventbus = Mesh.mesh().getVertx().eventBus();
			MessageConsumer<Object> consumer = eventbus.consumer(event.address)
				.handler(ev -> sub.onComplete())
				.exceptionHandler(sub::onError);
			// The handler will be invoked once the event listener is registered 
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
		return doAndWaitForEvent(event, () -> {
		});
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
		eb.publish(JOB_WORKER_ADDRESS + name, null);
	}

	/**
	 * Returns a list of all events which are publicly exposed via the eventbus websocket bridge.
	 * 
	 * @return
	 */
	public static List<MeshEvent> publicEvents() {
		List<MeshEvent> events = new ArrayList<>();
		events.addAll(Arrays.asList(MeshEvent.values()));
		return events;
	}

	public String getAddress() {
		return address;
	}

	public Class<? extends MeshEventModel> getBodyModel() {
		return bodyModel;
	}
}
