package com.gentics.mesh.etc.config.env;

import java.lang.reflect.Field;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface Option {

	static final Logger log = LoggerFactory.getLogger(Option.class);

	default void overrideWithEnv() {
		for (Field field : getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(EnvironmentVariable.class)) {
				EnvironmentVariable envInfo = field.getAnnotation(EnvironmentVariable.class);
				String name = envInfo.name();
				String value = System.getenv(name);
				try {
					if (value != null) {
						field.setAccessible(true);
						Class<?> typeClazz = field.getType();
						log.info("Setting env {" + name + "=" + value + "}");
						if ("null".equals(value)) {
							value = null;
						}
						if (typeClazz.equals(String.class)) {
							field.set(this, value);
							continue;
						}
						if (typeClazz.equals(boolean.class)) {
							field.setBoolean(this, Boolean.valueOf(value));
							continue;
						}
						if (typeClazz.equals(long.class)) {
							field.setLong(this, Long.valueOf(value));
							continue;
						}
						if (typeClazz.equals(int.class)) {
							field.setInt(this, Integer.valueOf(value));
							continue;
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Could not set environment variable {" + name + "} with value {" + value + "}", e);
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
