package com.gentics.mesh.parameter;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.util.DateUtils;

public interface ProjectPurgeParameters extends ParameterProvider {

	public static final String SINCE_QUERY_PARAM_KEY = "since";

	default ProjectPurgeParameters setSince(String date) {
		setParameter(SINCE_QUERY_PARAM_KEY, date);
		return this;
	}

	default String getSince() {
		return getParameter(SINCE_QUERY_PARAM_KEY);
	}

	default Optional<ZonedDateTime> getSinceDate() {
		String since = getSince();
		if (since == null) {
			return Optional.empty();
		} else {
			long timeInMs = DateUtils.fromISO8601(since, true);
			ZonedDateTime date = DateUtils.toZonedDateTime(timeInMs);
			return Optional.of(date);
		}
	}

}
