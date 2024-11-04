package com.gentics.mesh.enterprise.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.enterprise.database.connector.PostgreSQLConnector;
import com.gentics.mesh.enterprise.license.LicenseService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class PostgresService implements EnterpriseDatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options, LicenseService licenseService) {
		return new PostgreSQLConnector(options, licenseService);
	}
}
