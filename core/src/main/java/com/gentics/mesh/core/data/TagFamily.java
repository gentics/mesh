package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;

public interface TagFamily extends GenericVertex<TagFamilyResponse>, NamedVertex, IndexedVertex {

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
	 * Create a new tag with the given name and creator within this tag family. Note that this method will not check for any tag name collisions.
	 * 
	 * @param name
	 *            Name of the new tag.
	 * @param project
	 * @param creator
	 *            User that is used to assign creator and editor references of the new tag.
	 * @return
	 */
	Tag create(String name, Project project, User creator);

	/**
	 * Remove the given tag from the tagfamily.
	 * 
	 * @param tag
	 */
	void removeTag(Tag tag);

	/**
	 * Add the given tag to the tagfamily.
	 * 
	 * @param tag
	 */
	void addTag(Tag tag);

	/**
	 * Return a list of all tags that are assigned to the tag family.
	 * 
	 * @return
	 */
	List<? extends Tag> getTags();

	/**
	 * Return a page of all tags which are visible to the given user. Use the paging parameters from the action context.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return
	 * @throws InvalidArgumentException
	 */
	Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException;

	/**
	 * Return the tag with the given name that was assigned to the tag family.
	 * 
	 * @param name
	 * @return Found tag or null when no matching tag could be found.
	 */
	Tag findTagByName(String name);

	TagFamilyImpl getImpl();
}
