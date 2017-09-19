package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.FAILED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.STARTING;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.UNKNOWN;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.release.ReleaseMicroschemaEdge;
import com.gentics.mesh.core.data.release.ReleaseSchemaEdge;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
		return null;
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	public JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = new JobResponse();
		response.setUuid(getUuid());

		User creator = getCreator();
		if (creator != null) {
			response.setCreator(creator.transformToReference());
		} else {
			log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no creator. Omitting creator field");
		}

		String date = getCreationDate();
		response.setCreated(date);
		response.setErrorMessage(getErrorMessage());
		response.setErrorDetail(getErrorDetail());
		response.setType(getType());
		response.setStatus(getStatus());
		response.setStopDate(getStopDate());
		response.setStartDate(getStartDate());
		response.setCompletionCount(getCompletionCount());
		response.setNodeName(getNodeName());

		Map<String, String> props = response.getProperties();
		props.put("releaseName", getRelease().getName());
		props.put("releaseUuid", getRelease().getUuid());

		if (getToSchemaVersion() != null) {
			SchemaContainer container = getToSchemaVersion().getSchemaContainer();
			props.put("schemaName", container.getName());
			props.put("schemaUuid", container.getUuid());
			props.put("fromVersion", getFromSchemaVersion().getVersion());
			props.put("toVersion", getToSchemaVersion().getVersion());
		}

		if (getToMicroschemaVersion() != null) {
			MicroschemaContainer container = getToMicroschemaVersion().getSchemaContainer();
			props.put("microschemaName", container.getName());
			props.put("microschemaUuid", container.getUuid());
			props.put("fromVersion", getFromMicroschemaVersion().getVersion());
			props.put("toVersion", getToMicroschemaVersion().getVersion());
		}
		return response;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid() + getErrorMessage() + getErrorDetail());
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
	public Long getStartTimestamp() {
		return getProperty(START_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setStartTimestamp(Long date) {
		setProperty(START_TIMESTAMP_PROPERTY_KEY, date);
	}

	@Override
	public Long getStopTimestamp() {
		return getProperty(STOP_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setStopTimestamp(Long date) {
		setProperty(STOP_TIMESTAMP_PROPERTY_KEY, date);
	}

	@Override
	public long getCompletionCount() {
		Long value = getProperty(COMPLETION_COUNT_PROPERTY_KEY);
		return value == null ? 0 : value;
	}

	@Override
	public void setCompletionCount(long count) {
		setProperty(COMPLETION_COUNT_PROPERTY_KEY, count);
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
	public MicroschemaContainerVersion getToMicroschemaVersion() {
		return out(HAS_TO_VERSION).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToMicroschemaVersion(MicroschemaContainerVersion toVersion) {
		setUniqueLinkOutTo(toVersion, HAS_TO_VERSION);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		remove();
	}

	@Override
	public MigrationStatus getStatus() {
		String status = getProperty(STATUS_PROPERTY_KEY);
		if (status == null) {
			return UNKNOWN;
		}
		return MigrationStatus.valueOf(status);
	}

	@Override
	public void setStatus(MigrationStatus status) {
		setProperty(STATUS_PROPERTY_KEY, status.name());
	}

	@Override
	public String getErrorDetail() {
		return getProperty(ERROR_DETAIL_PROPERTY_KEY);
	}

	@Override
	public void setErrorDetail(String info) {
		setProperty(ERROR_DETAIL_PROPERTY_KEY, info);
	}

	@Override
	public String getErrorMessage() {
		return getProperty(ERROR_MSG_PROPERTY_KEY);
	}

	@Override
	public void setErrorMessage(String message) {
		setProperty(ERROR_MSG_PROPERTY_KEY, message);
	}

	@Override
	public void setError(Throwable e) {
		setErrorDetail(ExceptionUtils.getStackTrace(e));
		setErrorMessage(e.getMessage());
	}

	@Override
	public boolean hasFailed() {
		return getErrorMessage() != null || getErrorDetail() != null;
	}

	@Override
	public void markAsFailed(Exception e) {
		setError(e);
	}

	@Override
	public void resetJob() {
		setStartTimestamp(null);
		setStopTimestamp(null);
		setErrorDetail(null);
		setErrorMessage(null);
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public void process() {
		log.info("Processing job {" + getUuid() + "}");
		db.tx(() -> {
			setStartTimestamp();
			setStatus(STARTING);
			setNodeName();
		});
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
			GenericRestException e = error(INTERNAL_SERVER_ERROR, "Unknown job type {" + getType() + "}");
			db.tx(() -> {
				setStopTimestamp();
				setStatus(FAILED);
				setError(e);
			});
			throw e;
		}
	}

	private void handleNodeMigration() {
		MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.schema);
		try {

			try (Tx tx = db.tx()) {
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

				ReleaseSchemaEdge releaseVersionEdge = release.findReleaseSchemaEdge(toContainerVersion);
				statusHandler.setVersionEdge(releaseVersionEdge);

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + release.getUuid()
						+ "} in project {" + project.getUuid() + "}");

				statusHandler.commitStatus();

				MeshInternal.get().nodeMigrationHandler().migrateNodes(project, release, fromContainerVersion, toContainerVersion, statusHandler)
						.await();
				statusHandler.done();
				tx.success();
			}
		} catch (Exception e) {
			statusHandler.error(e, "Error while preparing node migration.");
		}
	}

	private void handleReleaseMigration() {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.release);
		try {

			if (log.isDebugEnabled()) {
				log.debug("Release migration for job {" + getUuid() + "} was requested");
			}
			status.commitStatus();

			try (Tx tx = db.tx()) {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} cannot be found.");
				}
				MeshInternal.get().releaseMigrationHandler().migrateRelease(release, status);
				status.done();
			}
		} catch (Exception e) {
			status.error(e, "Error while preparing release migration.");
			throw e;
		}
	}

	private void handleMicroschemaMigration() {
		MigrationStatusHandler statusHandler = new MigrationStatusHandlerImpl(this, Mesh.vertx(), MigrationType.microschema);
		try {

			try (Tx tx = db.tx()) {
				Release release = getRelease();
				if (release == null) {
					throw error(BAD_REQUEST, "Release for job {" + getUuid() + "} not found");
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
				ReleaseMicroschemaEdge releaseVersionEdge = release.findReleaseMicroschemaEdge(toContainerVersion);
				statusHandler.setVersionEdge(releaseVersionEdge);

				if (log.isDebugEnabled()) {
					log.debug("Micronode migration for microschema {" + schemaContainer.getUuid() + "} from version {"
							+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} was requested");
				}

				statusHandler.commitStatus();

				MeshInternal.get().micronodeMigrationHandler().migrateMicronodes(release, fromContainerVersion, toContainerVersion, statusHandler)
						.await();
				statusHandler.done();
			}
		} catch (Exception e) {
			statusHandler.error(e, "Error while preparing micronode migration.");
		}
	}

	@Override
	public String getNodeName() {
		return getProperty(NODE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNodeName(String nodeName) {
		setProperty(NODE_NAME_PROPERTY_KEY, nodeName);
	}

}
