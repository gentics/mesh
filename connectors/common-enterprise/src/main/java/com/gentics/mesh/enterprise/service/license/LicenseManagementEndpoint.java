package com.gentics.mesh.enterprise.service.license;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import java.util.Collections;
import java.util.Optional;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class LicenseManagementEndpoint extends AbstractInternalEndpoint {

	public static final String ENDPOINT_ROOT = "license";

	private final LicenseService licenseService;

	public LicenseManagementEndpoint(LicenseService licenseService, MeshAuthChain chain, LocalConfigApi localConfigApi,
			Database db, MeshOptions options) {
		super(ENDPOINT_ROOT, chain, localConfigApi, db, options);
		this.licenseService = licenseService;
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		InternalEndpointRoute readAllEndpoint = createRoute();
		readAllEndpoint.path("/");
		readAllEndpoint.method(GET);
		readAllEndpoint.description("Gets current license info.");
		readAllEndpoint.produces(APPLICATION_JSON);
		readAllEndpoint.exampleResponse(OK, new LicenseResponse().setFeatures(Collections.singleton("Mesh SQL")).setStatus("VALID").setLicenseKey("ABCD-****-****-****-EFGH"), "License info response.");
		readAllEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			Optional.ofNullable(licenseService.getLicenseInfo()).ifPresentOrElse(
					license -> ac.send(license, OK), 
					() -> rc.response().setStatusCode(404).end(new GenericMessageResponse("No license found.").toJson(ac.isMinify(options.getHttpServerOptions()))));
		}, false);
	}

	@Override
	public String getDescription() {
		return "License management endpoint";
	}

}
