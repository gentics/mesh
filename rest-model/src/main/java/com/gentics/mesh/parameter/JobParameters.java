package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.Set;

import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;

/**
 * Parameters for the "list jobs" endpoint
 */
public interface JobParameters extends ParameterProvider {
	public static final String STATUS_PARAMETER_KEY = "status";
	public static final String TYPE_PARAMETER_KEY = "type";
	public static final String BRANCH_NAME_PARAMETER_KEY = "branchName";
	public static final String BRANCH_UUID_PARAMETER_KEY = "branchUuid";
	public static final String SCHEMA_NAME_PARAMETER_KEY = "schemaName";
	public static final String SCHEMA_UUID_PARAMETER_KEY = "schemaUuid";
	public static final String MICROSCHEMA_NAME_PARAMETER_KEY = "microschemaName";
	public static final String MICROSCHEMA_UUID_PARAMETER_KEY = "microschemaUuid";
	public static final String FROM_VERSION_PARAMETER_KEY = "fromVersion";
	public static final String TO_VERSION_PARAMETER_KEY = "toVersion";

	/**
	 * Return the job stati for filtering. Empty for no filtering (return all job stati)
	 * 
	 * @return set of job stati
	 */
	default Set<JobStatus> getStatus() {
		return getMultivalueParameter(STATUS_PARAMETER_KEY, JobStatus::valueOf);
	}

	/**
	 * Set the stati for filtering
	 * 
	 * @param status
	 * @return
	 */
	default JobParameters setStatus(JobStatus... status) {
		setMultivalueParameter(STATUS_PARAMETER_KEY, Arrays.asList(status), JobStatus::name);
		return this;
	}

	/**
	 * Return the job types for filtering. Empty for no filtering (return all job types)
	 * 
	 * @return set of job types
	 */
	default Set<JobType> getType() {
		return getMultivalueParameter(TYPE_PARAMETER_KEY, JobType::valueOf);
	}

	/**
	 * Set the types for filtering
	 * 
	 * @param status
	 * @return
	 */
	default JobParameters setType(JobType... type) {
		setMultivalueParameter(TYPE_PARAMETER_KEY, Arrays.asList(type), JobType::name);
		return this;
	}

	/**
	 * Get the branch name(s) for filtering
	 * @return set of branch names (may be empty, but not null)
	 */
	default Set<String> getBranchName() {
		return getMultivalueParameter(BRANCH_NAME_PARAMETER_KEY);
	}

	/**
	 * Set the branch name(s) for filtering
	 * @param branchName branch name(s)
	 * @return fluent API
	 */
	default JobParameters setBranchName(String...branchName) {
		setMultivalueParameter(BRANCH_NAME_PARAMETER_KEY, Arrays.asList(branchName));
		return this;
	}

	/**
	 * Get the branch uuid(s) for filtering
	 * @return set of branch uuids (may be empty, but not null)
	 */
	default Set<String> getBranchUuid() {
		return getMultivalueParameter(BRANCH_UUID_PARAMETER_KEY);
	}

	/**
	 * Set the branch uuid(s) for filtering
	 * @param branchUuid branch uuid(s)
	 * @return fluent API
	 */
	default JobParameters setBranchUuid(String...branchUuid) {
		setMultivalueParameter(BRANCH_UUID_PARAMETER_KEY, Arrays.asList(branchUuid));
		return this;
	}

	/**
	 * Get the schema name(s) for filtering
	 * @return set of schema names (may be empty, but not null)
	 */
	default Set<String> getSchemaName() {
		return getMultivalueParameter(SCHEMA_NAME_PARAMETER_KEY);
	}

	/**
	 * Set the schema name(s) for filtering
	 * @param schemaName schema name(s)
	 * @return fluent API
	 */
	default JobParameters setSchemaName(String...schemaName) {
		setMultivalueParameter(SCHEMA_NAME_PARAMETER_KEY, Arrays.asList(schemaName));
		return this;
	}

	/**
	 * Get the schema uuid(s) for filtering
	 * @return set of schema uuids (may be empty, but not null)
	 */
	default Set<String> getSchemaUuid() {
		return getMultivalueParameter(SCHEMA_UUID_PARAMETER_KEY);
	}

	/**
	 * Set the schema uuid(s) for filtering
	 * @param schemaUuid schema uuid(s)
	 * @return fluent API
	 */
	default JobParameters setSchemaUuid(String...schemaUuid) {
		setMultivalueParameter(SCHEMA_UUID_PARAMETER_KEY, Arrays.asList(schemaUuid));
		return this;
	}

	/**
	 * Get the microschema name(s) for filtering
	 * @return set of microschema names (may be empty, but not null)
	 */
	default Set<String> getMicroschemaName() {
		return getMultivalueParameter(MICROSCHEMA_NAME_PARAMETER_KEY);
	}

	/**
	 * Set the microschema name(s) for filtering
	 * @param microschemaName microschema name(s)
	 * @return fluent API
	 */
	default JobParameters setMicroschemaName(String...microschemaName) {
		setMultivalueParameter(MICROSCHEMA_NAME_PARAMETER_KEY, Arrays.asList(microschemaName));
		return this;
	}

	/**
	 * Get the microschema uuid(s) for filtering
	 * @return set of microschema uuids (may be empty, but not null)
	 */
	default Set<String> getMicroschemaUuid() {
		return getMultivalueParameter(MICROSCHEMA_UUID_PARAMETER_KEY);
	}

	/**
	 * Set the microschema uuid(s) for filtering
	 * @param microschemaUuid microschema uuid(s)
	 * @return fluent API
	 */
	default JobParameters setMicroschemaUuid(String...microschemaUuid) {
		setMultivalueParameter(MICROSCHEMA_UUID_PARAMETER_KEY, Arrays.asList(microschemaUuid));
		return this;
	}

	/**
	 * Get the fromVersion(s) for filtering
	 * @return set of fromVersions (may be empty, but not null)
	 */
	default Set<String> getFromVersion() {
		return getMultivalueParameter(FROM_VERSION_PARAMETER_KEY);
	}

	/**
	 * Set the fromVersion(s) for filtering
	 * @param fromVersion fromVersion(s)
	 * @return fluent API
	 */
	default JobParameters setFromVersion(String...fromVersion) {
		setMultivalueParameter(FROM_VERSION_PARAMETER_KEY, Arrays.asList(fromVersion));
		return this;
	}

	/**
	 * Get the toVersion(s) for filtering
	 * @return set of toVersions (may be empty, but not null)
	 */
	default Set<String> getToVersion() {
		return getMultivalueParameter(TO_VERSION_PARAMETER_KEY);
	}

	/**
	 * Set the toVersion(s) for filtering
	 * @param toVersion toVersion(s)
	 * @return fluent API
	 */
	default JobParameters setToVersion(String...toVersion) {
		setMultivalueParameter(TO_VERSION_PARAMETER_KEY, Arrays.asList(toVersion));
		return this;
	}

	/**
	 * Check whether any parameters were set
	 * @return true if no parameters have been set, false otherwise
	 */
	default boolean isEmpty() {
		return getStatus().isEmpty() &&
				getType().isEmpty() &&
				getBranchName().isEmpty() &&
				getBranchUuid().isEmpty() &&
				getSchemaName().isEmpty() &&
				getSchemaUuid().isEmpty() &&
				getMicroschemaName().isEmpty() &&
				getMicroschemaUuid().isEmpty() &&
				getFromVersion().isEmpty() &&
				getToVersion().isEmpty();
	}
}
