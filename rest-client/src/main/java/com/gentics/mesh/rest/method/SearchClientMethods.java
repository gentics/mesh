package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;

import io.vertx.core.Future;

public interface SearchClientMethods {

	Future<NodeListResponse> searchNodes(String json, QueryParameterProvider... parameters);

	Future<UserListResponse> searchUsers(String json, QueryParameterProvider... parameters);

	Future<GroupListResponse> searchGroups(String json, QueryParameterProvider... parameters);

	Future<RoleListResponse> searchRoles(String json, QueryParameterProvider... parameters);

	Future<ProjectListResponse> searchProject(String json, QueryParameterProvider... parameters);

	Future<TagListResponse> searchTags(String json, QueryParameterProvider... parameters);

	Future<TagFamilyListResponse> searchTagFamilies(String json, QueryParameterProvider... parameters);

	Future<SchemaListResponse> searchSchemas(String json, QueryParameterProvider... parameters);

	Future<MicroschemaListResponse> searchMicroschemas(String json, QueryParameterProvider... parameters);

}
