package com.gentics.mesh.core.verticle.release;

import static com.gentics.mesh.Events.JOB_WORKER_ADDRESS;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Iterator;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.info.ReleaseInfoMicroschemaList;
import com.gentics.mesh.core.rest.release.info.ReleaseInfoSchemaList;
import com.gentics.mesh.core.rest.release.info.ReleaseMicroschemaInfo;
import com.gentics.mesh.core.rest.release.info.ReleaseSchemaInfo;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.ResultInfo;
import com.gentics.mesh.util.Tuple;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * CRUD Handler for Releases
 */
public class ReleaseCrudHandler extends AbstractCrudHandler<Release, ReleaseResponse> {

	private static final Logger log = LoggerFactory.getLogger(ReleaseCrudHandler.class);

	private SearchQueue searchQueue;

	private BootstrapInitializer boot;

	@Inject
	public ReleaseCrudHandler(Database db, SearchQueue searchQueue, HandlerUtilities utils, BootstrapInitializer boot) {
		super(db, utils);
		this.searchQueue = searchQueue;
		this.boot = boot;
	}

	@Override
	public RootVertex<Release> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getReleaseRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		throw new NotImplementedException("Release can't be deleted");
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		utils.operateTx(ac, (tx) -> {
			Database db = MeshInternal.get().database();
			User user = ac.getUser();

			ResultInfo info = db.tx(() -> {
				SearchQueueBatch batch = searchQueue.create();
				RootVertex<Release> root = getRootVertex(ac);
				Release created = root.create(ac, batch);
				Project project = created.getProject();
				ReleaseResponse model = created.transformToRestSync(ac, 0);
				ResultInfo resultInfo = new ResultInfo(model, batch);
				resultInfo.setProperty("path", created.getAPIPath(ac));
				resultInfo.setProperty("projectUuid", project.getUuid());
				resultInfo.setProperty("releaseUuid", created.getUuid());
				JobRoot jobRoot = boot.jobRoot();
				jobRoot.enqueueReleaseMigration(user, created);
				return resultInfo;
			});

			// The release has been created now lets start the release migration (specific node migration)
			vertx.eventBus().send(JOB_WORKER_ADDRESS, null);

			ac.setLocation(info.getProperty("path"));
			// Finally process the batch
			info.getBatch().processSync();
			return info.getModel();
		}, model -> ac.send(model, CREATED));

	}

	/**
	 * Handle getting the schema versions of a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release to be queried
	 */
	public void handleGetSchemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.operateTx(() -> {
			Release release = getRootVertex(ac).loadObjectByUuid(ac, uuid, READ_PERM);
			return getSchemaVersionsInfo(release);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of schema version to a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release
	 */
	public void handleAssignSchemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.operateTx(() -> {
			RootVertex<Release> root = getRootVertex(ac);
			Release release = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			ReleaseInfoSchemaList schemaReferenceList = ac.fromJson(ReleaseInfoSchemaList.class);
			Project project = ac.getProject();
			SchemaContainerRoot schemaContainerRoot = project.getSchemaContainerRoot();

			Tuple<Single<ReleaseInfoSchemaList>, SearchQueueBatch> tuple = db.tx(() -> {
				SearchQueueBatch batch = searchQueue.create();

				// Resolve the list of references to graph schema container versions
				for (SchemaReference reference : schemaReferenceList.getSchemas()) {
					SchemaContainerVersion version = schemaContainerRoot.fromReference(reference);
					SchemaContainerVersion assignedVersion = release.findLatestSchemaVersion(version.getSchemaContainer());
					if (assignedVersion != null && Double.valueOf(assignedVersion.getVersion()) > Double.valueOf(version.getVersion())) {
						throw error(BAD_REQUEST, "release_error_downgrade_schema_version", version.getName(), assignedVersion.getVersion(),
								version.getVersion());
					}
					release.assignSchemaVersion(ac.getUser(), version);
				}

				return Tuple.tuple(getSchemaVersionsInfo(release), batch);
			});

			// 1. Process batch and create need indices
			tuple.v2().processSync();

			// 2. Invoke migrations which will populate the created index
			Mesh.vertx().eventBus().send(JOB_WORKER_ADDRESS, null);

			return tuple.v1();

		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle getting the microschema versions of a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release to be queried
	 */
	public void handleGetMicroschemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.operateTx(() -> {
			Release release = getRootVertex(ac).loadObjectByUuid(ac, uuid, GraphPermission.READ_PERM);
			return getMicroschemaVersions(release);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of microschema version to a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release
	 */
	public void handleAssignMicroschemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.operateTx(() -> {
			RootVertex<Release> root = getRootVertex(ac);
			Release release = root.loadObjectByUuid(ac, uuid, UPDATE_PERM);
			ReleaseInfoMicroschemaList microschemaReferenceList = ac.fromJson(ReleaseInfoMicroschemaList.class);
			MicroschemaContainerRoot microschemaContainerRoot = ac.getProject().getMicroschemaContainerRoot();

			User user = ac.getUser();
			Single<ReleaseInfoMicroschemaList> model = db.tx(() -> {
				// Transform the list of references into microschema container version vertices
				for (MicroschemaReference reference : microschemaReferenceList.getMicroschemas()) {
					MicroschemaContainerVersion version = microschemaContainerRoot.fromReference(reference);

					MicroschemaContainerVersion assignedVersion = release.findLatestMicroschemaVersion(version.getSchemaContainer());
					if (assignedVersion != null && Double.valueOf(assignedVersion.getVersion()) > Double.valueOf(version.getVersion())) {
						throw error(BAD_REQUEST, "release_error_downgrade_microschema_version", version.getName(), assignedVersion.getVersion(),
								version.getVersion());
					}
					release.assignMicroschemaVersion(user, version);
				}
				return getMicroschemaVersions(release);
			});

			vertx.eventBus().send(JOB_WORKER_ADDRESS, null);
			return model;

		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Get the REST model of the schema versions of the release.
	 * 
	 * @param release
	 *            release
	 * @return single emitting the rest model
	 */
	protected Single<ReleaseInfoSchemaList> getSchemaVersionsInfo(Release release) {
		return Observable.fromIterable(release.findAllLatestSchemaVersionEdges()).map(edge -> {
			SchemaReference reference = edge.getSchemaContainerVersion().transformToReference();
			ReleaseSchemaInfo info = new ReleaseSchemaInfo(reference);
			info.setMigrationStatus(edge.getMigrationStatus());
			info.setJobUuid(edge.getJobUuid());
			return info;
		}).collect(() -> {
			return new ReleaseInfoSchemaList();
		}, (x, y) -> {
			x.getSchemas().add(y);
		});
	}

	/**
	 * Get the REST model of the microschema versions of the release.
	 * 
	 * @param release
	 *            release
	 * @return single emitting the rest model
	 */
	protected Single<ReleaseInfoMicroschemaList> getMicroschemaVersions(Release release) {
		return Observable.fromIterable(release.findAllLatestMicroschemaVersionEdges()).map(edge -> {
			MicroschemaReference reference = edge.getMicroschemaContainerVersion().transformToReference();
			ReleaseMicroschemaInfo info = new ReleaseMicroschemaInfo(reference);
			info.setMigrationStatus(edge.getMigrationStatus());
			info.setJobUuid(edge.getJobUuid());
			return info;
		}).collect(() -> {
			return new ReleaseInfoMicroschemaList();
		}, (x, y) -> {
			x.getMicroschemas().add(y);
		});
	}

	public void handleMigrateRemainingMicronodes(InternalActionContext ac, String releaseUuid) {
		utils.operateTx(ac, () -> {
			Project project = ac.getProject();
			JobRoot jobRoot = boot.jobRoot();
			User user = ac.getUser();
			Release release = project.getReleaseRoot().findByUuid(releaseUuid);
			for (MicroschemaContainer microschemaContainer : boot.microschemaContainerRoot().findAllIt()) {
				MicroschemaContainerVersion latestVersion = microschemaContainer.getLatestVersion();
				MicroschemaContainerVersion currentVersion = latestVersion;
				while (true) {
					currentVersion = currentVersion.getPreviousVersion();
					if (currentVersion == null) {
						break;
					}

					Job job = jobRoot.enqueueMicroschemaMigration(user, release, currentVersion, latestVersion);
					job.process();

					try (Tx tx = db.tx()) {
						Iterator<? extends NodeGraphFieldContainer> it = currentVersion.getDraftFieldContainers(release.getUuid());
						log.info("After migration " + microschemaContainer.getName() + ":" + currentVersion.getVersion() + " - "
								+ currentVersion.getUuid() + "=" + it.hasNext());
					}
				}

			}
			return message(ac, "schema_migration_invoked");
		}, model -> ac.send(model, OK));

	}

	/**
	 * Helper handler which will handle requests for processing remaining not yet migrated nodes.
	 *
	 * @param ac
	 * @param releaseUuid
	 */
	public void handleMigrateRemainingNodes(InternalActionContext ac, String releaseUuid) {

		utils.operateTx(ac, () -> {
			JobRoot jobRoot = boot.jobRoot();
			User user = ac.getUser();
			Release release = ac.getProject().getReleaseRoot().findByUuid(releaseUuid);
			for (SchemaContainer schemaContainer : boot.schemaContainerRoot().findAllIt()) {
				SchemaContainerVersion latestVersion = schemaContainer.getLatestVersion();
				SchemaContainerVersion currentVersion = latestVersion;
				while (true) {
					currentVersion = currentVersion.getPreviousVersion();
					if (currentVersion == null) {
						break;
					}
					Job job = jobRoot.enqueueSchemaMigration(user, release, currentVersion, latestVersion);
					try {
						job.process();
						Iterator<NodeGraphFieldContainer> it = currentVersion.getFieldContainers(release.getUuid());
						log.info("After migration " + schemaContainer.getName() + ":" + currentVersion.getVersion() + " - " + currentVersion.getUuid()
								+ " has unmigrated containers: " + it.hasNext());
					} catch (Exception e) {
						log.error("Migration failed of " + schemaContainer.getName() + ":" + currentVersion.getVersion() + " - "
								+ currentVersion.getUuid() + " failed with error", e);
					}

				}

			}

			return message(ac, "schema_migration_executed");
		}, model -> ac.send(model, OK));
	}

}
