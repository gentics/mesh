package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.madl.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MicronodeMigrationJobImpl extends JobImpl {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationJobImpl.class);

	public static void init(LegacyDatabase database) {
		database.addVertexType(MicronodeMigrationJobImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void prepare() {
		// NOP
	}

	protected void processTask() {
		MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.microschema);
		try {

			try (Tx tx = DB.get().tx()) {
				Branch branch = getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + getUuid() + "} not found");
				}

				MicroschemaContainerVersion fromContainerVersion = getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + getUuid() + "} could not be found.");
				}

				MicroschemaContainerVersion toContainerVersion = getToMicroschemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + getUuid() + "} could not be found.");
				}

				MicroschemaContainer schemaContainer = fromContainerVersion.getSchemaContainer();
				BranchMicroschemaEdge branchVersionEdge = branch.findBranchMicroschemaEdge(toContainerVersion);
				statusHandler.setVersionEdge(branchVersionEdge);

				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + schemaContainer.getUuid() + "} from version {"
							+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} was requested");
				}

				statusHandler.commit();

				MeshInternal.get().micronodeMigrationHandler().migrateMicronodes(branch, fromContainerVersion, toContainerVersion, statusHandler)
						.blockingAwait();
				statusHandler.done();
			}
		} catch (Exception e) {
			statusHandler.error(e, "Error while preparing micronode migration.");
		}
	}
}
