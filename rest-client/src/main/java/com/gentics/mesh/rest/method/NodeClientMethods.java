package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;

public interface NodeClientMethods {

	Future<NodeResponse> findNodeByUuid(String projectName, String uuid, QueryParameterProvider... parameters);

	Future<NodeResponse> createNode(String projectName, NodeCreateRequest nodeCreateRequest, QueryParameterProvider... parameters);

	Future<NodeResponse> updateNode(String projectName, String uuid, NodeUpdateRequest nodeUpdateRequest, QueryParameterProvider... parameters);

	Future<GenericMessageResponse> deleteNode(String projectName, String uuid);

	Future<NodeListResponse> findNodes(String projectName, QueryParameterProvider... parameters);

	// Relations

	Future<NodeListResponse> findNodeChildren(String projectName, String parentNodeUuid, QueryParameterProvider... parameters);

	Future<NodeListResponse> findNodesForTag(String projectName, String tagUuid, QueryParameterProvider... parameters);

	Future<NodeResponse> addTagToNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters);

	Future<NodeResponse> removeTagFromNode(String projectName, String nodeUuid, String tagUuid, QueryParameterProvider... parameters);
}
