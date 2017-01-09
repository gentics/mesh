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
	 * Search nodes in project
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
	 * Search users.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<UserListResponse> searchUsers(String json, ParameterProvider... parameters);

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
	 * Search roles.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<RoleListResponse> searchRoles(String json, ParameterProvider... parameters);

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
	 * Search tags.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagListResponse> searchTags(String json, ParameterProvider... parameters);

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
	 * Search tag families.
	 * 
	 * @param json
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagFamilyListResponse> searchTagFamilies(String json, ParameterProvider... parameters);

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
	 * Search schemas.
	 * 
	 * @param json
	 *            Elasticsearch search request
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaListResponse> searchSchemas(String json, ParameterProvider... parameters);

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
	 * Trigger a reindex action which will rebuild the index for all elements. This is useful when you want to sync the search index after restoring a backup.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeReindex();

}
