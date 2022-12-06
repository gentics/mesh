package com.gentics.mesh.parameter;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface DatabaseReindexParameters extends ParameterProvider {

	public static final String LIMIT_TO_PARAMETER_KEY = "limitTo";

	/**
	 * Set the index names the rebuild should be limited to.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default DatabaseReindexParameters setLimitTo(String[] indexNames) {
		return setLimitTo(Arrays.stream(indexNames));
	}

	/**
	 * Set the index names the rebuild should be limited to.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default DatabaseReindexParameters setLimitTo(Stream<String> indexNames) {
		setParameter(LIMIT_TO_PARAMETER_KEY, indexNames.collect(Collectors.joining(",")));
		return this;
	}

	/**
	 * Check if there are indices to limite the rebuild to..
	 * 
	 * @return
	 */
	default String[] isConsistencyCheck() {
		String stored = getParameter(LIMIT_TO_PARAMETER_KEY);
		return stored == null ? null : stored.split(",");
	}
}
