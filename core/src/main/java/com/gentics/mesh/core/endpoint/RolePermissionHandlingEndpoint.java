package com.gentics.mesh.core.endpoint;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Abstract endpoint implementation with methods that add routes for getting/granting/revoking role permissions
 */
public abstract class RolePermissionHandlingEndpoint extends AbstractInternalEndpoint {

	protected RolePermissionHandlingEndpoint(String basePath, MeshAuthChainImpl chain) {
		super(basePath, chain);
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
		grantPermissionsEndpoint.exampleRequest(roleExamples.getObjectPermissionGrantRequest(includePublishPermissions));
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
		revokePermissionsEndpoint.exampleRequest(roleExamples.getObjectPermissionRevokeRequest(includePublishPermissions));
		revokePermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(includePublishPermissions), "Updated permissions.");
		revokePermissionsEndpoint.events(ROLE_PERMISSIONS_CHANGED);
		revokePermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam(uuidParameterName);
			crudHandler.handleRevokePermissions(ac, uuid);
		});

		InternalEndpointRoute revokePermissionsEndpointStandard = createRoute();
		revokePermissionsEndpointStandard.path(path);
		revokePermissionsEndpointStandard.addUriParameter(uuidParameterName, "Uuid of the " + typeDescription, uuidParameterExample);
		revokePermissionsEndpointStandard.method(PUT);
		revokePermissionsEndpointStandard.description("Revoke permissions on the " + typeDescription + " from multiple roles.");
		revokePermissionsEndpointStandard.consumes(APPLICATION_JSON);
		revokePermissionsEndpointStandard.produces(APPLICATION_JSON);
		revokePermissionsEndpointStandard.exampleRequest(roleExamples.getObjectPermissionRevokeRequest(includePublishPermissions));
		revokePermissionsEndpointStandard.exampleResponse(OK, roleExamples.getObjectPermissionResponse(includePublishPermissions), "Updated permissions.");
		revokePermissionsEndpointStandard.events(ROLE_PERMISSIONS_CHANGED);
		revokePermissionsEndpointStandard.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam(uuidParameterName);
			crudHandler.handleRevokePermissions(ac, uuid);
		});
	}
}
