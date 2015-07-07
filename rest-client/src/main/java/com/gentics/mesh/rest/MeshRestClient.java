package com.gentics.mesh.rest;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public class MeshRestClient extends AbstractMeshRestClient {

	public MeshRestClient(String host) {
		this(host, DEFAULT_PORT);
	}

	public MeshRestClient(String host, int port) {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost(host);
		options.setDefaultPort(port);
		client = Vertx.vertx().createHttpClient(options);
	}

	@Override
	public Future<NodeResponse> findNodeByUuid(String projectName, String uuid, NodeRequestParameters parameters) {
		String uri = "/" + projectName + "/nodes/" + uuid;
		if (parameters != null) {
			uri += parameters.getQuery();
		}
		System.out.println(uri);
		return handleRequest(GET, uri, NodeResponse.class);
	}

	@Override
	public Future<NodeResponse> createNode(NodeCreateRequest nodeCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> deleteNode(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<NodeListResponse> findNodes(String projectName, PagingInfo pagingInfo) {
		String params = "";
		if (pagingInfo != null) {
			params += "?per_page=" + pagingInfo.getPerPage() + "&page=" + pagingInfo.getPage();
		}
		return handleRequest(GET, "/" + projectName + "/nodes" + params, NodeListResponse.class);
	}

	@Override
	public Future<TagListResponse> findTagsForNode(String nodeUuid, PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<NodeListResponse> findNodeChildren(String parentNodeUuid, PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagResponse> createTag(TagCreateRequest tagCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagResponse> findTagByUuid(String projectName, String uuid) {
		return handleRequest(GET, "/" + projectName + "/tags/" + uuid, TagResponse.class);
	}

	@Override
	public Future<TagResponse> updateTag(String projectName, String uuid, TagUpdateRequest tagUpdateRequest) {
		return handleRequest(PUT, "/" + projectName + "/tags/" + uuid, TagResponse.class, tagUpdateRequest);
	}

	@Override
	public Future<GenericMessageResponse> deleteTag(String projectName, String uuid) {
		return handleRequest(DELETE, "/" + projectName + "/tags/" + uuid, GenericMessageResponse.class);
	}

	@Override
	public Future<TagListResponse> findTags(String projectName, PagingInfo pagingInfo) {
		String params = "?per_page=" + pagingInfo.getPerPage() + "&page=" + pagingInfo.getPage();
		return handleRequest(GET, "/" + projectName + "/tags" + params, TagListResponse.class);
	}

	@Override
	public Future<TagResponse> findTagByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<NodeListResponse> findNodesForTag(String tagUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ProjectResponse> findProjectByUuid(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ProjectListResponse> findProjects(PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ProjectResponse> createProject(ProjectCreateRequest projectCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<ProjectResponse> updateProject(ProjectUpdateRequest projectUpdateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> deleteProject(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagFamilyResponse> findTagFamilyByUuid(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagFamilyListResponse> findTagFamilies(String projectName, PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagFamilyResponse> createTagFamily(String project, TagFamilyCreateRequest tagFamilyCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> deleteTagFamily(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<TagFamilyResponse> updateTagFamily(TagFamilyUpdateRequest tagFamilyUpdateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GroupResponse> findGroupByUuid(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GroupListResponse> findGroups(PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GroupResponse> updateGroup(GroupUpdateRequest groupUpdateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> deleteGroup(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<UserResponse> findUserByUuid(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<UserResponse> findUserByUsername(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<UserListResponse> findUsers(PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<UserResponse> createUser(UserCreateRequest userCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<UserResponse> updateUser(UserUpdateRequest userUpdateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> deleteUser(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RoleResponse> findRoleByUuid(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RoleListResponse> findRoles(PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RoleResponse> createRole(RoleCreateRequest roleCreateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<GenericMessageResponse> deleteRole(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<UserResponse> login() {

		MeshResponseHandler<UserResponse> meshHandler = new MeshResponseHandler<>(UserResponse.class, this);
		meshHandler.handle(rh -> {
			if (rh.statusCode() == 200) {
				setCookie(rh.headers().get("Set-Cookie"));
			}
		});
		HttpClientRequest request = client.get(BASEURI + "/auth/me", meshHandler);
		request.headers().add("Authorization", "Basic " + authEnc);
		request.headers().add("Accept", "application/json");
		request.end();
		return meshHandler.getFuture();

	}

	@Override
	public Future<UserResponse> me() {
		return handleRequest(GET, BASEURI + "auth/me", UserResponse.class);
	}

	@Override
	public Future<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<NodeResponse> findNodeByUuid(String projectName, String uuid) {
		return findNodeByUuid(projectName, uuid, null);
	}

	//
	// @Override
	// public Future<TagResponse> createTag(TagCreateRequest tagCreateRequest) {
	// Future<TagResponse> future = Future.future();
	//
	// Map<String, String> extraHeaders = new HashMap<>();
	// Buffer buffer = Buffer.buffer();
	// buffer.appendString(JsonUtil.toJson(tagCreateRequest));
	// extraHeaders.put("content-length", String.valueOf(buffer.length()));
	// extraHeaders.put("content-type", "application/json");
	//
	// HttpClientRequest request = client.post(BASEURI + "/project/tags", rh -> {
	// rh.bodyHandler(bh -> {
	// if (rh.statusCode() == 200) {
	// String json = bh.toString();
	// try {
	// TagResponse tagResponse = JsonUtil.readValue(json, TagResponse.class);
	// future.complete(tagResponse);
	// } catch (Exception e) {
	// future.fail(e);
	// }
	// } else {
	// future.fail("Could not fetch tag:" + rh.statusCode());
	// }
	// });
	// });
	//
	// return future;
	// }
	//
	// @Override
	// public Future<TagResponse> findTag(String uuid) {
	// Future<TagResponse> future = Future.future();
	// HttpClientRequest request = client.get(BASEURI + "/tags", rh -> {
	// rh.bodyHandler(bh -> {
	//
	// });
	// System.out.println("Received response with status code " + rh.statusCode());
	// });
	//
	// request.exceptionHandler(e -> {
	// System.out.println("Received exception: " + e.getMessage());
	// e.printStackTrace();
	// });
	// return future;
	// }
}
