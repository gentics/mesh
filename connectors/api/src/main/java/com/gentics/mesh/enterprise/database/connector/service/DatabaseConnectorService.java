package com.gentics.mesh.enterprise.database.connector.service;

import java.util.ServiceLoader;

import com.gentics.mesh.enterprise.database.connector.DatabaseConnector;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;

/**
 * An instantiation entry point for the {@link ServiceLoader}.
 */
public interface DatabaseConnectorService {

	/**
	 * Instantiate the connector.
	 * 
	 * @param options
	 */
	DatabaseConnector instantiate(EnterpriseMeshOptions options);
}
