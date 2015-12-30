package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;

/**
 * The TagFamily Domain Model interface.
 * 
 * A tag family is the parent element for multiple tags. A typical tag family would be "colors" for tags "red", "blue", "green". Tag families are
 * bound to projects via the {@link TagFamilyRootImpl} class.
 */
public interface TagFamily extends MeshCoreVertex<TagFamilyResponse, TagFamily>, ReferenceableElement<TagFamilyReference> {

	public static final String TYPE = "tagFamily";

	/**
	 * Return the description of the tag family.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Set the description of the tag family.
	 * 
	 * @param description
	 */
	void setDescription(String description);

	/**
	 * Create a new tag with the given name and creator. Note that this method will not check for any tag name collisions. Internally the connected tag root
	 * will be used to link the created tag to this tag family.
	 * 
	 * @param name
	 *            Name of the new tag.
	 * @param project
	 *            Root project of the tag.
	 * @param creator
	 *            User that is used to assign creator and editor references of the new tag.
	 * @return
	 */
	Tag create(String name, Project project, User creator);

	/**
	 * Return a page of all tags which are visible to the given user. Use the paging parameters from the action context.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return
	 * @throws InvalidArgumentException
	 */
	PageImpl<? extends Tag> getTags(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException;

	/**
	 * Return the tag family to which this tag belongs.
	 * 
	 * @return
	 */
	TagFamilyRoot getTagFamilyRoot();

	/**
	 * Return the project to which the tag family has been assigned.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Set the project to which the tag family should be assigned.
	 * 
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Set the tag root element for the tag family.
	 * 
	 * @param tagRoot
	 */
	void setTagRoot(TagRoot tagRoot);

	/**
	 * Return the tag root for the tag family.
	 * 
	 * @return
	 */
	TagRoot getTagRoot();
}
