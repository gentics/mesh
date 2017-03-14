package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.SchemaUpdateParameters;

import dagger.Lazy;
import io.vertx.core.eventbus.DeliveryOptions;
import rx.Single;

public class MicroschemaCrudHandler extends AbstractCrudHandler<MicroschemaContainer, MicroschemaResponse> {

	private MicroschemaComparator comparator;

	private Lazy<BootstrapInitializer> boot;

	private SearchQueue searchQueue;

	@Inject
	public MicroschemaCrudHandler(Database db, MicroschemaComparator comparator, Lazy<BootstrapInitializer> boot, SearchQueue searchQueue,
			HandlerUtilities utils) {
		super(db, utils);
		this.comparator = comparator;
		this.boot = boot;
		this.searchQueue = searchQueue;
	}

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex(InternalActionContext ac) {
		return boot.get().microschemaContainerRoot();
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.operateNoTx(ac, () -> {
			RootVertex<MicroschemaContainer> root = getRootVertex(ac);
			MicroschemaContainer schemaContainer = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
			requestModel.validate();

			SchemaChangesListModel model = new SchemaChangesListModel();
			model.getChanges().addAll(comparator.diff(schemaContainer.getLatestVersion().getSchema(), requestModel));
			String name = schemaContainer.getName();

			if (model.getChanges().isEmpty()) {
				return message(ac, "schema_update_no_difference_detected", name);
			}
			db.tx(() -> {
				SearchQueueBatch batch = searchQueue.create();
				MicroschemaContainerVersion createdVersion = schemaContainer.getLatestVersion().applyChanges(ac, model, batch);

				SchemaUpdateParameters updateParams = ac.getSchemaUpdateParameters();
				if (updateParams.getUpdateAssignedReleases()) {
					Map<Release, MicroschemaContainerVersion> referencedReleases = schemaContainer.findReferencedReleases();

					// Assign the created version to the found releases
					for (Map.Entry<Release, MicroschemaContainerVersion> releaseEntry : referencedReleases.entrySet()) {
						Release release = releaseEntry.getKey();
						Project projectOfRelease = release.getProject();

						// Check whether a list of release names was specified and skip releases which were not included in the list.
						List<String> releaseNames = updateParams.getReleaseNames();
						if (releaseNames != null && !releaseNames.isEmpty() && !releaseNames.contains(release.getName())) {
							continue;
						}

						MicroschemaContainerVersion previouslyReferencedVersion = releaseEntry.getValue();

						// Assign the new version to the release
						release.assignMicroschemaVersion(createdVersion);

						// start microschema migration
						DeliveryOptions options = new DeliveryOptions();
						options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, projectOfRelease.getUuid());
						options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, release.getUuid());
						options.addHeader(NodeMigrationVerticle.UUID_HEADER, createdVersion.getSchemaContainer().getUuid());
						options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, previouslyReferencedVersion.getUuid());
						options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, createdVersion.getUuid());
						Mesh.vertx().eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options);

					}
				}

				return batch;
			}).processSync();
			return message(ac, "migration_invoked", name);

		}, model -> ac.send(model, OK));

	}

	/**
	 * Compare the latest schema version with the given schema model.
	 * 
	 * @param ac
	 * @param uuid
	 *            Schema uuid
	 */
	public void handleDiff(InternalActionContext ac, String uuid) {
		utils.operateNoTx(ac, () -> {
			MicroschemaContainer microschema = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			Microschema requestModel = JsonUtil.readValue(ac.getBodyAsString(), MicroschemaModel.class);
			requestModel.validate();
			return microschema.getLatestVersion().diff(ac, comparator, requestModel);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a schema apply changes request.
	 * 
	 * @param ac
	 *            Context which contains the changes data
	 * @param schemaUuid
	 *            Schema which should be modified
	 */
	public void handleApplySchemaChanges(InternalActionContext ac, String schemaUuid) {
		utils.operateNoTx(ac, () -> {
			MicroschemaContainer schema = boot.get().microschemaContainerRoot().loadObjectByUuid(ac, schemaUuid, UPDATE_PERM);
			SearchQueueBatch batch = searchQueue.create();
			db.tx(() -> {
				schema.getLatestVersion().applyChanges(ac, batch);
				return null;
			});
			batch.processSync();
			return message(ac, "migration_invoked", schema.getName());
		}, model -> ac.send(model, OK));

	}

	/**
	 * Handle a microschema read list request.
	 * 
	 * @param ac
	 */
	public void handleReadMicroschemaList(InternalActionContext ac) {
		utils.readElementList(ac, () -> ac.getProject().getMicroschemaContainerRoot());
	}

	/**
	 * Handle a request which will add a microschema to a project.
	 * 
	 * @param ac
	 *            Internal Action Context which also contains the project to which the microschema will be added.
	 * @param microschemaUuid
	 *            Microschema uuid which should be added to the project.
	 */
	public void handleAddMicroschemaToProject(InternalActionContext ac, String microschemaUuid) {
		validateParameter(microschemaUuid, "microschemaUuid");

		db.operateNoTx(() -> {
			Project project = ac.getProject();
			if (!ac.getUser().hasPermission(project, UPDATE_PERM)) {
				String projectUuid = project.getUuid();
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}
			MicroschemaContainer microschema = getRootVertex(ac).loadObjectByUuid(ac, microschemaUuid, READ_PERM);
			return db.tx(() -> {
				project.getMicroschemaContainerRoot().addMicroschema(microschema);
				return microschema.transformToRest(ac, 0);
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	public void handleRemoveMicroschemaFromProject(InternalActionContext ac, String microschemaUuid) {
		validateParameter(microschemaUuid, "microschemaUuid");

		db.operateNoTx(() -> {
			Project project = ac.getProject();
			String projectUuid = project.getUuid();
			if (!ac.getUser().hasPermission(project, UPDATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", projectUuid);
			}
			// TODO check whether microschema is assigned to project
			MicroschemaContainer microschema = getRootVertex(ac).loadObjectByUuid(ac, microschemaUuid, READ_PERM);
			return db.tx(() -> {
				project.getMicroschemaContainerRoot().removeMicroschema(microschema);
				return Single.just(null);
			});
		}).subscribe(model -> ac.send(NO_CONTENT), ac::fail);
	}
}
