package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;

public interface NodeClientMethods {

	Future<NodeResponse> findNodeByUuid(String uuid);

	Future<NodeResponse> createNode(NodeCreateRequest nodeCreateRequest);

	Future<NodeListResponse> findNodes(String projectName, String name);

	Future<NodeListResponse> findNodes();

	Future<TagListResponse> findTagsForNode(String nodeUuid);

	Future<GenericMessageResponse> deleteNode(String uuid);

}
