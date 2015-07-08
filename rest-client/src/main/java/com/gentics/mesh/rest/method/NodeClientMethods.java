package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;

public interface NodeClientMethods {

	Future<NodeResponse> findNodeByUuid(String projectName, String uuid, NodeRequestParameters parameters);

	Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest);

	Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest, NodeRequestParameters parameters);

	Future<GenericMessageResponse> deleteNode(String projectName, String uuid);

	Future<NodeListResponse> findNodes(String projectName, PagingInfo pagingInfo);

	// Relations

	Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, PagingInfo pagingInfo, NodeRequestParameters nodeRequestParameters);

	Future<NodeResponse> findNodeByUuid(String projectName, String uuid);

	Future<NodeListResponse> findNodesForTag(String projectName, String tagUuid);

	Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, NodeRequestParameters nodeRequestParameters);

	Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, NodeRequestParameters nodeRequestParameters);
}
