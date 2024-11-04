package com.gentics.mesh.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.MariaDBConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class MariaDBService implements DatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options) {
		return new MariaDBConnector(options);
	}
}
