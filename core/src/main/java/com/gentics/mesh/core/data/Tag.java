package com.gentics.mesh.core.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;

public interface Tag extends GenericNode {

	void setName(String name);

	String getName();

	List<? extends TagFieldContainer> getFieldContainers();

	TagFamily getTagFamily();

	TagReference tansformToTagReference(TransformationInfo info);

	Tag transformToRest(MeshAuthUser requestUser, Handler<AsyncResult<TagResponse>> resultHandler);

	void removeNode(Node node);

	List<? extends Node> getNodes();

	void remove();

	void delete();

	TagImpl getImpl();

	Page<Node> findTaggedNodes(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);
}
