package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.enterprise.server.license.LicenseService;
import com.gentics.mesh.service.AuthenticationService;

import io.vertx.ext.web.RoutingContext;

public class LicenseCheckingAuthenticationService implements AuthenticationService {

	private LicenseService licenseService;
	private AbstractEnterpriseDatabaseConnector connector;

	@Override
	public boolean handle(RoutingContext rc) {
		// TODO whitelist routes?
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
		return false;
	}

	public void init(AbstractEnterpriseDatabaseConnector connector, LicenseService licenseService) {
		this.licenseService = licenseService;
		this.connector = connector;
	}
}
