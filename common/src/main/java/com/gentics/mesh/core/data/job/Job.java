package com.gentics.mesh.core.data.job;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.util.DateUtils;

/**
 * A job can be added to the {@link JobRoot} vertex. Jobs are used to persist information about long running tasks.
 */
public interface Job extends MeshCoreVertex<JobResponse, Job>, CreatorTrackingVertex {

	public static final String TYPE_PROPERTY_KEY = "type";

	public static final String ERROR_DETAIL_PROPERTY_KEY = "error_detail";

	public static final String ERROR_MSG_PROPERTY_KEY = "error_msg";

	public static final String START_TIMESTAMP_PROPERTY_KEY = "startDate";

	public static final String STOP_TIMESTAMP_PROPERTY_KEY = "stopDate";

	public static final String COMPLETION_COUNT_PROPERTY_KEY = "completionCount";

	public static final String STATUS_PROPERTY_KEY = "status";

	public static final String NODE_NAME_PROPERTY_KEY = "nodeName";
	/**
	 * The max length before detail error messages will be truncated
	 */
	int ERROR_DETAIL_MAX_LENGTH = 50000;

	/**
	 * Return the job type.
	 * 
	 * @return
	 */
	MigrationType getType();

	/**
	 * Set the job type.
	 * 
	 * @param schema
	 */
	void setType(MigrationType type);

	/**
	 * Return the release reference for the job.
	 * 
	 * @return
	 */
	Release getRelease();

	/**
	 * Set the release reference for the job.
	 * 
	 * @param release
	 */
	void setRelease(Release release);

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
	 * Prepare the job.
	 */
	void prepare();

	/**
	 * Process the job.
	 */
	void process();

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
	MigrationStatus getStatus();

	/**
	 * Set migration status.
	 * 
	 * @param status
	 */
	void setStatus(MigrationStatus status);

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

}
