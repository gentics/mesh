package com.gentics.mesh.enterprise.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.service.DatabaseConnectorService;
import com.gentics.mesh.enterprise.database.connector.OracleConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.handler.RuntimeServiceRegistry;

public class OracleService implements DatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options, RuntimeServiceRegistry runtimeServiceRegistry) {
		return new OracleConnector(options, runtimeServiceRegistry);
	}
}
