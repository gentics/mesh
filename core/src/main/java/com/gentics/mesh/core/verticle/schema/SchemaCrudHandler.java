package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_INDEX;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.SchemaUpdateParameters;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.util.Tuple;

import dagger.Lazy;
import io.vertx.core.eventbus.DeliveryOptions;
import rx.Single;

public class SchemaCrudHandler extends AbstractCrudHandler<SchemaContainer, Schema> {

	private SchemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	private NodeIndexHandler nodeIndexHandler;

	@Inject
	public SchemaCrudHandler(Database db, SchemaComparator comparator, Lazy<BootstrapInitializer> boot, NodeIndexHandler nodeIndexHandler) {
		super(db);
		this.comparator = comparator;
		this.boot = boot;
		this.nodeIndexHandler = nodeIndexHandler;
	}

	@Override
	public RootVertex<SchemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.get().schemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		operateNoTx(ac, () -> {
			RootVertex<SchemaContainer> root = getRootVertex(ac);
			SchemaContainer schemaContainer = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModel.class);
			// Diff the schema with the latest version
			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(MeshInternal.get().schemaComparator().diff(schemaContainer.getLatestVersion().getSchema(), requestModel));
			String schemaName = schemaContainer.getName();
			// No changes -> done
			if (model.getChanges().isEmpty()) {
				return message(ac, "schema_update_no_difference_detected", schemaName);
			} else {

				List<DeliveryOptions> events = new ArrayList<>();
				db.tx(() -> {
					SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();
					// Apply the found changes to the schema
					SchemaContainerVersion createdVersion = schemaContainer.getLatestVersion().applyChanges(ac, model, batch);

					SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
					if (updateParams.getUpdateAssignedReleases()) {
						Map<Release, SchemaContainerVersion> referencedReleases = schemaContainer.findReferencedReleases();

						// Assign the created version to the found releases
						for (Map.Entry<Release, SchemaContainerVersion> releaseEntry : referencedReleases.entrySet()) {
							Release release = releaseEntry.getKey();
							Project projectOfRelease = release.getProject();

							// Check whether a list of release names was specified and skip releases which were not included in the list.
							List<String> releaseNames = updateParams.getReleaseNames();
							if (releaseNames != null && !releaseNames.isEmpty() && !releaseNames.contains(release.getName())) {
								continue;
							}

							SchemaContainerVersion previouslyReferencedVersion = releaseEntry.getValue();

							// Assign the new version to the release
							release.assignSchemaVersion(createdVersion);

							// Update the index type specific ES mapping
							nodeIndexHandler.updateNodeIndexMapping("node-" + projectOfRelease.getUuid() + "-" + release.getUuid() + "-draft",
									createdVersion.getSchema()).await();
							nodeIndexHandler.updateNodeIndexMapping("node-" + projectOfRelease.getUuid() + "-" + release.getUuid() + "-published",
									createdVersion.getSchema()).await();

							// Invoke the node release migration
							DeliveryOptions options = new DeliveryOptions();
							options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, release.getRoot().getProject().getUuid());
							options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, release.getUuid());
							options.addHeader(NodeMigrationVerticle.UUID_HEADER, createdVersion.getSchemaContainer().getUuid());
							options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, previouslyReferencedVersion.getUuid());
							options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, createdVersion.getUuid());
							events.add(options);

						}

					}

					return batch;
				}).processSync();

				for (DeliveryOptions option : events) {
					Mesh.vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, option);
				}

				return message(ac, "migration_invoked", schemaName);
			}
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a schema diff request.
	 * 
	 * @param ac
	 *            Context which contains the schema data to compare with
	 * @param uuid
	 *            Uuid of the schema which should also be used for comparison
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		operateNoTx(ac, () -> {
			SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaModel.class);
			return schema.getLatestVersion().diff(ac, comparator, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a read project list request.
	 * 
	 * @param ac
	 */
	public void handleReadProjectList(InternalActionContext ac) {
		HandlerUtilities.readElementList(ac, () -> ac.getProject().getSchemaContainerRoot());
	}

	/**
	 * Handle a add schema to project request.
	 * 
	 * @param ac
	 *            Context which provides the project reference
	 * @param schemaUuid
	 *            Uuid of the schema which should be added to the project
	 */
	public void handleAddSchemaToProject(InternalActionContext ac, String schemaUuid) {
		validateParameter(schemaUuid, "schemaUuid");

		operateNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (ac.getUser().hasPermission(project, GraphPermission.UPDATE_PERM)) {
				SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, schemaUuid, READ_PERM);
				Tuple<SearchQueueBatch, Single<Schema>> tuple = db.tx(() -> {

					project.getSchemaContainerRoot().addSchemaContainer(schema);
					SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();

					String releaseUuid = project.getLatestRelease().getUuid();
					SchemaContainerVersion schemaContainerVersion = schema.getLatestVersion();
					batch.addEntry(NodeIndexHandler.getIndexName(projectUuid, releaseUuid, schemaContainerVersion.getUuid(), DRAFT), Node.TYPE,
							CREATE_INDEX);
					batch.addEntry(NodeIndexHandler.getIndexName(projectUuid, releaseUuid, schemaContainerVersion.getUuid(), PUBLISHED), Node.TYPE,
							CREATE_INDEX);
					//					nodeIndexHandler.
					//					
					//					String draftIndexName = NodeIndexHandler.getIndexName(projectUuid, project.getLatestRelease().getUuid(), schema.getUuid(), DRAFT);
					//					nodeIndexHandler.updateNodeIndexMapping(draftIndexName, schema.getLatestVersion().getSchema()).await();
					//
					//					String publishIndexName = NodeIndexHandler.getIndexName(projectUuid, project.getLatestRelease().getUuid(), schema.getUuid(),
					//							PUBLISHED);
					//					nodeIndexHandler.updateNodeIndexMapping(publishIndexName, schema.getLatestVersion().getSchema()).await();

					return Tuple.tuple(batch, schema.transformToRest(ac, 0));
				});
				tuple.v1().processSync();
				return tuple.v2();
			} else {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}

		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle a remove schema from project request.
	 * 
	 * @param ac
	 * @param schemaUuid
	 *            Uuid of the schema which should be removed from the project.
	 */
	public void handleRemoveSchemaFromProject(InternalActionContext ac, String schemaUuid) {
		validateParameter(schemaUuid, "schemaUuid");

		operateNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (ac.getUser().hasPermission(project, GraphPermission.UPDATE_PERM)) {
				// TODO check whether schema is assigned to project

				SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, READ_PERM);
				return db.tx(() -> {
					project.getSchemaContainerRoot().removeSchemaContainer(schema);
					return Single.just(null);
				});
			} else {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}

		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}

	public void handleGetSchemaChanges(InternalActionContext ac) {
		// TODO Auto-generated method stub

	}

	/**
	 * Handle an apply changes to schema request.
	 * 
	 * @param ac
	 *            Context which contains the changes request data
	 * @param schemaUuid
	 *            Uuid of the schema which should be modified
	 */
	public void handleApplySchemaChanges(InternalActionContext ac, String schemaUuid) {
		validateParameter(schemaUuid, "schemaUuid");

		operateNoTx(ac, () -> {
			SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			db.tx(() -> {
				SearchQueueBatch batch = MeshInternal.get().boot().meshRoot().getSearchQueue().createBatch();
				schema.getLatestVersion().applyChanges(ac, batch);
				return batch;
			}).processSync();
			return message(ac, "migration_invoked", schema.getName());
		}, model -> ac.send(model, OK));

	}

	/**
	 * Helper handler which will handle requests for processing remaining not yet migrated nodes.
	 * 
	 * @param ac
	 */
	public void handleMigrateRemaining(InternalActionContext ac) {
		operateNoTx(ac, () -> {
			for (SchemaContainer schemaContainer : boot.get().schemaContainerRoot().findAll()) {
				SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();
				SchemaContainerVersion currentVersion = latestVersion;
				while (true) {
					currentVersion = currentVersion.getPreviousVersion();
					if (currentVersion == null) {
						break;
					}
					//TODO determine the releaseUuid
					String releaseUuid = null;
					System.out.println("Before migration " + schemaContainer.getName() + " - " + currentVersion.getUuid() + "="
							+ currentVersion.getFieldContainers(releaseUuid).size());
					//					if (!getLatestVersion().getUuid().equals(version.getUuid())) {
					//						for (GraphFieldContainer container : version.getFieldContainers()) {
					//							NodeImpl node = container.in(HAS_FIELD_CONTAINER).nextOrDefaultExplicit(NodeImpl.class, null);
					//							System.out.println(
					//									"Node: " + node.getUuid() + "ne: " + node.getLastEditedTimestamp() + "nc: " + node.getCreationTimestamp());
					//						}
					//					}
					CountDownLatch latch = new CountDownLatch(1);
					DeliveryOptions options = new DeliveryOptions();
					options.addHeader(NodeMigrationVerticle.UUID_HEADER, schemaContainer.getUuid());
					options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, currentVersion.getUuid());
					options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, latestVersion.getUuid());
					SchemaContainerVersion version = currentVersion;
					Mesh.vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, options, rh -> {
						try (NoTx noTrx = db.noTx()) {
							System.out.println("After migration " + schemaContainer.getName() + " - " + version.getUuid() + "="
									+ version.getFieldContainers(releaseUuid).size());
						}
						latch.countDown();
					});
					latch.await();
				}

			}
			return message(ac, "schema_migration_invoked");
		}, model -> ac.send(model, OK));
	}

}
