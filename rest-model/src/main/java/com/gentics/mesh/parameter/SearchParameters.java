package com.gentics.mesh.parameter;

import java.util.Optional;

public interface SearchParameters extends ParameterProvider {

	String WAIT_PARAMETER_KEY = "wait";

	/**
	 * Set the recursive flag. When enabled the deletion will also effect subelements.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	default SearchParameters setWait(boolean flag) {
		setParameter(WAIT_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the recursive flag for recursive deletion is enabled.
	 * 
	 * @return
	 */
	default Optional<Boolean> isWait() {
		return Optional.ofNullable(getParameter(WAIT_PARAMETER_KEY))
			.flatMap(str -> {
				if ("true".equalsIgnoreCase(str)) {
					return Optional.of(true);
				} else if ("false".equalsIgnoreCase(str)) {
					return Optional.of(false);
				} else {
					return Optional.empty();
				}
			});
	}
}
