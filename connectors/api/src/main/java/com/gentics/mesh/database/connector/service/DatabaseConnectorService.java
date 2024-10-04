package com.gentics.mesh.database.connector.service;

import java.util.ServiceLoader;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.handler.RuntimeServiceRegistry;

/**
 * An instantiation entry point for the {@link ServiceLoader}.
 */
public interface DatabaseConnectorService {

	/**
	 * Instantiate the connector.
	 * 
	 * @param options
	 */
	DatabaseConnector instantiate(HibernateMeshOptions options, RuntimeServiceRegistry runtimeServiceRegistry);
}
