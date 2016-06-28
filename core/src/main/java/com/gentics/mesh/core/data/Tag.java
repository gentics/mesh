package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.InvalidArgumentException;

/**
 * Graph Domain Model interface for a tag.
 * 
 * Tags can currently only hold a single string value. Tags are not localizable. A tag can only be assigned to a single tag family.
 */
public interface Tag extends MeshCoreVertex<TagResponse, Tag>, ReferenceableElement<TagReference>, UserTrackingVertex {

	public static final String TYPE = "tag";

	List<? extends TagGraphFieldContainer> getFieldContainers();

	/**
	 * Return the tag family to which the tag belongs.
	 * 
	 * @return
	 */
	TagFamily getTagFamily();

	/**
	 * Unassign the the node from the tag.
	 * 
	 * @param node
	 */
	void removeNode(Node node);

	/**
	 * Return a list of nodes that were tagged by this tag in the given release
	 * @param release release
	 * 
	 * @return
	 */
	List<? extends Node> getNodes(Release release);

	/**
	 * Return a page of nodes that are visible to the user and which are tagged by this tag. Use the paging and language information provided.
	 * 
	 * @param requestUser
	 * @param release
	 * @param languageTags
	 * @param type
	 * @param pagingInfo
	 * @return
	 * @throws InvalidArgumentException
	 */
	PageImpl<? extends Node> findTaggedNodes(MeshAuthUser requestUser, Release release, List<String> languageTags, ContainerType type, PagingParameters pagingInfo)
			throws InvalidArgumentException;

	/**
	 * Return the tag graph field container that hold the tag name for the given language.
	 * 
	 * @param language
	 * @return
	 */
	TagGraphFieldContainer getFieldContainer(Language language);

	/**
	 * Return the tag graph field container. Create the container for the given language if non could be found.
	 * 
	 * @param language
	 * @return
	 */
	TagGraphFieldContainer getOrCreateFieldContainer(Language language);

	/**
	 * Set the tag family of this tag.
	 * 
	 * @param tagFamily
	 */
	void setTagFamily(TagFamily tagFamily);

	/**
	 * Set the project to which tag is assigned to.
	 * 
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Return the project to which the tag was assigned to
	 * 
	 * @return
	 */
	Project getProject();

}
