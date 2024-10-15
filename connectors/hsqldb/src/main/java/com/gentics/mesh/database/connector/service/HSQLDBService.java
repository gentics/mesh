package com.gentics.mesh.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.HSQLDBConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.handler.RuntimeServiceRegistry;

public class HSQLDBService implements DatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options, RuntimeServiceRegistry runtimeServiceRegistry) {
		return new HSQLDBConnector(options);
	}
}
