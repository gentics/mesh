package com.gentics.mesh.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.HSQLDBConnector;
import com.gentics.mesh.database.connector.service.DatabaseConnectorService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class HSQLDBService implements DatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options) {
		return new HSQLDBConnector(options);
	}
}
