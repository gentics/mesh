package com.gentics.mesh.etc.config.env;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface Option {

	Logger log = LoggerFactory.getLogger(Option.class);

	/**
	 * Override the annotated methods and fields of this option class
	 * and referenced sub options with environment variables.
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
			// check if the current fields is an option if so we call the override with env recursively
			if (Option.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				Option subOption;
				try {
					subOption = (Option) field.get(this);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Could not access sub option: " + field.getName(), e);
				}
				if (subOption != null) {
					log.trace("Override sub option: " + field.getName());
					subOption.overrideWithEnv();
				}
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
