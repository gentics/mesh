package com.gentics.mesh.core.data.tag;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;
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
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for tags.
 */
public interface HibTag extends HibCoreElement<TagResponse>, HibReferenceableElement<TagReference>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.TAG, TAG_CREATED, TAG_UPDATED, TAG_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the tag family of the tag.
	 * 
	 * @return
	 */
	HibTagFamily getTagFamily();

	/**
	 * Return the project in which the tag is used.
	 * 
	 * @return
	 */
	HibProject getProject();

	/**
	 * Delete the tag.
	 */
	void deleteElement();

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getTagFamily().getUuid() + "/tags/" + getUuid();
	}

	/**
	 * Return the composed search index document if for the element.
	 * 
	 * @param elementUuid
	 * @return
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

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
}
