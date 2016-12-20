package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import org.elasticsearch.index.query.QueryBuilder;

public interface SearchClientMethods {

	/**
	 * Search nodes.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeListResponse> searchNodes(String json, ParameterProvider... parameters);

	/**
	 * Search nodes.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<NodeListResponse> searchNodes(QueryBuilder query, ParameterProvider... parameters) {
		return searchNodes(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search nodes in project
	 *
	 * @param projectName Project Name
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters);

	/**
	 * Search nodes in project
	 *
	 * @param projectName Project Name
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<NodeListResponse> searchNodes(String projectName, QueryBuilder query, ParameterProvider... parameters) {
		return searchNodes(projectName, JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search users.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters);

	/**
	 * Search users.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<UserListResponse> searchUsers(QueryBuilder query, ParameterProvider... parameters) {
		return searchUsers(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search groups.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<GroupListResponse> searchGroups(String json, ParameterProvider... parameters);

	/**
	 * Search groups.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<GroupListResponse> searchGroups(QueryBuilder query, ParameterProvider... parameters) {
		return searchGroups(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search roles.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters);

	/**
	 * Search roles.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<RoleListResponse> searchRoles(QueryBuilder query, ParameterProvider... parameters) {
		return searchRoles(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search projects.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectListResponse> searchProjects(String json, ParameterProvider... parameters);

	/**
	 * Search projects.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<ProjectListResponse> searchProjects(QueryBuilder query, ParameterProvider... parameters) {
		return searchProjects(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search tags.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters);

	/**
	 * Search tags.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<TagListResponse> searchTags(QueryBuilder query, ParameterProvider... parameters) {
		return searchTags(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search tags in project
	 *
	 * @param projectName project name
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters);

	/**
	 * Search tags in project
	 *
	 * @param projectName Project Name
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<TagListResponse> searchTags(String projectName, QueryBuilder query, ParameterProvider... parameters) {
		return searchTags(projectName, JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search tag families.
	 * 
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters);

	/**
	 * Search tag families.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<TagFamilyListResponse> searchTagFamilies(QueryBuilder query, ParameterProvider... parameters) {
		return searchTagFamilies(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search tag families in project
	 *
	 * @param projectName
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters);

	/**
	 * Search tag families in project
	 *
	 * @param projectName Project Name
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, QueryBuilder query, ParameterProvider... parameters) {
		return searchTagFamilies(projectName, JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search schemas.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters);

	/**
	 * Search schemas.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<SchemaListResponse> searchSchemas(QueryBuilder query, ParameterProvider... parameters) {
		return searchSchemas(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Search microschemas.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<MicroschemaListResponse> searchMicroschemas(String json, ParameterProvider... parameters);

	/**
	 * Search microschemas.
	 *
	 * @param query
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	default MeshRequest<MicroschemaListResponse> searchMicroschemas(QueryBuilder query, ParameterProvider... parameters) {
		return searchMicroschemas(JsonUtil.queryToString(query), parameters);
	}

	/**
	 * Load the search queue status.
	 * 
	 * @return
	 */
	MeshRequest<SearchStatusResponse> loadSearchStatus();

	/**
	 * Trigger a reindex action which will rebuild the index for all elements. This is useful when you want to sync the search index after restoring a backup.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeReindex();

}
