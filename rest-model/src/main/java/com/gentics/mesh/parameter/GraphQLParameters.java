package com.gentics.mesh.parameter;

import java.util.Optional;

public interface GraphQLParameters extends ParameterProvider {
	String WAIT_PARAMETER_KEY = "wait";

	default GraphQLParameters setWait(boolean flag) {
		setParameter(WAIT_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

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
