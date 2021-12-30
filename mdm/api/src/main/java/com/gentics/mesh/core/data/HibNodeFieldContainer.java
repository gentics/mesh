package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

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
		return ContentDao.composeIndexName(projectUuid, branchUuid, getSchemaContainerVersion().getUuid(), type);
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
	 * Delete the field container. This will also delete linked elements like lists. If the container has a "next" container, that container will be deleted as
	 * well.
	 * 
	 * @param bac
	 */
	void delete(BulkActionContext bac);

	/**
	 * Delete the field container. This will also delete linked elements like lists.
	 * 
	 * @param bac
	 * @param deleteNext
	 *            true to also delete all "next" containers, false to only delete this container
	 */
	void delete(BulkActionContext bac, boolean deleteNext);

	/**
	 * "Delete" the field container from the branch. This will not actually delete the container itself, but will remove DRAFT and PUBLISHED edges
	 *
	 * @param branch
	 * @param bac
	 */
	void deleteFromBranch(HibBranch branch, BulkActionContext bac);

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

	/**
	 * Check whether this field container is the initial version for any branch.
	 * 
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial() {
		return isType(INITIAL);
	}

	/**
	 * Check whether this field container is the draft version for any branch.
	 * 
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft() {
		return isType(DRAFT);
	}

	/**
	 * Check whether this field container is the published version for any branch.
	 * 
	 * @return true if it is published, false if not
	 */
	default boolean isPublished() {
		return isType(PUBLISHED);
	}

	/**
	 * Check whether this field container has the given type for any branch.
	 * 
	 * @param type
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type);

	/**
	 * Check whether this field container is the initial version for the given branch.
	 * 
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial(String branchUuid) {
		return isType(INITIAL, branchUuid);
	}

	/**
	 * Check whether this field container is the draft version for the given branch.
	 * 
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft(String branchUuid) {
		return isType(DRAFT, branchUuid);
	}

	/**
	 * Check whether this field container is the published version for the given branch.
	 * 
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is published, false if not
	 */
	default boolean isPublished(String branchUuid) {
		return isType(PUBLISHED, branchUuid);
	}

	/**
	 * Check whether this field container has the given type in the given branch.
	 * 
	 * @param type
	 * @param branchUuid
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type, String branchUuid);

	/**
	 * Get the branch Uuids for which this container is the container of given type.
	 * 
	 * @param type
	 *            type
	 * @return set of branch Uuids (may be empty, but never null)
	 */
	Set<String> getBranches(ContainerType type);

	HibSchemaVersion getSchemaContainerVersion();

	/**
	 * Get all micronode fields that have a micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode fields
	 */
	List<HibMicronodeField> getMicronodeFields(HibMicroschemaVersion version);

	/**
	 * Get all micronode list fields that have at least one micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode list fields
	 */
	Result<HibMicronodeFieldList> getMicronodeListFields(HibMicroschemaVersion version);

	/**
	 * Return the ETag for the field container.
	 * 
	 * @param ac
	 * @return Generated entity tag
	 */
	String getETag(InternalActionContext ac);

	/**
	 * Determine the display field value by checking the schema and the referenced field and store it as a property.
	 */
	void updateDisplayFieldValue();

	/**
	 * Update the current segment field and increment any found postfix number.
	 */
	void postfixSegmentFieldValue();

	/**
	 * Traverse to the base node and build up the path to this container.
	 * 
	 * @param ac
	 * @return
	 */
	Path getPath(InternalActionContext ac);

	/**
	 * A container is purgeable when it is not being utilized as draft, published or initial version in any branch.
	 *
	 * @return
	 */
	boolean isPurgeable();

	/**
	 * Check whether auto purge is enabled globally or for the schema of the container.
	 *
	 * @return
	 */
	boolean isAutoPurgeEnabled();

	/**
	 * Purge the container from the version history and ensure that the links between versions are consistent.
	 *
	 * @param bac
	 *            Action context for the deletion process
	 */
	void purge(BulkActionContext bac);

	/**
	 * Purge the container from the version without the use of a Bulk Action Context.
	 */
	default void purge() {
		purge(new DummyBulkActionContext());
	}

	/**
	 * Return all versions.
	 *
	 * @return
	 */
	Result<HibNodeFieldContainer> versions();
}
