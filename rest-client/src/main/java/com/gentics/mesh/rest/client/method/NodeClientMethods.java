package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpsertRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * Interface for Node specific REST API methods.
 */
public interface NodeClientMethods {

	/**
	 * Find the node with the given UUID in the project with the given name. The query parameters can be utilized to set the desired language and expand field
	 * settings.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param uuid
	 *            Uuid of the node
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Create a node within the given project. The query parameters determine which language of the node will be returned.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param nodeCreateRequest
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters);

	/**
	 * Create a node within the given project. The query parameters determine which language of the node will be returned. Use the provided uuid for the node.
	 * 
	 * @param uuid
	 *            Uuid for the new node
	 * @param projectName
	 *            Name of the project
	 * @param nodeCreateRequest
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters);

	/**
	 * Create or update a node within the given project.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param uuid
	 *            Uuid for the new node
	 * @param nodeUpsertRequest
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeResponse> upsertNode(String projectName, String uuid, NodeUpsertRequest nodeUpsertRequest, ParameterProvider... parameters);

	/**
	 * Update the node with the given UUID.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param uuid
	 *            Uuid of the node which should be updated
	 * @param nodeUpdateRequest
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest, ParameterProvider... parameters);

	/**
	 * Delete the node with the given UUID. All languages will be deleted.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<Void> deleteNode(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Delete the node with the given language.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param uuid
	 *            Uuid of the node
	 * @param languageTag
	 *            Language to be deleted
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<Void> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters);

	/**
	 * Find all nodes within the project with the given name. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters);

	/**
	 * Find all child nodes of the given node with the given parentNodeUuid. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param parentNodeUuid
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, ParameterProvider... parameters);

	/**
	 * Find all nodes that were tagged by the tag with the given tagUuid. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 *            Name of the project which contains the nodes
	 * @param tagFamilyUuid
	 * @param tagUuid
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, ParameterProvider... parameters);

	/**
	 * Add with the given tagUuid to the node with the given nodeUuid. The query parameters can be used to set language settings.
	 * 
	 * @param projectName
	 *            Name of the project which contains the node
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param tagUuid
	 *            Uuid of the tag
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters);

	/**
	 * Remove a tag with the given tagUuid from the node with the given nodeUuid. The query parameters can be used to set language settings.
	 * 
	 * @param projectName
	 *            Name of the project which contains the node
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param tagUuid
	 *            Uuid of the tag
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<Void> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters);

	/**
	 * Move the given node into the target folder. This operation will also affect the children of the given node. Please also note that it is not possible to
	 * move a node into one of its children. This operation can only be executed within the scope of a single project.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 * @param targetFolderUuid
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<Void> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters);

	/**
	 * Load multiple tags that were assigned to a given node.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Update the assigned tags of the given node using the list of tag references within the request.
	 * 
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param request
	 *            Update request
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid, TagListUpdateRequest request,
		ParameterProvider... parameters);

	/**
	 * Get the publish status of a node
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Get the publish status of a node language
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param languageTag
	 *            Language to get the status for
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
		ParameterProvider... parameters);

	/**
	 * Publish a node and all its languages.
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node to be published
	 * @param parameters
	 * @return
	 */
	MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Publish a node language.
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param languageTag
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters);

	/**
	 * Take a node and all node languages offline.
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<Void> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Take a node language offline.
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param languageTag
	 * @param parameters
	 * @return Mesh request which can be invoked
	 * @deprecated Use {@link #takeNodeLanguageOffline(String, String, String, ParameterProvider...)} instead.
	 */
	@Deprecated
	default MeshRequest<Void> takeNodeLanguage(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters) {
		return takeNodeLanguageOffline(projectName, nodeUuid, languageTag, parameters);
	}

	/**
	 * Take a node language offline.
	 *
	 * @param projectName
	 *            Name of the project
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param languageTag
	 * @param parameters
	 * @return Mesh request which can be invoked
	 */
	MeshRequest<Void> takeNodeLanguageOffline(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters);

}
