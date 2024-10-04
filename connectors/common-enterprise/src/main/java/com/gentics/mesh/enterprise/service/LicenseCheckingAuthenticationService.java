package com.gentics.mesh.enterprise.service;

import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.enterprise.database.connector.AbstractEnterpriseDatabaseConnector;
import com.gentics.mesh.enterprise.service.license.LicenseManagementEndpoint;
import com.gentics.mesh.enterprise.service.license.LicenseService;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.service.AuthenticationService;

import io.vertx.ext.web.RoutingContext;

public class LicenseCheckingAuthenticationService implements AuthenticationService {

	private AbstractEnterpriseDatabaseConnector connector;
	private Set<String> whitelist;

	@Override
	public boolean handle(RoutingContext rc) {
		if (whitelist.stream().noneMatch(allowedPath -> rc.request().path().startsWith(allowedPath))) {
			LicenseService licenseService = connector.getLicenseService();
			if (licenseService != null) {
				if (!licenseService.isLicenseSufficient()) {
					rc.response()
						.setStatusCode(402)
						.end(new GenericMessageResponse("The usage of " + connector.getClass().getSimpleName() + " requires a valid Gentics license. The functionality of Mesh will be limited.")
								//TODO display parameter override
								.toJson(connector.getOptions().getHttpServerOptions().isMinifyJson()));
					return true;
				}
			}
		}
		return false;
	}

	public void init(AbstractEnterpriseDatabaseConnector connector) {
		this.connector = connector;
		this.whitelist = VersionUtils.generateVersionMountpoints().map(root -> root + "/" + LicenseManagementEndpoint.ENDPOINT_ROOT).collect(Collectors.toSet());
	}
}
