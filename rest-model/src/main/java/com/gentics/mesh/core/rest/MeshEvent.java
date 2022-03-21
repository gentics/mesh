package com.gentics.mesh.core.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
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
import com.gentics.mesh.core.rest.event.job.JobEventModel;
import com.gentics.mesh.core.rest.event.job.ProjectVersionPurgeEventModel;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMovedEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectBranchEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.core.rest.event.s3binary.S3BinaryEventModel;
import com.gentics.mesh.core.rest.event.search.SearchIndexSyncEventModel;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;

/**
 * Central list of used eventbus addresses.
 */
public enum MeshEvent {

	PROJECT_VERSION_PURGE_START("mesh.project.version_purge.start",
		ProjectVersionPurgeEventModel.class,
		"Emitted once a version purge job starts",
		Examples::versionPurgeEvent),

	PROJECT_VERSION_PURGE_FINISHED("mesh.project.version_purge.finished",
		ProjectVersionPurgeEventModel.class,
		"Emitted once a version purge job finishes successully or failed",
		Examples::versionPurgeEvent),

	/**
	 * Schema migration start event.
	 */
	SCHEMA_MIGRATION_START("mesh.schema.migration.start",
		SchemaMigrationMeshEventModel.class,
		"Emitted once a schema migration starts.",
		Examples::schemaMigrationEvent),

	/**
	 * Schema migration finished event (contains status information)
	 */
	SCHEMA_MIGRATION_FINISHED("mesh.schema.migration.finished",
		SchemaMigrationMeshEventModel.class,
		"Emitted once the migration finishes successful or failed.",
		Examples::schemaMigrationEvent),

	/**
	 * Event which is send once the schema gets assigned to a branch.
	 */
	SCHEMA_BRANCH_ASSIGN("mesh.schema-branch.assign",
		BranchSchemaAssignEventModel.class,
		"Emitted once a schema has been assigned to a branch.",
		Examples::schemaBranchAssignEvent),

	/**
	 * Event which is send once the schema gets unassigned from a branch.
	 */
	SCHEMA_BRANCH_UNASSIGN("mesh.schema-branch.unassign",
		BranchSchemaAssignEventModel.class,
		"Emitted once a schema has been unassigned from a branch.",
		Examples::schemaBranchAssignEvent),

	/**
	 * Event which is send once the microschema gets assigned to a branch.
	 */
	MICROSCHEMA_BRANCH_ASSIGN("mesh.microschema-branch.assign",
		BranchMicroschemaAssignModel.class,
		"Emitted once a microschema gets assigned to a branch.",
		Examples::microschemaBranchAssignEvent),

	/**
	 * Event which is send once the microschema gets unassigned from a branch.
	 */
	MICROSCHEMA_BRANCH_UNASSIGN("mesh.microschema-branch.unassign",
		BranchMicroschemaAssignModel.class,
		"Emitted once a microschema gets unassigned from a branch.",
		Examples::microschemaBranchAssignEvent),

	/**
	 * Microschema migration start event.
	 */
	MICROSCHEMA_MIGRATION_START("mesh.microschema.migration.start",
		MicroschemaMigrationMeshEventModel.class,
		"Emitted when a microschema migration starts.",
		Examples::microschemaMigrationEvent),

	/**
	 * Microschema migration finished event.
	 */
	MICROSCHEMA_MIGRATION_FINISHED("mesh.microschema.migration.finished",
		MicroschemaMigrationMeshEventModel.class,
		"Emitted when a microschema migration finishes.",
		Examples::microschemaMigrationEvent),

	/**
	 * Branch migration start event.
	 */
	BRANCH_MIGRATION_START("mesh.branch.migration.start",
		BranchMigrationMeshEventModel.class,
		"Emitted when a branch migration job starts.",
		Examples::branchMigrationEvent),

	/**
	 * Branch migration finished event.
	 */
	BRANCH_MIGRATION_FINISHED("mesh.branch.migration.finished",
		BranchMigrationMeshEventModel.class,
		"Emitted when a branch migration job finishes.",
		Examples::branchMigrationEvent),

	/**
	 * Event which is send once the mesh instance is fully started and ready to accept requests.
	 */
	STARTUP("mesh.startup-complete",
		null,
		"Emitted once the Gentics Mesh instance is fully started and ready to accept requests."),

	/**
	 * Address for handler which will process registered job.
	 */
	JOB_WORKER_ADDRESS("job.worker",
		null,
		"Event which will trigger job processing."),

	/**
	 * Event which is send once a new node is joining the cluster.
	 */
	CLUSTER_NODE_JOINING("mesh.cluster.node.joining",
		null,
		"Emitted when a node joins the cluster."),

	/**
	 * Event which is send once a node finished joining the cluster.
	 */
	CLUSTER_NODE_JOINED("mesh.cluster.node.joined",
		null,
		"Emitted when a node joined the cluster."),

	/**
	 * Event which is send once a node is about to leave the cluster.
	 */
	CLUSTER_NODE_LEAVING("mesh.cluster.node.leaving",
		null,
		"Emitted when a node is leaving the cluster."),

	/**
	 * Event which is send once a node left the cluster.
	 */
	CLUSTER_NODE_LEFT("mesh.cluster.node.left",
		null,
		"Emitted when a cluster node left the cluster."),

	/**
	 * Event which is send once the database status (offline, online, not_available, backup, synchronizing) changes.
	 */
	CLUSTER_DATABASE_CHANGE_STATUS("mesh.cluster.db.status",
		null,
		"Emitted when the database status changes. (e.g. offline, online, backup, syncing)"),

	/**
	 * Event which is send to update the permission stores.
	 */
	CLEAR_PERMISSION_STORE("mesh.clear-permission-store",
		null,
		"Event which will clear the permission stores."),

	/**
	 * Event which is send to update the webroot path stores.
	 */
	CLEAR_PATH_STORE("mesh.clear-path-store",
		null,
		"Event which will clear the path stores."),

	/* User */

	USER_CREATED("mesh.user.created",
		MeshElementEventModelImpl.class,
		"Emitted when a user was created.",
		Examples::userEvent),

	USER_UPDATED("mesh.user.updated",
		MeshElementEventModelImpl.class,
		"Emitted when a user was updated.",
		Examples::userEvent),

	USER_DELETED("mesh.user.deleted",
		MeshElementEventModelImpl.class,
		"Emitted when a user was deleted.",
		Examples::userEvent),

	/* Group */

	GROUP_CREATED("mesh.group.created",
		MeshElementEventModelImpl.class,
		"Emitted when a group was created.",
		Examples::groupEvent),

	GROUP_UPDATED("mesh.group.updated",
		MeshElementEventModelImpl.class,
		"Emitted when a group was updated.",
		Examples::groupEvent),

	GROUP_DELETED("mesh.group.deleted",
		MeshElementEventModelImpl.class,
		"Emitted when a group was deleted.",
		Examples::groupEvent),

	GROUP_USER_ASSIGNED("mesh.group-user.assigned",
		GroupUserAssignModel.class,
		"Emitted when a user was assigned to a group.",
		Examples::groupUserAssignEvent),

	GROUP_USER_UNASSIGNED("mesh.group-user.unassigned",
		GroupUserAssignModel.class,
		"Emitted when a user was unassigned from a group.",
		Examples::groupUserAssignEvent),

	GROUP_ROLE_ASSIGNED("mesh.group-role.assigned",
		GroupRoleAssignModel.class,
		"Emitted when a role was assigned to a group.",
		Examples::groupRoleAssignEvent),

	GROUP_ROLE_UNASSIGNED("mesh.group-role.unassigned",
		GroupRoleAssignModel.class,
		"Emitted when a role was unassigned from a group.",
		Examples::groupRoleAssignEvent),

	/* Role */

	ROLE_CREATED("mesh.role.created",
		MeshElementEventModelImpl.class,
		"Emitted when a role was created.",
		Examples::roleEvent),

	ROLE_UPDATED("mesh.role.updated",
		MeshElementEventModelImpl.class,
		"Emitted when a role was updated.",
		Examples::roleEvent),

	ROLE_DELETED("mesh.role.deleted",
		MeshElementEventModelImpl.class,
		"Emitted when a role was deleted.",
		Examples::roleEvent),

	ROLE_PERMISSIONS_CHANGED("mesh.role.permissions.changed",
		PermissionChangedEventModel.class,
		"Emitted when the role permissions were changed.",
		Examples::rolePermissionChangedEvent),

	/* Tag */

	TAG_CREATED("mesh.tag.created",
		TagMeshEventModel.class,
		"Emitted when a tag was created.",
		Examples::tagEvent),

	TAG_UPDATED("mesh.tag.updated",
		TagMeshEventModel.class,
		"Emitted when a tag was updated.",
		Examples::tagEvent),

	TAG_DELETED("mesh.tag.deleted",
		TagMeshEventModel.class,
		"Emitted when a tag was deleted.",
		Examples::tagEvent),

	/* Tag Family */

	TAG_FAMILY_CREATED("mesh.tagfamily.created",
		TagFamilyMeshEventModel.class,
		"Emitted when a tag family was created.",
		Examples::tagFamilyEvent),

	TAG_FAMILY_UPDATED("mesh.tagfamily.updated",
		TagFamilyMeshEventModel.class,
		"Emitted when a tag family was updated.",
		Examples::tagFamilyEvent),

	TAG_FAMILY_DELETED("mesh.tagfamily.deleted",
		TagFamilyMeshEventModel.class,
		"Emitted when a tag family was deleted.",
		Examples::tagFamilyEvent),

	/* Project */

	PROJECT_CREATED("mesh.project.created",
		MeshElementEventModelImpl.class,
		"Emitted when a project was created.",
		Examples::projectEvent),

	PROJECT_UPDATED("mesh.project.updated",
		MeshElementEventModelImpl.class,
		"Emitted when a project was updated.",
		Examples::projectEvent),

	PROJECT_DELETED("mesh.project.deleted",
		MeshElementEventModelImpl.class,
		"Emitted when a project was deleted.",
		Examples::projectEvent),

	PROJECT_SCHEMA_ASSIGNED("mesh.project-schema.assigned",
		ProjectSchemaEventModel.class,
		"Emitted when a schema was assigned to a project."),

	PROJECT_SCHEMA_UNASSIGNED("mesh.project-schema.unassigned",
		ProjectSchemaEventModel.class,
		"Emitted when a schema was unassigned from a project."),

	PROJECT_MICROSCHEMA_ASSIGNED("mesh.project-microschema.assigned",
		ProjectMicroschemaEventModel.class,
		"Emitted when a microschema was assigned to a projec.t"),

	PROJECT_MICROSCHEMA_UNASSIGNED("mesh.project-microschema.unassigned",
		ProjectMicroschemaEventModel.class,
		"Emitted when a microschema was unassigned from a project."),

	PROJECT_LATEST_BRANCH_UPDATED("mesh.project-latest-branch.updated",
		ProjectBranchEventModel.class,
		"Emitted when the latest branch reference of a project was updated."),

	/* Node */

	NODE_CREATED("mesh.node.created",
		NodeMeshEventModel.class,
		"Emitted when a node was created.",
		Examples::nodeEvent),

	NODE_UPDATED("mesh.node.updated",
		NodeMeshEventModel.class,
		"Emitted when a node was updated.",
		Examples::nodeEvent),

	NODE_DELETED("mesh.node.deleted",
		NodeMeshEventModel.class,
		"Emitted when a node was deleted.",
		Examples::nodeEvent),

	NODE_TAGGED("mesh.node.tagged",
		NodeTaggedEventModel.class,
		"Emitted when a node was tagged.",
		Examples::nodeTaggedEvent),

	NODE_UNTAGGED("mesh.node.untagged",
		NodeTaggedEventModel.class,
		"Emitted when a node was untagged.",
		Examples::nodeTaggedEvent),

	NODE_PUBLISHED("mesh.node.published",
		NodeMeshEventModel.class,
		"Emitted whena a node or node content was published.",
		Examples::nodeContentEvent),

	NODE_UNPUBLISHED("mesh.node.unpublished",
		NodeMeshEventModel.class,
		"Emitted when a node or node content was unpublished.",
		Examples::nodeContentEvent),

	NODE_MOVED("mesh.node.moved",
		NodeMovedEventModel.class,
		"Emitted when a node was moved.",
		Examples::nodeMovedEvent),

	NODE_CONTENT_DELETED("mesh.node-content.deleted",
		NodeMeshEventModel.class,
		"Emitted when a content of a node was deleted. (e.g. English language was deleted)",
		Examples::nodeContentEvent),

	NODE_CONTENT_CREATED("mesh.node-content.created",
		NodeMeshEventModel.class,
		"Emitted when a content of a node was created. (e.g. English translation was added)",
		Examples::nodeContentEvent),

	NODE_REFERENCE_UPDATED("mesh.node-reference.updated",
		NodeMeshEventModel.class,
		"Emitted when a referencing node gets indirectly updated. (e.g. via deleting a node in the node list of the referenced node.)",
		Examples::nodeEvent),

	/* Schema */

	SCHEMA_CREATED("mesh.schema.created",
		MeshElementEventModelImpl.class,
		"Emitted when a schema was created.",
		Examples::schemaEvent),

	SCHEMA_UPDATED("mesh.schema.updated",
		MeshElementEventModelImpl.class,
		"Emitted when a schema was updated.",
		Examples::schemaEvent),

	SCHEMA_DELETED("mesh.schema.deleted",
		MeshElementEventModelImpl.class,
		"Emitted when a schema was deleted",
		Examples::schemaEvent),

	/* Microschema */

	MICROSCHEMA_CREATED("mesh.microschema.created",
		MeshElementEventModelImpl.class,
		"Emitted when a microschema was created.",
		Examples::microschemaEvent),

	MICROSCHEMA_UPDATED("mesh.microschema.updated",
		MeshElementEventModelImpl.class,
		"Emitted when a microschema was updated.",
		Examples::microschemaEvent),

	MICROSCHEMA_DELETED("mesh.microschema.deleted",
		MeshElementEventModelImpl.class,
		"Emitted when a microschema was deleted.",
		Examples::microschemaEvent),

	/* Branch */

	BRANCH_CREATED("mesh.branch.created",
		BranchMeshEventModel.class,
		"Emitted when a branch was created.",
		Examples::branchEvent),

	BRANCH_UPDATED("mesh.branch.updated",
		BranchMeshEventModel.class,
		"Emitted when a branch was updated.",
		Examples::branchEvent),

	BRANCH_DELETED("mesh.branch.deleted",
		BranchMeshEventModel.class,
		"Emitted when a branch was deleted.",
		Examples::branchEvent),

	BRANCH_TAGGED("mesh.branch.tagged",
		BranchTaggedEventModel.class,
		"Emitted when a branch was tagged.",
		Examples::branchTaggingEvent),

	BRANCH_UNTAGGED("mesh.branch.untagged",
		BranchTaggedEventModel.class,
		"Emitted when a branch was untagged.",
		Examples::branchTaggingEvent),

	/* Job */

	JOB_CREATED("mesh.jobn.created",
		JobEventModel.class,
		"Emitted when a job was created.",
		Examples::jobEvent),

	JOB_UPDATED("mesh.job.updated",
		JobEventModel.class,
		"Emitted when a job was updated.",
		Examples::jobEvent),

	JOB_DELETED("mesh.job.deleted",
		JobEventModel.class,
		"Emitted when a job was deleted.",
		Examples::jobEvent),

	/* Search index related (SYNC) */

	/**
	 * Address for the handler which will process index sync requests.
	 */
	INDEX_SYNC_REQUEST("mesh.search.index.sync.request",
		SearchIndexSyncEventModel.class,
		"Event address which can be used to trigger the sync process."),

	/**
	 * Emitted when an index sync process starts.
	 */
	INDEX_SYNC_START("mesh.search.index.sync.start",
		null,
		"Emitted when the index sync process starts."),

	/**
	 * Address to which index sync results will be published (failed, succeeded)
	 */
	INDEX_SYNC_FINISHED("mesh.search.index.sync.finished",
		null,
		"Emitted when the index sync process finishes."),

	/* Search index related (CLEAR) */

	/**
	 * Event which will trigger the index clear process.
	 */
	INDEX_CLEAR_REQUEST("mesh.search.index.clear.request",
		null,
		"Event address which will trigger a index clear."),

	/**
	 * Emitted when an index clear is starting.
	 */
	INDEX_CLEAR_START("mesh.search.index.clear.start",
		null,
		"Emitted when the index clear process starts."),

	/**
	 * Emitted when an index clear has finished.
	 */
	INDEX_CLEAR_FINISHED("mesh.search.index.clear.finished",
		null,
		"Emitted when the index clear process finishes."),

	/**
	 * Event address which will trigger an index check.
	 */
	INDEX_CHECK_REQUEST("mesh.search.index.check.request",
		null,
		"Event address which will trigger an index check."),

	/**
	 * Emitted when an index check process starts.
	 */
	INDEX_CHECK_START("mesh.search.index.check.start",
		null,
		"Emitted when the index check process starts."),

	/**
	 * Address to which index check results will be published (failed, succeeded)
	 */
	INDEX_CHECK_FINISHED("mesh.search.index.check.finished",
		null,
		"Emitted when the index check process finishes."),

	/**
	 * Event that is emitted when the search verticle has been working and is now idle.
	 */
	SEARCH_IDLE("mesh.search.process.idle",
		null,
		"Emitted when the search interation process has been working and is now in idle."),

	IS_SEARCH_IDLE("mesh.search.process.isidle",
		null,
		"When emitted, this event will be answered with the current idle status."),

	/**
	 * Event that will cause all pending Elasticsearch requests to be sent.
	 */
	SEARCH_FLUSH_REQUEST("mesh.search.flush.request",
		null,
		"Event which will cause all pending Elasticsearch requests to be sent."),

	/**
	 * Event that will cause all pending Elasticsearch requests to be sent.
	 */
	SEARCH_REFRESH_REQUEST("mesh.search.refresh.request",
		null,
		"Event which will cause all search indices to be refreshed, so that changes can be queried."),

	// Backup & Restore Events

	GRAPH_BACKUP_START("mesh.graph.backup.start",
		null, "Emitted once the backup process starts."),

	GRAPH_BACKUP_FINISHED("mesh.graph.backup.finished",
		null,
		"Emitted once the backup process finishes."),

	GRAPH_RESTORE_START("mesh.graph.restore.start",
		null,
		"Emitted once the restore process starts."),

	GRAPH_RESTORE_FINISHED("mesh.graph.restore.finished",
		null,
		"Emitted once the restore process finishes."),

	GRAPH_EXPORT_START("mesh.graph.export.start",
		null,
		"Emitted once the graph database export process starts."),

	GRAPH_EXPORT_FINISHED("mesh.graph.export.finished",
		null,
		"Emitted once the graph database export process finishes"),

	GRAPH_IMPORT_START("mesh.graph.import.start",
		null,
		"Emitted once the graph database import process starts."),

	GRAPH_IMPORT_FINISHED("mesh.graph.import.finished",
		null,
		"Emitted once the graph database import process finishes."),

	REPAIR_START("mesh.graph.repair.start",
		null,
		"Emitted once the repair operation is started."),

	REPAIR_FINISHED("mesh.graph.repair.finished",
		null,
		"Emitted once the repair operation finishes."),

	// Plugin Events

	PLUGIN_DEPLOYING("mesh.plugin.deploying",
		null,
		"Emitted once a plugin is being deployed."),

	PLUGIN_PRE_REGISTERED("mesh.plugin.pre-registered",
		null,
		"Emitted once a plugin has been pre-registered."),

	PLUGIN_REGISTERED("mesh.plugin.registered",
		null,
		"Emitted once a plugin has been registered."),

	PLUGIN_DEPLOYED("mesh.plugin.deployed",
		null,
		"Emitted once a plugin has been deployed."),

	PLUGIN_DEPLOY_FAILED("mesh.plugin.deploy.failed",
		null,
		"Emitted when a plugin deployment fails."),

	PLUGIN_UNDEPLOYING("mesh.plugin.undeploying",
		null,
		"Emitted once a plugin is being undeployed."),

	PLUGIN_UNDEPLOYED("mesh.plugin.undeployed",
		null,
		"Emitted once a plugin has been undeployed."),

	/* S3 Binary */

	S3BINARY_CREATED("mesh.s3binary.created",
			S3BinaryEventModel.class,
		"Emitted when a S3 binary field was created."),

	S3BINARY_DELETED("mesh.s3binary.deleted",
			S3BinaryEventModel.class,
		"Emitted when a S3 binary field gets deleted."),

	S3BINARY_METADATA_EXTRACTED("mesh.s3binary.metadata.extracted",
			S3BinaryEventModel.class,
		"Emitted when the metadata of a S3 binary field is extracted.");

	public final String address;
	public final Class<? extends MeshEventModel> bodyModel;
	public final String description;
	private final Supplier<? extends MeshEventModel> exampleGenerator;

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

	MeshEvent(String address, Class<? extends MeshEventModel> bodyModel, String description) {
		this(address, bodyModel, description, () -> null);
	}

	<R extends MeshEventModel> MeshEvent(String address, Class<R> bodyModel, String description, Supplier<R> exampleGenerator) {
		this.address = address;
		this.bodyModel = bodyModel;
		this.description = description;
		this.exampleGenerator = exampleGenerator;
	}

	/**
	 * Invoke the given runnable and wait for the event.
	 * 
	 * @param mesh
	 * @param event
	 * @param runnable
	 * @return
	 */
	public static Completable doAndWaitForEvent(Mesh mesh, MeshEvent event, Action runnable) {
		return doAndWaitForEvent(mesh.getVertx(), event, runnable);
	}

	/**
	 * Invoke the given runnable and wait for the event.
	 * 
	 * @param vertx
	 * @param event
	 * @param runnable
	 * @return
	 */
	public static Completable doAndWaitForEvent(Vertx vertx, MeshEvent event, Action runnable) {
		return Completable.create(sub -> {
			EventBus eventbus = vertx.eventBus();
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

	/**
	 * Async await for the given event.
	 * 
	 * @param mesh
	 * @param event
	 * @return
	 */
	public static Completable waitForEvent(Mesh mesh, MeshEvent event) {
		return doAndWaitForEvent(mesh, event, () -> {
		});
	}

	@Override
	public String toString() {
		return address;
	}

	/**
	 * Trigger the job processing event via the mesh server API. This is only possible in embedded mode or within plugins.
	 * 
	 * @param mesh
	 */
	public static void triggerJobWorker(Mesh mesh) {
		triggerJobWorker(mesh.getVertx().eventBus(), mesh.getOptions());
	}

	/**
	 * Trigger the job processing event via the Vert.x API. This is only possible in embedded mode or within plugins.
	 * 
	 * @param eb event bus
	 * @param options current Mesh options 
	 */
	public static void triggerJobWorker(EventBus eb, MeshOptions options) {
		eb.publish(JOB_WORKER_ADDRESS + options.getNodeName(), null);
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

	public String getDescription() {
		return description;
	}

	/**
	 * Return the example model for the event.
	 * 
	 * @return
	 */
	public MeshEventModel example() {
		return exampleGenerator.get();
	}
}
