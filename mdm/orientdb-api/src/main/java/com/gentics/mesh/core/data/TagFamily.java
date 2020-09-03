package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
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
	RootVertex<Tag>, ProjectElement, HibTagFamily {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.TAGFAMILY, TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED);

	/**
	 * Construct the index name for tag family indices. Use the projectUuid in order to create a project specific index.
	 * 
	 * @param projectUuid
	 * @return
	 */
	static String composeIndexName(String projectUuid) {
		Objects.requireNonNull(projectUuid, "A projectUuid must be provided.");
		StringBuilder indexName = new StringBuilder();
		indexName.append("tagfamily");
		indexName.append("-").append(projectUuid);
		return indexName.toString();
	}

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Construct the documentId for tag family index documents.
	 * 
	 * @param elementUuid
	 * @return documentId
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

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
	Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingParameters pagingInfo);

	/**
	 * Return the tag family to which this tag belongs.
	 * 
	 * @return
	 */
	TagFamilyRoot getTagFamilyRoot();

	/**
	 * Set the project to which the tag family should be assigned.
	 * 
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Create a new tag using the information from the action context.
	 * 
	 * @param ac
	 * @param batch
	 * @return
	 */
	Tag create(InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Add the given tag to the aggregation vertex.
	 * 
	 * @param tag
	 *            Tag to be added
	 */
	void addTag(Tag tag);

	/**
	 * Remove the tag from the aggregation vertex.
	 * 
	 * @param tag
	 *            Tag to be removed
	 */
	void removeTag(Tag tag);

}
