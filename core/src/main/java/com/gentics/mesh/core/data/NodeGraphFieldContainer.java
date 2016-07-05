package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.VersionNumber;

/**
 * A node field container is a aggregation node that holds localized fields.
 *
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer, EditorTrackingVertex {

	/**
	 * Delete the field container. This will also delete linked elements like lists. If the container has a "next" container, that container will be deleted as
	 * well.
	 * 
	 * @param batch
	 */
	void delete(SearchQueueBatch batch);

	/**
	 * "Delete" the field container from the release. This will not actually delete the container itself, but will remove DRAFT and PUBLISHED edges
	 *
	 * @param release
	 * @param batch
	 */
	void deleteFromRelease(Release release, SearchQueueBatch batch);

	/**
	 * Return the display field value for this container.
	 * 
	 * @return
	 */
	String getDisplayFieldValue();

	/**
	 * Get the parent node.
	 *
	 * @return
	 */
	Node getParentNode();

	/**
	 * Update the property webroot path info. This will also check for uniqueness conflicts of the webroot path and will throw a
	 * {@link Errors#conflict(String, String, String, String...)} if one found.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @param conflictI18n
	 *            key of the message in case of conflicts
	 */
	void updateWebrootPathInfo(String releaseUuid, String conflictI18n);

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
	 * Get the next version.
	 * 
	 * @return next version or null
	 */
	NodeGraphFieldContainer getNextVersion();

	/**
	 * Set the next version.
	 * 
	 * @param container
	 */
	void setNextVersion(NodeGraphFieldContainer container);

	/**
	 * Get the previous version.
	 * 
	 * @return previous version or null
	 */
	NodeGraphFieldContainer getPreviousVersion();

	/**
	 * Make this container a clone of the given container. Property Vertices are reused
	 *
	 * @param container
	 *            container
	 */
	void clone(NodeGraphFieldContainer container);

	/**
	 * Check whether this field container is the draft version for the given release
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is the draft, false if not
	 */
	boolean isDraft(String releaseUuid);

	/**
	 * Check whether this field container is the published version for the given release
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is published, false if not
	 */
	boolean isPublished(String releaseUuid);

	/**
	 * Get tuples of type and release Uuids specifying for which release the container is a container of a type
	 *
	 * @return set of tuples (may be empty, but never null)
	 */
	Set<Tuple<String, ContainerType>> getReleaseTypes();

	/**
	 * Get the release Uuids for which this container is the container of given type
	 * 
	 * @param type
	 *            type
	 * @return set of release Uuids (may be empty, but never null)
	 */
	Set<String> getReleases(ContainerType type);

	/**
	 * Add a search queue batch entry to the given batch for the given action.
	 * 
	 * @param batch
	 * @param action
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            type
	 */
	void addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action, String releaseUuid, ContainerType type);

	/**
	 * Compare the container values of both containers and return a list of differences.
	 * 
	 * @param container
	 */
	List<FieldContainerChange> compareTo(NodeGraphFieldContainer container);

	/**
	 * Compare the values of this container with the values of the given fieldmap and return a list of detected differences.
	 * 
	 * @param fieldMap
	 * @return
	 */
	List<FieldContainerChange> compareTo(FieldMap fieldMap);

	@Override
	SchemaContainerVersion getSchemaContainerVersion();

	/**
	 * Get all micronode fields that have a micronode using the given microschema container version
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode fields
	 */
	List<? extends MicronodeGraphField> getMicronodeFields(MicroschemaContainerVersion version);

	/**
	 * Get all micronode list fields that have at least one micronode using the given microschema container version
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode list fields
	 */
	List<? extends MicronodeGraphFieldList> getMicronodeListFields(MicroschemaContainerVersion version);
}
