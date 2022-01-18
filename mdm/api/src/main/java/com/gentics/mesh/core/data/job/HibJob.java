package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.MeshEvent.JOB_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.JOB_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.JOB_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.util.DateUtils;

import io.reactivex.Completable;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Domain model for job.
 */
public interface HibJob extends HibCoreElement<JobResponse>, HibCreatorTracking {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.JOB, JOB_CREATED, JOB_UPDATED, JOB_DELETED);

	String TYPE_PROPERTY_KEY = "type";

	String ERROR_DETAIL_PROPERTY_KEY = "error_detail";

	String ERROR_MSG_PROPERTY_KEY = "error_msg";

	String START_TIMESTAMP_PROPERTY_KEY = "startDate";

	String STOP_TIMESTAMP_PROPERTY_KEY = "stopDate";

	String COMPLETION_COUNT_PROPERTY_KEY = "completionCount";

	String STATUS_PROPERTY_KEY = "status";

	String NODE_NAME_PROPERTY_KEY = "nodeName";

	String WARNING_PROPERTY_KEY = "warnings";

	String ERROR_DETAIL_MAX_LENGTH_MSG = "..." + System.lineSeparator() +
			"For further details concerning this error please refer to the logs.";

	/**
	 * The max length before detail error messages will be truncated
	 */
	int ERROR_DETAIL_MAX_LENGTH = 50000;

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Set the current node name.
	 */
	default void setNodeName() {
		String nodeName = Tx.get().data().options().getNodeName();
		setNodeName(nodeName);
	}

	/**
	 * Set the branch reference for this job.
	 * 
	 * @param branch
	 */
	void setBranch(HibBranch branch);

	/**
	 * Mark the error as failed and store the exception information.
	 * 
	 * @param e
	 */
	default void markAsFailed(Exception e) {
		setError(e);
	}

	/**
	 * Return the branch of the job.
	 * 
	 * @return
	 */
	HibBranch getBranch();

	/**
	 * Return the type of the job.
	 * 
	 * @return
	 */
	JobType getType();

	/**
	 * Return the human readable error message.
	 * 
	 * @return
	 */
	String getErrorMessage();

	/**
	 * Set the human readable error message.
	 * 
	 * @param message
	 */
	void setErrorMessage(String message);

	/**
	 * Return the error detail information.
	 * 
	 * @return
	 */
	String getErrorDetail();

	/**
	 * Set the error detail information.
	 * 
	 * @param info
	 */
	void setErrorDetail(String info);

	/**
	 * Get migration status.
	 * 
	 * @return
	 */
	JobStatus getStatus();

	/**
	 * Set migration status.
	 * 
	 * @param status
	 */
	void setStatus(JobStatus status);

	/**
	 * Removes the error information from the job and thus it can be processed again.
	 */
	default void resetJob() {
		setStartTimestamp(null);
		setStopTimestamp(null);
		setErrorDetail(null);
		setErrorMessage(null);
		setStatus(JobStatus.QUEUED);
	}

	/**
	 * Check whether the job has failed.
	 *
	 * @return
	 */
	default boolean hasFailed() {
		return getErrorMessage() != null || getErrorDetail() != null;
	}

	/**
	 * Set the job type.
	 * 
	 * @param type
	 */
	void setType(JobType type);

	/**
	 * Return the schema version reference.
	 * 
	 * @return
	 */
	HibSchemaVersion getFromSchemaVersion();

	/**
	 * Set the schema version reference.
	 * 
	 * @param version
	 */
	void setFromSchemaVersion(HibSchemaVersion version);

	/**
	 * Return the schema version reference.
	 * 
	 * @return
	 */
	HibSchemaVersion getToSchemaVersion();

	/**
	 * Set the schema version reference.
	 * 
	 * @param version
	 */
	void setToSchemaVersion(HibSchemaVersion version);

	/**
	 * Return the microschema version reference.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getFromMicroschemaVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param fromVersion
	 */
	void setFromMicroschemaVersion(HibMicroschemaVersion fromVersion);

	/**
	 * Return the microschema version reference.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getToMicroschemaVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param toVersion
	 */
	void setToMicroschemaVersion(HibMicroschemaVersion toVersion);

	/**
	 * Set the error information using the provided exception.
	 * 
	 * @param e
	 */
	default void setError(Throwable e) {
		String stackTrace = ExceptionUtils.getStackTrace(e);
		// truncate the error detail message to the max length for the error detail property
		setErrorDetail(truncateStackTrace(stackTrace));
		setErrorMessage(e.getMessage());
	}

	private String truncateStackTrace(String info) {
		if (info != null && info.length() > ERROR_DETAIL_MAX_LENGTH) {
			return info.substring(0, ERROR_DETAIL_MAX_LENGTH - ERROR_DETAIL_MAX_LENGTH_MSG.length()) + ERROR_DETAIL_MAX_LENGTH_MSG;
		}
		return info;
	}

	/**
	 * Return the start date of the job.
	 * 
	 * @return
	 */
	default String getStartDate() {
		Long timestamp = getStartTimestamp();
		if (timestamp == null) {
			return null;
		}
		return DateUtils.toISO8601(timestamp);
	}

	/**
	 * Return the start timestamp of the job.
	 * 
	 * @return
	 */
	Long getStartTimestamp();

	/**
	 * Set the start timestamp of the job.
	 * 
	 * @param date
	 */
	void setStartTimestamp(Long date);

	/**
	 * Set the current start timestamp.
	 */
	default void setStartTimestamp() {
		setStartTimestamp(System.currentTimeMillis());
	}

	/**
	 * Return the stop date of the job.
	 * 
	 * @return
	 */
	default String getStopDate() {
		Long timestamp = getStopTimestamp();
		if (timestamp == null) {
			return null;
		}
		return DateUtils.toISO8601(timestamp);
	}

	/**
	 * Return the stop timestamp of the job.
	 * 
	 * @return
	 */
	Long getStopTimestamp();

	/**
	 * Set the stop date of the job.
	 * 
	 * @param date
	 */
	void setStopTimestamp(Long date);

	/**
	 * Set the current stop timestamp.
	 */
	default void setStopTimestamp() {
		setStopTimestamp(System.currentTimeMillis());
	}

	/**
	 * Return the amount of elements which have already been processed.
	 * 
	 * @return
	 */
	long getCompletionCount();

	/**
	 * Set the count of total processed elements.
	 * 
	 * @param count
	 */
	void setCompletionCount(long count);

	/**
	 * Return the name of the node on which the job is being executed.
	 * 
	 * @return
	 */
	String getNodeName();

	/**
	 * Set the name of the node on which the job is being executed.
	 * 
	 * @param nodeName
	 */
	void setNodeName(String nodeName);

	/**
	 * Return the stored warnings.
	 * 
	 * @return
	 */
	JobWarningList getWarnings();

	/**
	 * Set the list of warnings.
	 * 
	 * @param warning
	 */
	void setWarnings(JobWarningList warnings);

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/admin/jobs/" + getUuid();
	}
}
