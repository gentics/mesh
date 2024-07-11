package com.gentics.mesh.core;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.gentics.mesh.database.connector.DatabaseConnector;

/**
 * Annotation for subclasses of {@link ManagedDatabaseTestContextProvider} to specify details of the managed database.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ManagedBy {
	/**
	 * Name of the managed database. This is also used in the WebSocket URL the provider connects to
	 * @return name
	 */
	String name();

	/**
	 * Class of the Mesh Database Connector
	 * @return class
	 */
	Class<? extends DatabaseConnector> connector();
}
