package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * Interface for Node specific rest API methods.
 */
public interface NodeClientMethods {

	/**
	 * Find the node with the given UUID in the project with the given name. The query parameters can be utilized to set the desired language and expand field
	 * settings.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeResponse> findNodeByUuid(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Create a node within the given project. The query parameters determine which language of the node will be returned.
	 * 
	 * @param projectName
	 * @param nodeCreateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters);

	/**
	 * Create a node within the given project. The query parameters determine which language of the node will be returned. Use the provided uuid for the node.
	 * 
	 * @param uuid
	 * @param projectName
	 * @param nodeCreateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeResponse> createNode(String uuid, String projectName, NodeCreateRequest nodeCreateRequest, ParameterProvider... parameters);

	/**
	 * Update the node with the given UUID.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param nodeUpdateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest, ParameterProvider... parameters);

	/**
	 * Delete the node with the given UUID. All languages will be deleted.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<Void> deleteNode(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Delete the node with the given language.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param languageTag
	 * @param parameters
	 * @return
	 */
	MeshRequest<Void> deleteNode(String projectName, String uuid, String languageTag, ParameterProvider... parameters);

	/**
	 * Find all nodes within the project with the given name. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeListResponse> findNodes(String projectName, ParameterProvider... parameters);

	/**
	 * Find all child nodes of the given node with the given parentNodeUuid. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 * @param parentNodeUuid
	 * @param parameters
	 * @return
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
	 * @return
	 */
	MeshRequest<NodeListResponse> findNodesForTag(String projectName, String tagFamilyUuid, String tagUuid, ParameterProvider... parameters);

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
	MeshRequest<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters);

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
	MeshRequest<Void> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, ParameterProvider... parameters);

	/**
	 * Move the given node into the target folder. This operation will also affect the children of the given node. Please also note that it is not possible to
	 * move a node into one of its children. This operation can only be executed within the scope of a single project.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param targetFolderUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<Void> moveNode(String projectName, String nodeUuid, String targetFolderUuid, ParameterProvider... parameters);

	/**
	 * Load multiple tags that were assigned to a given node.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagListResponse> findTagsForNode(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Update the assigned tags of the given node using the list of tag references within the request.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<TagListResponse> updateTagsForNode(String projectName, String nodeUuid, TagListUpdateRequest request,
			ParameterProvider... parameters);

	/**
	 * Get the publish status of a node
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<PublishStatusResponse> getNodePublishStatus(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Get the publish status of a node language
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param languageTag
	 * @param parameters
	 * @return
	 */
	MeshRequest<PublishStatusModel> getNodeLanguagePublishStatus(String projectName, String nodeUuid, String languageTag,
			ParameterProvider... parameters);

	/**
	 * Publish a node.
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<PublishStatusResponse> publishNode(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Publish a node language.
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param languageTag
	 * @param parameters
	 * @return
	 */
	MeshRequest<PublishStatusModel> publishNodeLanguage(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters);

	/**
	 * Take a node and all node languages offline.
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<Void> takeNodeOffline(String projectName, String nodeUuid, ParameterProvider... parameters);

	/**
	 * Take a node language offline.
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param languageTag
	 * @param parameters
	 * @return
	 */
	MeshRequest<Void> takeNodeLanguage(String projectName, String nodeUuid, String languageTag, ParameterProvider... parameters);

}
