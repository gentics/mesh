package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.transformation.TransformationParameters;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;

public interface Tag extends GenericNode<TagResponse> {

	void setName(String name);

	String getName();

	List<? extends TagFieldContainer> getFieldContainers();

	TagFamily getTagFamily();

	TagReference tansformToTagReference(TransformationParameters info);

	void removeNode(Node node);

	List<? extends Node> getNodes();

	void remove();

	void delete();

	TagImpl getImpl();

	Page<Node> findTaggedNodes(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo);
}
