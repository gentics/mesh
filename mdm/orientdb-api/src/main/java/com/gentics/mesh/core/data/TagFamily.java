package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * The TagFamily domain model interface.
 * 
 * A tag family is the parent element for multiple tags. A typical tag family would be "colors" for tags "red", "blue", "green". Tag families are bound to
 * projects via the {@link TagFamilyRootImpl} class.
 */
public interface TagFamily extends MeshCoreVertex<TagFamilyResponse>, ReferenceableElement<TagFamilyReference>, UserTrackingVertex,
	RootVertex<Tag>, ProjectElement, HibTagFamily, GraphDBBucketableElement {

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
	 * Return a page of all tags which are visible to the given user. Use the paging parameters from the action context.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends Tag> getTags(HibUser requestUser, PagingParameters pagingInfo);

	/**
	 * Return the tag family to which this tag belongs.
	 * 
	 * @return
	 */
	TagFamilyRoot getTagFamilyRoot();

	/**
	 * Create a new tag using the information from the action context.
	 * 
	 * @param ac
	 * @param batch
	 * @return
	 */
	Tag create(InternalActionContext ac, EventQueueBatch batch);

	/**
	 * @deprecated Remove after PersistingTag/FamilyDao approached.
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	@Deprecated
	Tag create(InternalActionContext ac, EventQueueBatch batch, String uuid);

}
