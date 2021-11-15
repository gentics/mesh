package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;

/**
 * Parameters for the "list jobs" endpoint
 */
public interface JobParameters extends ParameterProvider {
	public static final String STATUS_PARAMETER_KEY = "status";
	public static final String TYPE_PARAMETER_KEY = "type";

	/**
	 * Return the job stati for filtering. Empty for no filtering (return all job stati)
	 * 
	 * @return set of job stati
	 */
	default Set<JobStatus> getStatus() {
		String value = getParameter(STATUS_PARAMETER_KEY);
		if (value == null || value.isEmpty()) {
			return Collections.emptySet();
		} else {
			return Arrays.asList(value.split(",")).stream().map(name -> JobStatus.valueOf(name))
					.collect(Collectors.toSet());
		}
	}

	/**
	 * Set the stati for filtering
	 * 
	 * @param status
	 * @return
	 */
	default JobParameters setStatus(JobStatus... status) {
		setParameter(STATUS_PARAMETER_KEY,
				Arrays.asList(status).stream().map(JobStatus::name).collect(Collectors.joining(",")));
		return this;
	}

	/**
	 * Return the job types for filtering. Empty for no filtering (return all job types)
	 * 
	 * @return set of job types
	 */
	default Set<JobType> getType() {
		String value = getParameter(TYPE_PARAMETER_KEY);
		if (value == null || value.isEmpty()) {
			return Collections.emptySet();
		} else {
			return Arrays.asList(value.split(",")).stream().map(name -> JobType.valueOf(name))
					.collect(Collectors.toSet());
		}
	}

	/**
	 * Set the types for filtering
	 * 
	 * @param status
	 * @return
	 */
	default JobParameters setType(JobType... type) {
		setParameter(TYPE_PARAMETER_KEY,
				Arrays.asList(type).stream().map(JobType::name).collect(Collectors.joining(",")));
		return this;
	}

}
