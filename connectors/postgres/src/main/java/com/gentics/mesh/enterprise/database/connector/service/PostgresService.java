package com.gentics.mesh.enterprise.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.service.DatabaseConnectorService;
import com.gentics.mesh.enterprise.database.connector.PostgreSQLConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class PostgresService implements DatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options) {
		return new PostgreSQLConnector(options);
	}
}
