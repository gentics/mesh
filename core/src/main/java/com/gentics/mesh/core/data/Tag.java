package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.util.InvalidArgumentException;

public interface Tag extends GenericVertex<TagResponse>, NamedNode {

	public static final String TYPE = "tag";

	List<? extends TagFieldContainer> getFieldContainers();

	/**
	 * Return the tag family to which the tag belongs.
	 * 
	 * @return
	 */
	TagFamily getTagFamily();

	TagReference tansformToTagReference();

	void removeNode(Node node);

	/**
	 * Delete the tag.
	 */
	void remove();

	/**
	 * Return a list of nodes that were tagged by this tag.
	 * 
	 * @return
	 */
	List<? extends Node> getNodes();

	/**
	 * Return a page of nodes that are visible to the user and which are tagged by this tag. Use the paging and language information provided.
	 * 
	 * @param requestUser
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 * @throws InvalidArgumentException 
	 */
	Page<? extends Node> findTaggedNodes(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) throws InvalidArgumentException;
}
