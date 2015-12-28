package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeBreadcrumbResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

/**
 * Interface for Node specific rest API methods.
 */
public interface NodeClientMethods {

	/**
	 * Find the node with the given uuid in the project with the given name. The query parameters can be utilized to set the desired language and expand field
	 * settings.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<NodeResponse> findNodeByUuid(String projectName, String uuid, QueryParameterProvider... parameters);

	/**
	 * Create a node within the given project. The query parameters determine which language of the node will be returned.
	 * 
	 * @param projectName
	 * @param nodeCreateRequest
	 * @param parameters
	 * @return
	 */
	Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, QueryParameterProvider... parameters);

	/**
	 * Update the node with the given uuid.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param nodeUpdateRequest
	 * @param parameters
	 * @return
	 */
	Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest, QueryParameterProvider... parameters);

	/**
	 * Delete the node with the given uuid. All languages will be deleted.
	 * 
	 * @param projectName
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteNode(String projectName, String uuid);

	/**
	 * Delete the node with the given language.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param languageTag
	 * @return
	 */
	Future<GenericMessageResponse> deleteNode(String projectName, String uuid, String languageTag);

	/**
	 * Find all nodes within the project with the given name. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	Future<NodeListResponse> findNodes(String projectName, QueryParameterProvider... parameters);

	// Relations

	/**
	 * Find all child nodes of the given node with the given parentNodeUuid. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 * @param parentNodeUuid
	 * @param parameters
	 * @return
	 */
	Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, QueryParameterProvider... parameters);

	/**
	 * Find all nodes that were tagged by the tag with the given tagUuid. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 *            Name of the project which contains the nodes
	 * @param tagUuid
	 * @param parameters
	 * @return
	 */
	Future<NodeListResponse> findNodesForTag(String projectName, String tagUuid, QueryParameterProvider... parameters);

	/**
	 * Add with the given tagUuid to the node with the given nodeUuid. The query parameters can be used to set language settings.
	 * 
	 * @param projectName
	 *            Name of the project which contains the node
	 * 
	 * @param nodeUuid
	 * @param tagUuid
	 * @param parameters
	 * @return
	 */
	Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters);

	/**
	 * Remove a tag with the given tagUuid from the node with the given nodeUuid. The query parameters can be used to set language settings.
	 * 
	 * @param projectName
	 *            Name of the project which contains the node
	 * 
	 * @param nodeUuid
	 * @param tagUuid
	 * @param parameters
	 * @return
	 */
	Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters);

	/**
	 * Move the given node into the target folder. This operation will also affect the children of the given node. Please also note that it is not possible to
	 * move a node into one of its children. This operation can only be executed within the scope of a single project.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param targetFolderUuid
	 * @return
	 */
	Future<GenericMessageResponse> moveNode(String projectName, String nodeUuid, String targetFolderUuid);

	/**
	 * Load the breadcrumb for the given node.
	 * 
	 * @param projectName
	 *            Name of the project which contains the node
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param parameters
	 * @return Future with the breadcrumb response
	 */
	Future<NodeBreadcrumbResponse> loadBreadcrumb(String projectName, String nodeUuid, QueryParameterProvider... parameters);

	/**
	 * Load multiple tags that were assigned to a given node.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param parameters
	 * @return
	 */
	Future<TagListResponse> findTagsForNode(String projectName, String nodeUuid, QueryParameterProvider... parameters);

}
