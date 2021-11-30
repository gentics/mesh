package com.gentics.mesh.core.data.tagfamily;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.HibReferenceableElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for tag families.
 */
public interface HibTagFamily extends HibCoreElement<TagFamilyResponse>, HibReferenceableElement<TagFamilyReference>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.TAGFAMILY, TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the project in which the tag family is used.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Delete the tag family.
	 */
	void deleteElement();

	/**
	 * Return the description.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Set the description.
	 * 
	 * @param description
	 */
	void setDescription(String description);

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getUuid();
	}

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
}
