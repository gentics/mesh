package com.gentics.mesh.core.data.job;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.util.DateUtils;

import io.reactivex.Completable;

/**
 * A job can be added to the {@link JobRoot} vertex. Jobs are used to persist information about long running tasks.
 */
public interface Job extends MeshCoreVertex<JobResponse, Job>, CreatorTrackingVertex {

	String TYPE_PROPERTY_KEY = "type";

	String ERROR_DETAIL_PROPERTY_KEY = "error_detail";

	String ERROR_MSG_PROPERTY_KEY = "error_msg";

	String START_TIMESTAMP_PROPERTY_KEY = "startDate";

	String STOP_TIMESTAMP_PROPERTY_KEY = "stopDate";

	String COMPLETION_COUNT_PROPERTY_KEY = "completionCount";

	String STATUS_PROPERTY_KEY = "status";

	String NODE_NAME_PROPERTY_KEY = "nodeName";

	String WARNING_PROPERTY_KEY = "warnings";

	/**
	 * The max length before detail error messages will be truncated
	 */
	int ERROR_DETAIL_MAX_LENGTH = 50000;

	/**
	 * Return the job type.
	 * 
	 * @return
	 */
	JobType getType();

	/**
	 * Set the job type.
	 * 
	 * @param type
	 */
	void setType(JobType type);

	/**
	 * Return the branch reference for the job.
	 * 
	 * @return
	 */
	Branch getBranch();

	/**
	 * Set the branch reference for the job.
	 * 
	 * @param branch
	 */
	void setBranch(Branch branch);

	/**
	 * Return the schema version reference.
	 * 
	 * @return
	 */
	SchemaContainerVersion getFromSchemaVersion();

	/**
	 * Set the schema version reference.
	 * 
	 * @param version
	 */
	void setFromSchemaVersion(SchemaContainerVersion version);

	/**
	 * Return the schema version reference.
	 * 
	 * @return
	 */
	SchemaContainerVersion getToSchemaVersion();

	/**
	 * Set the schema version reference.
	 * 
	 * @param version
	 */
	void setToSchemaVersion(SchemaContainerVersion version);

	/**
	 * Return the microschema version reference.
	 * 
	 * @return
	 */
	MicroschemaContainerVersion getFromMicroschemaVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param fromVersion
	 */
	void setFromMicroschemaVersion(MicroschemaContainerVersion fromVersion);

	/**
	 * Return the microschema version reference.
	 * 
	 * @return
	 */
	MicroschemaContainerVersion getToMicroschemaVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param toVersion
	 */
	void setToMicroschemaVersion(MicroschemaContainerVersion toVersion);

	/**
	 * Process the job.
	 */
	Completable process();

	/**
	 * Mark the job as failed.
	 * 
	 * @param e
	 */
	void markAsFailed(Exception e);

	/**
	 * Set the error information using the provided exception.
	 * 
	 * @param e
	 */
	void setError(Throwable e);

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
	 * Removes the error information from the job and thus it can be processed again.
	 */
	void resetJob();

	/**
	 * Check whether the job has failed.
	 * 
	 * @return
	 */
	boolean hasFailed();

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
	 * Set the current node name.
	 */
	default void setNodeName() {
		String nodeName = Mesh.mesh().getOptions().getNodeName();
		setNodeName(nodeName);
	}

	/**
	 * Return the stored warnings.
	 * @return
	 */
	JobWarningList getWarnings();

	/**
	 * Set the list of warnings.
	 * @param warning
	 */
	void setWarnings(JobWarningList warnings);

}
