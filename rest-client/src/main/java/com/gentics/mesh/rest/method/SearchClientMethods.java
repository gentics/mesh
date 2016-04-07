package com.gentics.mesh.rest.method;

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
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface SearchClientMethods {

	/**
	 * Search nodes.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<NodeListResponse> searchNodes(String json, QueryParameterProvider... parameters);

	/**
	 * Search nodes in project
	 *
	 * @param projectName Project Name
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<NodeListResponse> searchNodes(String projectName, String json, QueryParameterProvider... parameters);

	/**
	 * Search users.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<UserListResponse> searchUsers(String json, QueryParameterProvider... parameters);

	/**
	 * Search groups.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<GroupListResponse> searchGroups(String json, QueryParameterProvider... parameters);

	/**
	 * Search roles.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<RoleListResponse> searchRoles(String json, QueryParameterProvider... parameters);

	/**
	 * Search projects.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<ProjectListResponse> searchProjects(String json, QueryParameterProvider... parameters);

	/**
	 * Search tags.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<TagListResponse> searchTags(String json, QueryParameterProvider... parameters);

	/**
	 * Search tags in project
	 *
	 * @param projectName project name
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<TagListResponse> searchTags(String projectName, String json, QueryParameterProvider... parameters);

	/**
	 * Search tag families.
	 * 
	 * @param json
	 * @param parameters
	 * @return
	 */
	Future<TagFamilyListResponse> searchTagFamilies(String json, QueryParameterProvider... parameters);

	/**
	 * Search tag families in project
	 *
	 * @param projectName
	 * @param json
	 * @param parameters
	 * @return
	 */
	Future<TagFamilyListResponse> searchTagFamilies(String projectName, String json, QueryParameterProvider... parameters);

	/**
	 * Search schemas.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<SchemaListResponse> searchSchemas(String json, QueryParameterProvider... parameters);

	/**
	 * Search microschemas.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	Future<MicroschemaListResponse> searchMicroschemas(String json, QueryParameterProvider... parameters);

	/**
	 * Load the search queue status.
	 * 
	 * @return
	 */
	Future<SearchStatusResponse> loadSearchStatus();

	/**
	 * Trigger a reindex action which will rebuild the index for all elements. This is useful when you want to sync the search index after restoring a backup.
	 * 
	 * @return
	 */
	Future<GenericMessageResponse> invokeReindex();

}
