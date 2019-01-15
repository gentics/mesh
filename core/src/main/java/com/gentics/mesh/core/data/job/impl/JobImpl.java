package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.STARTING;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.UNKNOWN;

import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.ETag;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Job
 */
public abstract class JobImpl extends AbstractMeshCoreVertex<JobResponse, Job> implements Job {

	private static final Logger log = LoggerFactory.getLogger(JobImpl.class);

	private static final String ERROR_DETAIL_MAX_LENGTH_MSG = "..." + System.lineSeparator() +
		"For further details concerning this error please refer to the logs.";

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
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
		response.setWarnings(getWarnings().getData());

		Map<String, String> props = response.getProperties();
		Release release = getRelease();
		if (release != null) {
			props.put("releaseName", release.getName());
			props.put("releaseUuid", release.getUuid());
		}

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
	public JobWarningList getWarnings() {
		String json = getProperty(WARNING_PROPERTY_KEY);
		if (json == null) {
			return new JobWarningList();
		} else {
			return JsonUtil.readValue(json, JobWarningList.class);
		}
	}

	@Override
	public void setWarnings(JobWarningList warnings) {
		String json = JsonUtil.toJson(warnings);
		setProperty(WARNING_PROPERTY_KEY, json);
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
		return out(HAS_FROM_VERSION).has(SchemaContainerVersionImpl.class).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public void setFromSchemaVersion(SchemaContainerVersion version) {
		setUniqueLinkOutTo(version, HAS_FROM_VERSION);
	}

	@Override
	public SchemaContainerVersion getToSchemaVersion() {
		return out(HAS_TO_VERSION).has(SchemaContainerVersionImpl.class).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToSchemaVersion(SchemaContainerVersion version) {
		setUniqueLinkOutTo(version, HAS_TO_VERSION);
	}

	@Override
	public MicroschemaContainerVersion getFromMicroschemaVersion() {
		return out(HAS_FROM_VERSION).has(MicroschemaContainerVersionImpl.class).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setFromMicroschemaVersion(MicroschemaContainerVersion fromVersion) {
		setUniqueLinkOutTo(fromVersion, HAS_FROM_VERSION);
	}

	@Override
	public MicroschemaContainerVersion getToMicroschemaVersion() {
		return out(HAS_TO_VERSION).has(MicroschemaContainerVersionImpl.class).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToMicroschemaVersion(MicroschemaContainerVersion toVersion) {
		setUniqueLinkOutTo(toVersion, HAS_TO_VERSION);
	}

	@Override
	public void delete(BulkActionContext bac) {
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
		// truncate the error detail message to the max length for the error detail property
		if (info != null && info.length() > ERROR_DETAIL_MAX_LENGTH) {
			info = info.substring(0, ERROR_DETAIL_MAX_LENGTH - ERROR_DETAIL_MAX_LENGTH_MSG.length()) + ERROR_DETAIL_MAX_LENGTH_MSG;
		}
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
		setStatus(MigrationStatus.QUEUED);
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public void process() {
		log.info("Processing job {" + getUuid() + "}");
		DB.get().tx(() -> {
			setStartTimestamp();
			setStatus(STARTING);
			setNodeName();
		});

		processTask();
	}

	/**
	 * Actual implementation of the task which the job executes.
	 */
	protected abstract void processTask();

	@Override
	public String getNodeName() {
		return getProperty(NODE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNodeName(String nodeName) {
		setProperty(NODE_NAME_PROPERTY_KEY, nodeName);
	}

}
