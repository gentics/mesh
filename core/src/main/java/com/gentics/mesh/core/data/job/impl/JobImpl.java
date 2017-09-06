package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.ETag;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

public class JobImpl extends AbstractMeshCoreVertex<JobResponse, Job> implements Job {

	private static final Logger log = LoggerFactory.getLogger(JobImpl.class);

	public static void init(Database database) {
		database.addVertexType(JobImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Job update(InternalActionContext ac, SearchQueueBatch batch) {
		throw new NotImplementedException("Jobs can't be updated");
	}

	@Override
	public TypeInfo getTypeInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Single<JobResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid());
	}

	@Override
	public void setType(MigrationType type) {
		setProperty(TYPE_PROPERTY_KEY, type.name());
	}

	@Override
	public MigrationType getType() {
		String type = getProperty(TYPE_PROPERTY_KEY);
		if (type == null) {
			return null;
		} else {
			return MigrationType.valueOf(type);
		}
	}

	@Override
	public Release getRelease() {
		return out(HAS_RELEASE).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public void setRelease(Release release) {
		setUniqueLinkOutTo(release, HAS_RELEASE);
	}

	@Override
	public SchemaContainerVersion getFromSchemaVersion() {
		return out(HAS_FROM_VERSION).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public void setFromSchemaVersion(SchemaContainerVersion version) {
		setUniqueLinkOutTo(version, HAS_FROM_VERSION);
	}

	@Override
	public SchemaContainerVersion getToSchemaVersion() {
		return out(HAS_TO_VERSION).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToSchemaVersion(SchemaContainerVersion version) {
		setUniqueLinkOutTo(version, HAS_TO_VERSION);
	}

	@Override
	public MicroschemaContainerVersion getFromMicroschemaVersion() {
		return out(HAS_FROM_VERSION).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setFromMicroschemaVersion(MicroschemaContainerVersion fromVersion) {
		setUniqueLinkOutTo(fromVersion, HAS_FROM_VERSION);
	}

	@Override
	public MicroschemaContainerVersion getToMicroschemaContainerVersion() {
		return out(HAS_TO_VERSION).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToMicroschemaVersion(MicroschemaContainerVersion toVersion) {
		setUniqueLinkOutTo(toVersion, HAS_TO_VERSION);
	}

	@Override
	public void process() {
		switch (getType()) {
		case schema:
			handleNodeMigration();
			break;
		case microschema:
			handleMicroschemaMigration();
			break;
		case release:
			handleReleaseMigration();
			break;
		default:
			throw error(INTERNAL_SERVER_ERROR, "Unknown job type {" + getType() + "}");
		}

	}

	public void handleNodeMigration() {
		MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(Mesh.vertx(), MigrationType.schema);
		try {

			db.tx(() -> {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} not found");
				}
				SchemaContainerVersion fromContainerVersion = getFromSchemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source schema version for job {" + getUuid() + "} could not be found.");
				}
				SchemaContainerVersion toContainerVersion = getToSchemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target schema version for job {" + getUuid() + "} could not be found.");
				}

				SchemaContainer schemaContainer = toContainerVersion.getSchemaContainer();
				if (schemaContainer == null) {
					throw error(BAD_REQUEST, "Schema container for job {" + getUuid() + "} can't be found.");
				}
				Project project = release.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
				}

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + release.getUuid()
						+ "} in project {" + project.getUuid() + "}");

				statusHandler.getInfo().setSourceName(schemaContainer.getName());
				statusHandler.getInfo().setSourceUuid(schemaContainer.getUuid());
				statusHandler.getInfo().setSourceVersion(fromContainerVersion.getVersion());
				statusHandler.getInfo().setTargetVersion(toContainerVersion.getVersion());
				statusHandler.updateStatus();

				// Acquire the global lock and invoke the migration
				db.tx(() -> {
					MeshInternal.get().nodeMigrationHandler().migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusHandler)
							.await();
				});
				statusHandler.done();
			});
		} catch (Exception e) {
			statusHandler.error(e, "Error while preparing node migration.");
		}
	}

	public void handleReleaseMigration() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(Mesh.vertx(), MigrationType.release);
		try {

			if (log.isDebugEnabled()) {
				log.debug("Release migration for job {" + getUuid() + "} was requested");
			}

			db.tx(() -> {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} cannot be found.");
				}
				Project project = release.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} cannot be found.");
				}

				db.tx(() -> {
					MeshInternal.get().releaseMigrationHandler().migrateRelease(release).await();
				});
				status.done();
			});
		} catch (Exception e) {
			status.error(e, "Error while preparing release migration.");
		}
	}

	public void handleMicroschemaMigration() {
		MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(Mesh.vertx(), MigrationType.microschema);
		try {

			db.tx(() -> {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} not found");
				}
				MicroschemaContainerVersion fromContainerVersion = getFromMicroschemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source version of microschema for job {" + getUuid() + "} could not be found.");
				}
				MicroschemaContainerVersion toContainerVersion = getToMicroschemaContainerVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target version of microschema for job {" + getUuid() + "} could not be found.");
				}
				MicroschemaContainer schemaContainer = fromContainerVersion.getSchemaContainer();
				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + schemaContainer.getUuid() + "} from version {"
							+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} was requested");
				}
				Project project = release.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + getUuid() + "} not found");
				}

				db.tx(() -> {
					MeshInternal.get().micronodeMigrationHandler()
							.migrateMicronodes(project, release, fromContainerVersion, toContainerVersion, statusHandler).await();
				});
				statusHandler.done();
			});
		} catch (Exception e) {
			statusHandler.error(e, "Error while preparing micronode migration.");
		}
	}

}
