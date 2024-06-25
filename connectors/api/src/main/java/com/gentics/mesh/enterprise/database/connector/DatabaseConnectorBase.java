package com.gentics.mesh.enterprise.database.connector;

/**
 * Base functions of a database connector.
 */
public abstract class DatabaseConnectorBase implements DatabaseConnector {

	protected static String MAIN_CLASS_NAME = null;

	/**
	 * Get the main connector class name, installed by an implementor class loader.
	 * 
	 * @return
	 */
	public static String getImplementorClassName() {
		return MAIN_CLASS_NAME;
	}
}
