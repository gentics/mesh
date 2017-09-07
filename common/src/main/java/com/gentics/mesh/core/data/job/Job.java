package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.rest.job.JobResponse;

/**
 * A job can be added to the {@link JobRoot} vertex. Jobs are used to persist information about long running tasks.
 */
public interface Job extends MeshCoreVertex<JobResponse, Job>, CreatorTrackingVertex {

	public static final String TYPE_PROPERTY_KEY = "type";

	public static final String ERROR_DETAIL_PROPERTY_KEY = "error_detail";

	public static final String ERROR_MSG_PROPERTY_KEY = "error_msg";

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
	MicroschemaContainerVersion getToMicroschemaContainerVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param toVersion
	 */
	void setToMicroschemaVersion(MicroschemaContainerVersion toVersion);

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
	void setError(Exception e);

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
	void removeErrorState();

	/**
	 * Check whether the job has failed.
	 * 
	 * @return
	 */
	boolean hasFailed();

}
