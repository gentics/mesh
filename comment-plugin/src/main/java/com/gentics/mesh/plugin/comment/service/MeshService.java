package com.gentics.mesh.plugin.comment.service;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.plugin.comment.Resources;
import com.gentics.mesh.plugin.comment.dagger.AdminClient;
import com.gentics.mesh.plugin.comment.dagger.UserClient;
import com.gentics.mesh.plugin.comment.error.ForbiddenException;
import com.gentics.mesh.plugin.comment.util.RxUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import javax.inject.Inject;

public class MeshService {
	private final MeshRestClient adminClient;
	private final MeshRestClient userClient;
	private final String projectName;
	private final String projectUuid;
	private final Resources resources;

	@Inject
	public MeshService(@AdminClient MeshRestClient adminClient,
					   @UserClient MeshRestClient userClient,
					   JsonObject project,
					   Resources resources) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.projectName = project.getString("name");
		this.projectUuid = project.getString("uuid");
		this.resources = resources;
	}

	/**
	 * Execute the graphql query.
	 * 
	 * @param queryName
	 * @param variables
	 * @return
	 */
	public Single<JsonObject> graphQl(String queryName, JsonObject variables) {
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(resources.loadQuery(queryName));
		request.setVariables(variables);
		return adminClient.graphql(projectName, request).toSingle()
			.map(GraphQLResponse::getData);
	}

	/**
	 * Checks if the current user has the permission on the node.
	 * Returns an error Completable if the user has no permissions.
	 */
	public Completable requirePermission(String nodeUuid, Permission permission) {
		return getUserPermissions(nodeUuid)
			.doOnSuccess(nodePerm -> requirePermission(nodePerm, permission))
			.ignoreElement();
	}

	/**
	 * Gets the permissions of the current user for a node.
	 */
	public Single<UserPermissionResponse> getUserPermissions(String nodeUuid) {
		return getCurrentUser()
			.flatMap(user -> adminClient.readUserPermissions(user.getUuid(),
				String.format("/projects/%s/nodes/%s", projectUuid, nodeUuid)).toSingle());
	}

	/**
	 * Checks if the current user has the permission on the node.
	 * Throws an {@link ForbiddenException} if the user has no permissions.
	 */
	public void requirePermission(UserPermissionResponse nodePerm, Permission permission) {
		if (!nodePerm.hasPerm(permission)) {
			throw new ForbiddenException();
		}
	}

	/**
	 * Gets the current user from mesh
	 */
	public Single<UserResponse> getCurrentUser() {
		// TODO Cache result for the current request
		return userClient.me().toSingle();
	}

	/**
	 * Makes sure that there is a node with the provided path.
	 * If not, folders are created until the path.
	 */
	public Single<NodeResponse> ensureFolders(String path) {
		return com.gentics.mesh.plugin.common.util.RxUtil.webrootMaybe(adminClient, projectName, path)
			.to(RxUtil.defaultSingle(() -> {
				String[] split = splitLastSegment(path);
				return ensureFolders(split[0])
					.flatMap(parent -> createFolder(parent.getUuid(), split[1]));
			}));
	}

	/**
	 * Creates a folder. The name is used for the slug and the name.
	 */
	private Single<NodeResponse> createFolder(String parentUuid, String folderName) {
		NodeCreateRequest createRequest = new NodeCreateRequest()
			.setLanguage("en")
			.setParentNodeUuid(parentUuid)
			.setSchemaName("folder");
		FieldMap fields = createRequest.getFields();
		fields.put("slug", new StringFieldImpl().setString(folderName));
		fields.put("name", new StringFieldImpl().setString(folderName));
		return adminClient.createNode(projectName, createRequest).toSingle();
	}

	/**
	 * Removes all trailing slashes and then removes the last segment of the provided path
	 */
	private String[] splitLastSegment(String path) {
		// Remove trailing slashes
		if (path.length() > 1) {
			path = path.replaceAll("/*$", "");
		}
		int index = path.lastIndexOf("/");
		return new String[]{
			index == 0 ? "/" : path.substring(0, index),
			path.substring(index + 1)
		};
	}
}
