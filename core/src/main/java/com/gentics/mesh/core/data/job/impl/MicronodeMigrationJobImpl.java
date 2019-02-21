package com.gentics.mesh.core.data.job.impl;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

public class MicronodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationJobImpl.class);

	public static void init(Database database) {
		database.addVertexType(MicronodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void prepare() {
		EventQueueBatch batch = EventQueueBatch.create();
		MicroschemaMigrationMeshEventModel event = new MicroschemaMigrationMeshEventModel();
		event.setEvent(MICROSCHEMA_MIGRATION_START);

		MicroschemaContainerVersion toVersion = getToMicroschemaVersion();
		event.setToVersion(toVersion.transformToReference());

		MicroschemaContainerVersion fromVersion = getFromMicroschemaVersion();
		event.setFromVersion(fromVersion.transformToReference());

		Branch branch = getBranch();
		Project project = branch.getProject();
		event.setProject(project.transformToReference());
		event.setBranch(branch.transformToReference());
		batch.add(event).dispatch();
	}

	protected Completable processTask() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.microschema);
		return Completable.defer(() -> {

			Branch branch = null;
			MicroschemaContainerVersion fromContainerVersion = null;
			MicroschemaContainerVersion toContainerVersion = null;

			try (Tx tx = DB.get().tx()) {
				branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}

				fromContainerVersion = getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + getUuid() + "} could not be found.");
				}

				toContainerVersion = getToMicroschemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + getUuid() + "} could not be found.");
				}

				MicroschemaContainer schemaContainer = fromContainerVersion.getSchemaContainer();
				BranchMicroschemaEdge branchVersionEdge = branch.findBranchMicroschemaEdge(toContainerVersion);
				status.setVersionEdge(branchVersionEdge);

				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} was requested");
				}

				status.commit();
				tx.success();
			} catch (Exception e) {
				DB.get().tx(() -> {
					status.error(e, "Error while preparing micronode migration.");
				});
				throw e;
			}

			return MeshInternal.get().micronodeMigrationHandler().migrateMicronodes(branch, fromContainerVersion, toContainerVersion,
				status).doOnComplete(() -> {
				DB.get().tx(() -> {
					JobWarningList warnings = new JobWarningList();
					setWarnings(warnings);
					status.done();
				});
			}).doOnError(err -> {
				DB.get().tx(() -> {
					status.error(err, "Error in micronode migration.");
				});
			});
		});
	}

}
