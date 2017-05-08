package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.SchemaUpdateParameters;
import com.gentics.mesh.util.Tuple;

import dagger.Lazy;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Single;

public class SchemaCrudHandler extends AbstractCrudHandler<SchemaContainer, SchemaResponse> {

	private static final Logger log = LoggerFactory.getLogger(SchemaCrudHandler.class);

	private SchemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	private SearchQueue searchQueue;

	@Inject
	public SchemaCrudHandler(Database db, SchemaComparator comparator, Lazy<BootstrapInitializer> boot, SearchQueue searchQueue,
			HandlerUtilities utils) {
		super(db, utils);
		this.comparator = comparator;
		this.boot = boot;
		this.searchQueue = searchQueue;
	}

	@Override
	public RootVertex<SchemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.get().schemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.operateNoTx(ac, () -> {

			// 1. Load the schema container with update permissions
			RootVertex<SchemaContainer> root = getRootVertex(ac);
			SchemaContainer schemaContainer = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			SchemaUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);
			requestModel.validate();

			// 2. Diff the schema with the latest version
			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(MeshInternal.get().schemaComparator().diff(schemaContainer.getLatestVersion().getSchema(), requestModel));
			String schemaName = schemaContainer.getName();

			// No changes -> done
			if (model.getChanges().isEmpty()) {
				return message(ac, "schema_update_no_difference_detected", schemaName);
			}

			List<DeliveryOptions> events = new ArrayList<>();
			Completable searchBatchCompletable = db.tx(() -> {

				// Check whether there are any microschemas which are referenced by the schema
				for (FieldSchema field : requestModel.getFields()) {
					if (field instanceof MicronodeFieldSchema) {
						MicronodeFieldSchema microschemaField = (MicronodeFieldSchema) field;

						// Check each allowed microschema individually
						for (String microschemaName : microschemaField.getAllowedMicroSchemas()) {

							// schema_error_microschema_reference_no_perm
							MicroschemaContainer microschema = boot.get().microschemaContainerRoot().findByName(microschemaName);
							if (microschema == null) {
								throw error(BAD_REQUEST, "schema_error_microschema_reference_not_found", microschemaName, field.getName());
							}
							if (!ac.getUser().hasPermission(microschema, READ_PERM)) {
								throw error(BAD_REQUEST, "schema_error_microschema_reference_no_perm", microschemaName, field.getName());
							}

							// Locate the projects to which the schema was linked - We need to ensure that the microschema is also linked to those projects
							for (SchemaContainerRoot roots : schemaContainer.getRoots()) {
								Project project = roots.getProject();
								if (project != null) {
									project.getMicroschemaContainerRoot().addMicroschema(microschema);
								}
							}
						}
					}
				}

				events.clear();
				List<Completable> completables = new ArrayList<>();
				SearchQueueBatch batch = searchQueue.create();
				completables.add(batch.processAsync());

				// 3. Apply the found changes to the schema
				SchemaContainerVersion createdVersion = schemaContainer.getLatestVersion().applyChanges(ac, model, batch);

				// Check whether the assigned releases of the schema should also directly be updated.
				// This will trigger a node migration.
				SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
				if (updateParams.getUpdateAssignedReleases()) {
					Map<Release, SchemaContainerVersion> referencedReleases = schemaContainer.findReferencedReleases();

					// Assign the created version to the found releases
					for (Map.Entry<Release, SchemaContainerVersion> releaseEntry : referencedReleases.entrySet()) {
						Release release = releaseEntry.getKey();

						// Check whether a list of release names was specified and skip releases which were not included in the list.
						List<String> releaseNames = updateParams.getReleaseNames();
						if (releaseNames != null && !releaseNames.isEmpty() && !releaseNames.contains(release.getName())) {
							continue;
						}

						SchemaContainerVersion previouslyReferencedVersion = releaseEntry.getValue();

						// Assign the new version to the release
						release.assignSchemaVersion(createdVersion);

						if (log.isDebugEnabled()) {
							log.debug("Preparing node migration for release {" + releaseEntry.getKey().getUuid() + "}");
						}
						String projectUuid = release.getRoot().getProject().getUuid();
						String fromVersion = previouslyReferencedVersion.getUuid();
						String toVersion = createdVersion.getUuid();
						String releaseUuid = release.getUuid();
						SchemaModel newSchema = createdVersion.getSchema();
						String schemaUuid = createdVersion.getSchemaContainer().getUuid();
						if (log.isDebugEnabled()) {
							log.debug("Migrating nodes from schema version {" + fromVersion + "} to {" + toVersion + "} for schema with uuid {"
									+ schemaUuid + "}");
						}

						// The node migration needs to write into a new index. Lets prepare the creation of that index
						SearchQueueBatch indexCreatingBatch = searchQueue.create();
						indexCreatingBatch.createNodeIndex(projectUuid, releaseUuid, toVersion, DRAFT, newSchema);
						indexCreatingBatch.createNodeIndex(projectUuid, releaseUuid, toVersion, PUBLISHED, newSchema);
						completables.add(indexCreatingBatch.processAsync());

						// Lets also prepare the invocation of the migration. The migration is delegated to a worker verticle.
						DeliveryOptions options = new DeliveryOptions();
						options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, projectUuid);
						options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, releaseUuid);
						options.addHeader(NodeMigrationVerticle.UUID_HEADER, schemaUuid);
						options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, fromVersion);
						options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, toVersion);
						events.add(options);
					}
				}
				return Completable.merge(completables);
			});

			searchBatchCompletable.await();

			// Invoke the node release migration
			for (DeliveryOptions option : events) {
				Mesh.vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, option);
			}

			return message(ac, "migration_invoked", schemaName);

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
		validateParameter(uuid, "uuid");

		utils.operateNoTx(ac, () -> {
			SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Schema requestModel = JsonUtil.readValue(ac.getBodyAsString(), SchemaUpdateRequest.class);
			requestModel.validate();
			return schema.getLatestVersion().diff(ac, comparator, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a read project list request.
	 * 
	 * @param ac
	 */
	public void handleReadProjectList(InternalActionContext ac) {
		utils.readElementList(ac, () -> ac.getProject().getSchemaContainerRoot());
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

		db.operateNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (ac.getUser().hasPermission(project, GraphPermission.UPDATE_PERM)) {
				SchemaContainer schema = getRootVertex(ac).loadObjectByUuid(ac, schemaUuid, READ_PERM);
				Tuple<SearchQueueBatch, Single<SchemaResponse>> tuple = db.tx(() -> {

					// Assign the schema to the project
					project.getSchemaContainerRoot().addSchemaContainer(schema);

//					// Check whether there are any microschemas which are referenced by the schema
//					for (FieldSchema field : schema.getLatestVersion().getSchema().getFields()) {
//						if (field instanceof MicronodeFieldSchema) {
//							MicronodeFieldSchema microschemaField = (MicronodeFieldSchema) field;
//							for (String microschemaName : microschemaField.getAllowedMicroSchemas()) {
//								// schema_error_microschema_reference_no_perm
//								MicroschemaContainer microschema = ac.getProject().getMicroschemaContainerRoot().findByName(microschemaName);
//								if (microschema == null) {
//									throw error(BAD_REQUEST, "schema_error_microschema_reference_not_found", microschemaName, field.getName());
//								}
//								if (ac.getUser().hasPermission(microschema, READ_PERM)) {
//									throw error(BAD_REQUEST, "schema_error_microschema_reference_no_perm", microschemaName, field.getName());
//								}
//								project.getMicroschemaContainerRoot().addMicroschema(microschema);
//							}
//						}
//					}

					SearchQueueBatch batch = searchQueue.create();

					String releaseUuid = project.getLatestRelease().getUuid();
					SchemaContainerVersion schemaContainerVersion = schema.getLatestVersion();
					batch.createNodeIndex(projectUuid, releaseUuid, schemaContainerVersion.getUuid(), DRAFT, schemaContainerVersion.getSchema());
					batch.createNodeIndex(projectUuid, releaseUuid, schemaContainerVersion.getUuid(), PUBLISHED, schemaContainerVersion.getSchema());
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

		db.operateNoTx(() -> {
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

		utils.operateNoTx(ac, () -> {
			SchemaContainer schema = boot.get().schemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			db.tx(() -> {
				SearchQueueBatch batch = searchQueue.create();
				schema.getLatestVersion().applyChanges(ac, batch);
				return batch;
			}).processSync();
			return message(ac, "migration_invoked", schema.getName());
		}, model -> ac.send(model, OK));

	}

}
