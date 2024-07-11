package com.gentics.mesh.database;

import java.util.Map;

/**
 * Database entity high level contract.
 * 
 * @author plyhun
 *
 */
public interface DatabaseProvider {
	/**
	 * Initialize the database and return persistance.xml properties
	 * @return
	 */
	Map<String, Object> init() throws Exception;

	/**
	 * Optional cleanup
	 */
	default void close() {

	}

	/**
	 * Describe the implementor.
	 * 
	 * @return
	 */
	String getProviderDescription();
}
