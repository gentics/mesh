package com.gentics.mesh.enterprise.service;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.enterprise.database.connector.AbstractEnterpriseDatabaseConnector;
import com.gentics.mesh.enterprise.service.license.LicenseManagementEndpoint;
import com.gentics.mesh.rest.InternalEndpoint;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.service.EndpointService;

public class LicenseManagementEndpointService implements EndpointService {

	private AbstractEnterpriseDatabaseConnector connector;
	private LicenseManagementEndpoint licenseEndpoint;

	public void init(AbstractEnterpriseDatabaseConnector connector) {
		this.connector = connector;
	}

	@Override
	public Set<InternalEndpoint> generateEndpoints(RouterStorage routerStorageImpl) {
		licenseEndpoint = new LicenseManagementEndpoint(
				connector.getLicenseService(), routerStorageImpl.getAuthChain(), routerStorageImpl.getLocalConfigApi(), 
				routerStorageImpl.getDb(), connector.getOptions());
		return Collections.singleton(licenseEndpoint);
	}
}
