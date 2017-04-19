package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

@Singleton
public class UserEndpoint extends AbstractEndpoint {

	private UserCrudHandler crudHandler;

	private UserTokenAuthHandler userTokenHandler;

	public UserEndpoint() {
		super("users", null);
	}

	@Inject
	public UserEndpoint(RouterStorage routerStorage, UserCrudHandler userCrudHandler, UserTokenAuthHandler userTokenHandler) {
		super("users", routerStorage);
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
		addAuthTokenHandler();
		addAPIKeyHandler();
		addReadPermissionHandler();
	}

	private void addAPIKeyHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:userUuid/apiKey");
		endpoint.setRAMLPath("/{userUuid}/apiKey");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		endpoint.description("Return API Key which can be used to authenticate the user. Store the key somewhere save since you won't be able to retrieve it later on.");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, userExamples.getAPIKeyResponse(), "The User API Key response.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleIssueAPIKey(ac, uuid);
		});

		Endpoint deleteEndpoint = createEndpoint();
		deleteEndpoint.path("/:userUuid/apiKey");
		deleteEndpoint.setRAMLPath("/{userUuid}/apiKey");
		deleteEndpoint.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		deleteEndpoint.description("Invalidate the issued API key.");
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Message confirming the invalidation of the API key.");
		deleteEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleDeleteAPIKey(ac, uuid);
		});
	}

	private void addReadPermissionHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.pathRegex("\\/([^\\/]*)\\/permissions\\/(.*)");
		endpoint.setRAMLPath("/{userUuid}/permissions/{path}");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("path", "Path to the element from which the permissions should be loaded.", "projects/:projectUuid/schemas");
		endpoint.description("Read the user permissions on the element/s that are located by the specified path.");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, userExamples.getUserPermissionResponse(), "Response which contains the loaded permissions.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String userUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			crudHandler.handlePermissionRead(ac, userUuid, pathToElement);
		});
	}

	private void addAuthTokenHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:userUuid/token");
		endpoint.setRAMLPath("/{userUuid}/token");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		endpoint.description("Return a one time token which can be used by any user to update a user (e.g.: Reset the password)");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, userExamples.getTokenResponse(), "User token response.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleFetchToken(ac, uuid);
		});
	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:userUuid");
		readOne.description("Read the user with the given uuid");
		readOne.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, userExamples.getUserResponse1("jdoe"), "User response which may also contain an expanded node.");
		readOne.addQueryParameters(NodeParametersImpl.class);
		readOne.addQueryParameters(VersioningParametersImpl.class);
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all users when no parameter was specified
		 */
		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.description("Load multiple users and return a paged list response.");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, userExamples.getUserListResponse(), "User list response which may also contain an expanded node references.");
		readAll.addQueryParameters(NodeParametersImpl.class);
		readAll.addQueryParameters(VersioningParametersImpl.class);
		readAll.addQueryParameters(RolePermissionParametersImpl.class);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:userUuid");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description(
				"Deactivate the user with the given uuid. Please note that users can't be deleted since they are needed to construct creator/editor information.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "User was deactivated.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {

		// Add the user token handler first in order to allow for recovery token handling 
		getRouter().route("/:userUuid").method(POST).handler(userTokenHandler);
		// Chain the regular auth handler afterwards in order to handle non-token code requests
		getRouter().route("/:userUuid").method(POST).handler(authHandler);

		Endpoint endpoint = createEndpoint();
		endpoint.path("/:userUuid");
		endpoint.addUriParameter("userUuid", "Uuid of the user.", UUIDUtil.randomUUID());
		endpoint.description("Update the user with the given uuid.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(UserParametersImpl.class);
		endpoint.exampleRequest(userExamples.getUserUpdateRequest("jdoe42"));
		endpoint.exampleResponse(OK, userExamples.getUserResponse1("jdoe42"), "Updated user response.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("userUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.description("Create a new user.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(userExamples.getUserCreateRequest("newuser"));
		endpoint.exampleResponse(CREATED, userExamples.getUserResponse1("newuser"), "User response of the created user.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleCreate(ac);
		});
	}
}
