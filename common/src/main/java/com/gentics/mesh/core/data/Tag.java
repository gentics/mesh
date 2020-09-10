package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;

import java.util.List;
import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.BucketableElement;

/**
 * Graph domain model interface for a tag.
 * 
 * Tags can currently only hold a single string value. Tags are not localizable. A tag can only be assigned to a single tag family.
 */
public interface Tag extends MeshCoreVertex<TagResponse, Tag>, ReferenceableElement<TagReference>, UserTrackingVertex, ProjectElement, BucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.TAG, TAG_CREATED, TAG_UPDATED, TAG_DELETED);

	/**
	 * Compose the index name for tags. Use the projectUuid in order to create a project specific index.
	 * 
	 * @param projectUuid
	 * @return
	 */
	static String composeIndexName(String projectUuid) {
		Objects.requireNonNull(projectUuid, "A projectUuid must be provided.");
		StringBuilder indexName = new StringBuilder();
		indexName.append("tag");
		indexName.append("-").append(projectUuid);
		return indexName.toString();
	}

	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the tag family to which the tag belongs.
	 * 
	 * @return Tag family of the tag
	 */
	TagFamily getTagFamily();

	/**
	 * Unassign the the node from the tag.
	 * 
	 * @param node
	 */
	void removeNode(Node node);

	/**
	 * Return a traversal result of nodes that were tagged by this tag in the given branch
	 * 
	 * @param branch
	 *            branch
	 * 
	 * @return Result
	 */
	TraversalResult<? extends Node> getNodes(Branch branch);

	/**
	 * Return a page of nodes that are visible to the user and which are tagged by this tag. Use the paging and language information provided.
	 * 
	 * @param requestUser
	 * @param branch
	 * @param languageTags
	 * @param type
	 * @param pagingInfo
	 * @return
	 */
	TransformablePage<? extends Node> findTaggedNodes(MeshAuthUser requestUser, Branch branch, List<String> languageTags, ContainerType type,
			PagingParameters pagingInfo);

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
	 * @return Project of the tag
	 */
	Project getProject();

	TraversalResult<? extends Node> findTaggedNodes(InternalActionContext ac);

}
