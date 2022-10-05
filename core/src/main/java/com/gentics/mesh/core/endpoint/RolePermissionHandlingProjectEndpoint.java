package com.gentics.mesh.core.endpoint;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

public abstract class RolePermissionHandlingProjectEndpoint extends AbstractProjectEndpoint {

	protected RolePermissionHandlingProjectEndpoint(String basePath, MeshAuthChainImpl chain,
			BootstrapInitializer boot) {
		super(basePath, chain, boot);
	}

	/**
	 * Add role permission handler
	 * @param uuidParameterName name of the uuid parameter (e.g. "groupUuid")
	 * @param uuidParameterExample example of the uuid parameter
	 * @param typeDescription description of the object type (e.g. "group")
	 * @param crudHandler crud handler
	 * @param includePublishPermissions true to include the publish permissions into the example
	 */
	protected void addRolePermissionHandler(String uuidParameterName, String uuidParameterExample, String typeDescription,
			AbstractCrudHandler<?, ?> crudHandler, boolean includePublishPermissions) {
		String path = "/:" + uuidParameterName + "/rolePermissions";
		InternalEndpointRoute readPermissionsEndpoint = createRoute();
		readPermissionsEndpoint.path(path);
		readPermissionsEndpoint.addUriParameter(uuidParameterName, "Uuid of the " + typeDescription, uuidParameterExample);
		readPermissionsEndpoint.method(GET);
		readPermissionsEndpoint.description("Get the permissions on the " + typeDescription + " for all roles.");
		readPermissionsEndpoint.produces(APPLICATION_JSON);
		readPermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(includePublishPermissions), "Loaded permissions.");
		readPermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam(uuidParameterName);
			crudHandler.handleReadPermissions(ac, uuid);
		}, false);

		InternalEndpointRoute grantPermissionsEndpoint = createRoute();
		grantPermissionsEndpoint.path(path);
		grantPermissionsEndpoint.addUriParameter(uuidParameterName, "Uuid of the " + typeDescription, uuidParameterExample);
		grantPermissionsEndpoint.method(POST);
		grantPermissionsEndpoint.description("Grant permissions on the " + typeDescription + " to multiple roles.");
		grantPermissionsEndpoint.consumes(APPLICATION_JSON);
		grantPermissionsEndpoint.produces(APPLICATION_JSON);
		grantPermissionsEndpoint.exampleRequest((String)null); // TODO
		grantPermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(includePublishPermissions), "Updated permissions.");
		grantPermissionsEndpoint.events(ROLE_PERMISSIONS_CHANGED);
		grantPermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam(uuidParameterName);
			crudHandler.handleGrantPermissions(ac, uuid);
		});

		InternalEndpointRoute revokePermissionsEndpoint = createRoute();
		revokePermissionsEndpoint.path(path);
		revokePermissionsEndpoint.addUriParameter(uuidParameterName, "Uuid of the " + typeDescription, uuidParameterExample);
		revokePermissionsEndpoint.method(DELETE);
		revokePermissionsEndpoint.description("Revoke permissions on the " + typeDescription + " from multiple roles.");
		revokePermissionsEndpoint.consumes(APPLICATION_JSON);
		revokePermissionsEndpoint.produces(APPLICATION_JSON);
		revokePermissionsEndpoint.exampleRequest((String)null); // TODO
		revokePermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(includePublishPermissions), "Updated permissions.");
		revokePermissionsEndpoint.events(ROLE_PERMISSIONS_CHANGED);
		revokePermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam(uuidParameterName);
			crudHandler.handleRevokePermissions(ac, uuid);
		});
	}
}
