package com.gentics.mesh.etc.config.env;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface Option {

	static final Logger log = LoggerFactory.getLogger(Option.class);

	/**
	 * Override the annotated methods and fields with
	 * environment variables.
	 */
	default void overrideWithEnv() {
		for (Method method : getClass().getDeclaredMethods()) {
			if (method.getParameterCount() == 1 && method.isAnnotationPresent(EnvironmentVariable.class)) {
				OptionUtils.overrideWithEnvViaMethod(method, this);
			}
		}
		for (Field field : getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(EnvironmentVariable.class)) {
				OptionUtils.overrideWitEnvViaFieldSet(field, this);
			}
		}
	}

	/**
	 * Validate the options.
	 * 
	 * @param options
	 *            Reference to the full mesh option. This may be useful in order to access other option values and assert that no conflict can happen.
	 */
	default void validate(MeshOptions options) {
		// No validation
	}

}
