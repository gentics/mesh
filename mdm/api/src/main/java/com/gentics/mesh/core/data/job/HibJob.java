package com.gentics.mesh.core.data.job;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.util.DateUtils;

import io.reactivex.Completable;

/**
 * Domain model for job.
 */
public interface HibJob extends HibCoreElement<JobResponse>, HibCreatorTracking {

	/**
	 * Set the branch reference for this job.
	 * 
	 * @param branch
	 */
	void setBranch(HibBranch branch);

	/**
	 * Mark the error as failed and store the exception information.
	 * 
	 * @param ex
	 */
	void markAsFailed(Exception ex);

	/**
	 * Return the branch of the job.
	 * 
	 * @return
	 */
	HibBranch getBranch();

	/**
	 * Return the creator of the job.
	 * 
	 * @return
	 */
	HibUser getCreator();

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
	void resetJob();

	/**
	 * Check whether the job has failed.
	 * 
	 * @return
	 */
	boolean hasFailed();

	/**
	 * Remove the job.
	 */
	void remove();

	/**
	 * The max length before detail error messages will be truncated
	 */
	int ERROR_DETAIL_MAX_LENGTH = 50000;

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
	 * Process the job.
	 */
	Completable process();

	/**
	 * Set the error information using the provided exception.
	 * 
	 * @param e
	 */
	void setError(Throwable e);

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

	@Override
	default JobResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		JobResponse response = new JobResponse();
		response.setUuid(getUuid());

		HibUser creator = getCreator();
		if (creator != null) {
			response.setCreator(creator.transformToReference());
		} else {
			//log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no creator. Omitting creator field");
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

		JobWarningList warnings = getWarnings();
		if (warnings != null) {
			response.setWarnings(warnings.getData());
		}

		Map<String, String> props = response.getProperties();
		HibBranch branch = getBranch();
		if (branch != null) {
			props.put("branchName", branch.getName());
			props.put("branchUuid", branch.getUuid());
		} else {
			log.debug("No referenced branch found.");
		}

		HibSchemaVersion toSchema = getToSchemaVersion();
		if (toSchema != null) {
			HibSchema container = toSchema.getSchemaContainer();
			props.put("schemaName", container.getName());
			props.put("schemaUuid", container.getUuid());
			props.put("fromVersion", getFromSchemaVersion().getVersion());
			props.put("toVersion", toSchema.getVersion());
		}

		HibMicroschemaVersion toMicroschema = getToMicroschemaVersion();
		if (toMicroschema != null) {
			HibMicroschema container = toMicroschema.getSchemaContainer();
			props.put("microschemaName", container.getName());
			props.put("microschemaUuid", container.getUuid());
			props.put("fromVersion", getFromMicroschemaVersion().getVersion());
			props.put("toVersion", toMicroschema.getVersion());
		}
		return response;
	}
}
