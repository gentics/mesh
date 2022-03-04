package com.gentics.mesh.core.data;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.VersionNumber;

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface HibNodeFieldContainer extends HibFieldContainer, HibEditorTracking, HibBucketableElement {

	/**
	 * Check if the field container exists in the storage.
	 * 
	 * @return
	 */
	default boolean isValid() {
		return true;
	}

	/**
	 * Return the index name for the given parameters.
	 * 
	 * @param projectUuid
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	default String getIndexName(String projectUuid, String branchUuid, ContainerType type) {
		return ContentDao.composeIndexName(projectUuid, branchUuid, getSchemaContainerVersion().getUuid(), type, null);
	}

	/**
	 * Return the document id for the container.
	 * 
	 * @return
	 */
	default String getDocumentId() {
		return ContentDao.composeDocumentId(getNode().getUuid(), getLanguageTag());
	}

	/**
	 * Return the display field value for this container.
	 * 
	 * @return
	 */
	String getDisplayFieldValue();

	/**
	 * Get the node to which this container belongs.
	 *
	 * @return
	 */
	HibNode getNode();

	/**
	 * Get the Version Number or null if no version set.
	 * 
	 * @return Version Number
	 */
	VersionNumber getVersion();

	/**
	 * Set the Version Number.
	 * 
	 * @param version
	 */
	void setVersion(VersionNumber version);

	/**
	 * Check whether the field container has a next version
	 * 
	 * @return true if the field container has a next version
	 */
	boolean hasNextVersion();

	/**
	 * Get the next versions.
	 * 
	 * @return iterable for all next versions
	 */
	Iterable<HibNodeFieldContainer> getNextVersions();

	/**
	 * Set the next version.
	 * 
	 * @param container
	 */
	void setNextVersion(HibNodeFieldContainer container);

	/**
	 * Check whether the field container has a previous version
	 * 
	 * @return true if the field container has a previous version
	 */
	boolean hasPreviousVersion();

	/**
	 * Get the previous version.
	 * 
	 * @return previous version or null
	 */
	HibNodeFieldContainer getPreviousVersion();

	/**
	 * Make this container a clone of the given container. Property Vertices are reused.
	 *
	 * @param container
	 *            container
	 */
	void clone(HibNodeFieldContainer container);

	@Override
	HibSchemaVersion getSchemaContainerVersion();

	/**
	 * Return the ETag for the field container.
	 * 
	 * @param ac
	 * @return Generated entity tag
	 */
	default String getETag(InternalActionContext ac) {
		Stream<String> referencedUuids = StreamSupport.stream(getReferencedNodes().spliterator(), false)
			.map(HibNode::getUuid);

		int hashcode = Stream.concat(Stream.of(getUuid()), referencedUuids)
			.collect(Collectors.toSet())
			.hashCode();

		return ETag.hash(hashcode);
	}

	@Override
	default Stream<HibNodeFieldContainer> getContents() {
		return Stream.of(this);
	}

	@Override
	default ReferenceType getReferenceType() {
		return ReferenceType.FIELD;
	}
}
