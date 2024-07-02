package com.gentics.mesh.enterprise.database.connector.service;

import com.gentics.mesh.enterprise.database.connector.DatabaseConnector;
import com.gentics.mesh.enterprise.database.connector.OracleConnector;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;

public class Service implements DatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(EnterpriseMeshOptions options) {
		return new OracleConnector(options);
	}
}
