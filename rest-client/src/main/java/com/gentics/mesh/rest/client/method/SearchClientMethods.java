package com.gentics.mesh.rest.client.method;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

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
	 * Search for nodes across all projects and return the raw search response.
	 * 
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<ObjectNode> searchNodesRaw(String json, ParameterProvider... parameters);

	/**
	 * Search for nodes in the project.
	 *
	 * @param projectName
	 *            Project Name
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeListResponse> searchNodes(String projectName, String json, ParameterProvider... parameters);

	/**
	 * Search for nodes in the project and return the raw response of the search engine.
	 * 
	 * @param projectName
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<ObjectNode> searchNodesRaw(String projectName, String json, ParameterProvider... parameters);

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
	 * Search users and return the raw search response.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchUsersRaw(String json);

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
	 * Search groups and return the raw response of the search engine.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchGroupsRaw(String json);

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
	 * Search for roles and return the raw search response.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchRolesRaw(String json);

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
	 * Search for projects and return the raw search response.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchProjectsRaw(String json);

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
	 * Search tags and return the raw search response.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchTagsRaw(String json);

	/**
	 * Search tags in project
	 *
	 * @param projectName
	 *            project name
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagListResponse> searchTags(String projectName, String json, ParameterProvider... parameters);

	/**
	 * Search for tags in the project and return the raw search response.
	 * 
	 * @param projectName
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchTagsRaw(String projectName, String json);

	/**
	 * Search tag families.
	 * 
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters);

	/**
	 * Search tag families and return the raw search response.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchTagFamiliesRaw(String json);

	/**
	 * Search tag families in project.
	 *
	 * @param projectName
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagFamilyListResponse> searchTagFamilies(String projectName, String json, ParameterProvider... parameters);

	/**
	 * Search tag families in project and return the raw response.
	 * 
	 * @param projectName
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchTagFamiliesRaw(String projectName, String json);

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
	 * Search schemas and and return the raw search response.
	 * 
	 * @param json
	 * @return
	 */
	MeshRequest<ObjectNode> searchSchemasRaw(String json);

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
	 * Search microschemas and return the raw search response.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @return
	 */
	MeshRequest<ObjectNode> searchMicroschemasRaw(String json);

	/**
	 * Clear all search indices by removing and re-creating them.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeIndexClear();

	/**
	 * Trigger the index sync action which will synchronize the index for all elements. This is useful when you want to sync the search index after restoring a
	 * backup.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeIndexSync();

	/**
	 * Return the elasticsearch status. This will also contain information about the progress of running index sync operations.
	 * 
	 * @return
	 */
	MeshRequest<SearchStatusResponse> searchStatus();
}
