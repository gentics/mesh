package com.gentics.mesh.parameter;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.util.DateUtils;

/**
 * Interface for project purge query parameters.
 */
public interface ProjectPurgeParameters extends ParameterProvider {

	public static final String BEFORE_QUERY_PARAM_KEY = "before";

	default ProjectPurgeParameters setBefore(String date) {
		setParameter(BEFORE_QUERY_PARAM_KEY, date);
		return this;
	}

	default String getBefore() {
		return getParameter(BEFORE_QUERY_PARAM_KEY);
	}

	default Optional<ZonedDateTime> getBeforeDate() {
		String since = getBefore();
		if (since == null) {
			return Optional.empty();
		} else {
			long timeInMs = DateUtils.fromISO8601(since, true);
			ZonedDateTime date = DateUtils.toZonedDateTime(timeInMs);
			return Optional.of(date);
		}
	}

}
