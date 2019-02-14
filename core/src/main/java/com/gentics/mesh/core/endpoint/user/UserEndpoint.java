package com.gentics.mesh.core.endpoint.user;

import static com.gentics.mesh.example.ExampleUuids.USER_EDITOR_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class UserEndpoint extends AbstractInternalEndpoint {

	private UserCrudHandler crudHandler;

	private UserTokenAuthHandler userTokenHandler;

	public UserEndpoint() {
		super("users", null);
	}

	@Inject
	public UserEndpoint(MeshAuthChain chain, UserCrudHandler userCrudHandler, UserTokenAuthHandler userTokenHandler) {
		super("users", chain);
		this.crudHandler = userCrudHandler;
		this.userTokenHandler = userTokenHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of users.";
	}

	@Override
	public void registerEndPoints() {
		addUpdateHandler();
		secureAll();

		addCreateHandler();
		addReadHandler();
		addDeleteHandler();
		addResetTokenHandler();
		addAPITokenHandler();
		addReadPermissionHandler();
	}

	private void addAPITokenHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:userUuid/token");
		endpoint.setRAMLPath("/{userUuid}/token");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		endpoint.description(
			"Return API token which can be used to authenticate the user. Store the key somewhere save since you won't be able to retrieve it later on.");
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, userExamples.getAPIKeyResponse(), "The User API token response.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleIssueAPIToken(ac, uuid);
		});

		InternalEndpointRoute deleteEndpoint = createRoute();
		deleteEndpoint.path("/:userUuid/token");
		deleteEndpoint.setRAMLPath("/{userUuid}/token");
		deleteEndpoint.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		deleteEndpoint.description("Invalidate the issued API token.");
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Message confirming the invalidation of the API token.");
		deleteEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleDeleteAPIToken(ac, uuid);
		});
	}

	private void addReadPermissionHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		endpoint.setRAMLPath("/{userUuid}/permissions/{path}");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		endpoint.addUriParameter("path", "Path to the element from which the permissions should be loaded.", "projects/:projectUuid/schemas");
		endpoint.description("Read the user permissions on the element that can be located by the specified path.");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, userExamples.getUserPermissionResponse(), "Response which contains the loaded permissions.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String userUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, userUuid, pathToElement);
		});
	}

	private void addResetTokenHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:userUuid/reset_token");
		endpoint.setRAMLPath("/{userUuid}/reset_token");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		endpoint.description("Return a one time token which can be used by any user to update a user (e.g.: Reset the password)");
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, userExamples.getTokenResponse(), "User token response.");
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleFetchToken(ac, uuid);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:userUuid");
		readOne.description("Read the user with the given uuid");
		readOne.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, userExamples.getUserResponse1("jdoe"), "User response which may also contain an expanded node.");
		readOne.addQueryParameters(NodeParametersImpl.class);
		readOne.addQueryParameters(VersioningParametersImpl.class);
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.addQueryParameters(GenericParametersImpl.class);
		readOne.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all users when no parameter was specified
		 */
		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.description("Load multiple users and return a paged list response.");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, userExamples.getUserListResponse(), "User list response which may also contain an expanded node references.");
		readAll.addQueryParameters(NodeParametersImpl.class);
		readAll.addQueryParameters(VersioningParametersImpl.class);
		readAll.addQueryParameters(RolePermissionParametersImpl.class);
		readAll.addQueryParameters(GenericParametersImpl.class);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:userUuid");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		endpoint.method(DELETE);
		endpoint.description(
			"Deactivate the user with the given uuid. Please note that users can't be deleted since they are needed to construct creator/editor information.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "User was deactivated.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {

		// Add the user token handler first in order to allow for recovery token handling
		getRouter().route("/:userUuid").method(POST).handler(userTokenHandler);
		// Chain the regular auth handler afterwards in order to handle non-token code requests
		if (chain != null) {
			chain.secure(getRouter().route("/:userUuid").method(POST));
		}

		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:userUuid");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", USER_EDITOR_UUID);
		endpoint.description("Update the user with the given uuid. The user is created if no user with the specified uuid could be found.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(UserParametersImpl.class);
		endpoint.exampleRequest(userExamples.getUserUpdateRequest("jdoe42"));
		endpoint.exampleResponse(OK, userExamples.getUserResponse1("jdoe42"), "Updated user response.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.description("Create a new user.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(userExamples.getUserCreateRequest("newuser"));
		endpoint.exampleResponse(CREATED, userExamples.getUserResponse1("newuser"), "User response of the created user.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		});
	}
}
