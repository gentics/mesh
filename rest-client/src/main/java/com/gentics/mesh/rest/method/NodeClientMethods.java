package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;

public interface NodeClientMethods {

	Future<NodeResponse> findNodeByUuid(String projectName, String uuid, NodeRequestParameters parameters);

	Future<NodeResponse> createNode(NodeCreateRequest nodeCreateRequest);

	Future<GenericMessageResponse> deleteNode(String uuid);

	Future<NodeListResponse> findNodes(String projectName, PagingInfo pagingInfo);

	// Relations

	Future<TagListResponse> findTagsForNode(String nodeUuid, PagingInfo pagingInfo);

	Future<NodeListResponse> findNodeChildren(String parentNodeUuid, PagingInfo pagingInfo);

	Future<NodeResponse> findNodeByUuid(String projectName, String uuid);

}
