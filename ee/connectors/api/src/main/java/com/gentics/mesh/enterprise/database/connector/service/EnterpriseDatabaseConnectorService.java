package com.gentics.mesh.enterprise.database.connector.service;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.database.connector.service.DatabaseConnectorService;
import com.gentics.mesh.enterprise.license.LicenseService;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

public interface EnterpriseDatabaseConnectorService extends DatabaseConnectorService {

	/**
	 * Instantiate the connector.
	 * 
	 * @param options
	 * @param licenseService
	 */
	DatabaseConnector instantiate(HibernateMeshOptions options, LicenseService licenseService);

	@Override
	default DatabaseConnector instantiate(HibernateMeshOptions options) {
		return instantiate(options, null);
	}
}
