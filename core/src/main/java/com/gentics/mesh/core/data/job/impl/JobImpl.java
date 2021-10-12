package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Job
 */
public abstract class JobImpl extends AbstractMeshCoreVertex<JobResponse> implements Job {

	private static final Logger log = LoggerFactory.getLogger(JobImpl.class);

	private static final String ERROR_DETAIL_MAX_LENGTH_MSG = "..." + System.lineSeparator() +
		"For further details concerning this error please refer to the logs.";

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Jobs can't be updated");
	}

	@Override
	public TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	@Override
	public JobWarningList getWarnings() {
		String json = property(WARNING_PROPERTY_KEY);
		if (json == null) {
			return new JobWarningList();
		} else {
			return JsonUtil.readValue(json, JobWarningList.class);
		}
	}

	@Override
	public void setWarnings(JobWarningList warnings) {
		String json = JsonUtil.toJson(warnings);
		property(WARNING_PROPERTY_KEY, json);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return getErrorMessage() + getErrorDetail();
	}

	@Override
	public void setType(JobType type) {
		property(TYPE_PROPERTY_KEY, type.name());
	}

	@Override
	public JobType getType() {
		String type = property(TYPE_PROPERTY_KEY);
		if (type == null) {
			return null;
		} else {
			return JobType.valueOf(type);
		}
	}

	@Override
	public Long getStartTimestamp() {
		return property(START_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setStartTimestamp(Long date) {
		property(START_TIMESTAMP_PROPERTY_KEY, date);
	}

	@Override
	public Long getStopTimestamp() {
		return property(STOP_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setStopTimestamp(Long date) {
		property(STOP_TIMESTAMP_PROPERTY_KEY, date);
	}

	@Override
	public long getCompletionCount() {
		Long value = property(COMPLETION_COUNT_PROPERTY_KEY);
		return value == null ? 0 : value;
	}

	@Override
	public void setCompletionCount(long count) {
		property(COMPLETION_COUNT_PROPERTY_KEY, count);
	}

	@Override
	public HibBranch getBranch() {
		return out(HAS_BRANCH, BranchImpl.class).nextOrNull();
	}

	@Override
	public void setBranch(HibBranch branch) {
		setSingleLinkOutTo(toGraph(branch), HAS_BRANCH);
	}

	@Override
	public HibSchemaVersion getFromSchemaVersion() {
		return out(HAS_FROM_VERSION).has(SchemaContainerVersionImpl.class).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public void setFromSchemaVersion(HibSchemaVersion version) {
		setSingleLinkOutTo(toGraph(version), HAS_FROM_VERSION);
	}

	@Override
	public HibSchemaVersion getToSchemaVersion() {
		return out(HAS_TO_VERSION).has(SchemaContainerVersionImpl.class).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToSchemaVersion(HibSchemaVersion version) {
		setSingleLinkOutTo(toGraph(version), HAS_TO_VERSION);
	}

	@Override
	public HibMicroschemaVersion getFromMicroschemaVersion() {
		return out(HAS_FROM_VERSION).has(MicroschemaContainerVersionImpl.class).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setFromMicroschemaVersion(HibMicroschemaVersion fromVersion) {
		setSingleLinkOutTo(toGraph(fromVersion), HAS_FROM_VERSION);
	}

	@Override
	public HibMicroschemaVersion getToMicroschemaVersion() {
		return out(HAS_TO_VERSION).has(MicroschemaContainerVersionImpl.class).nextOrDefaultExplicit(MicroschemaContainerVersionImpl.class, null);
	}

	@Override
	public void setToMicroschemaVersion(HibMicroschemaVersion toVersion) {
		setSingleLinkOutTo(toGraph(toVersion), HAS_TO_VERSION);
	}

	@Override
	public void delete(BulkActionContext bac) {
		remove();
	}

	@Override
	public JobStatus getStatus() {
		String status = property(STATUS_PROPERTY_KEY);
		if (status == null) {
			return UNKNOWN;
		}
		return JobStatus.valueOf(status);
	}

	@Override
	public void setStatus(JobStatus status) {
		property(STATUS_PROPERTY_KEY, status.name());
	}

	@Override
	public String getErrorDetail() {
		return property(ERROR_DETAIL_PROPERTY_KEY);
	}

	@Override
	public void setErrorDetail(String info) {
		// truncate the error detail message to the max length for the error detail property
		if (info != null && info.length() > ERROR_DETAIL_MAX_LENGTH) {
			info = info.substring(0, ERROR_DETAIL_MAX_LENGTH - ERROR_DETAIL_MAX_LENGTH_MSG.length()) + ERROR_DETAIL_MAX_LENGTH_MSG;
		}
		property(ERROR_DETAIL_PROPERTY_KEY, info);
	}

	@Override
	public String getErrorMessage() {
		return property(ERROR_MSG_PROPERTY_KEY);
	}

	@Override
	public void setErrorMessage(String message) {
		property(ERROR_MSG_PROPERTY_KEY, message);
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
		setStatus(JobStatus.QUEUED);
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public Completable process() {
		return Completable.defer(() -> {

			db().tx(() -> {
				log.info("Processing job {" + getUuid() + "}");
				setStartTimestamp();
				setStatus(STARTING);
				setNodeName();
			});

			return processTask();
		});
	}

	/**
	 * Actual implementation of the task which the job executes.
	 */
	protected abstract Completable processTask();

	@Override
	public String getNodeName() {
		return property(NODE_NAME_PROPERTY_KEY);
	}

	@Override
	public void setNodeName(String nodeName) {
		property(NODE_NAME_PROPERTY_KEY, nodeName);
	}

}
