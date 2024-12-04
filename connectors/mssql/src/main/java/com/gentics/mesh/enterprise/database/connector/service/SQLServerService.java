package com.gentics.mesh.enterprise.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.enterprise.database.connector.SQLServerConnector;
import com.gentics.mesh.enterprise.license.LicenseService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public class SQLServerService implements EnterpriseDatabaseConnectorService {

	@Override
	public DatabaseConnector instantiate(HibernateMeshOptions options, LicenseService licenseService) {
		return new SQLServerConnector(options, licenseService);
	}
}
