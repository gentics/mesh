package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.VersionNumber;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer, EditorTrackingVertex {

	// Webroot index

	String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	String WEBROOT_INDEX_NAME = "webrootPathInfoIndex";

	String PUBLISHED_WEBROOT_PROPERTY_KEY = "publishedWebrootPathInfo";

	String PUBLISHED_WEBROOT_INDEX_NAME = "publishedWebrootPathInfoIndex";

	// Url Field index

	String WEBROOT_URLFIELD_PROPERTY_KEY = "webrootUrlInfo";

	String WEBROOT_URLFIELD_INDEX_NAME = "webrootUrlInfoIndex";

	String PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY = "publishedWebrootUrlInfo";

	String PUBLISHED_WEBROOT_URLFIELD_INDEX_NAME = "publishedWebrootInfoIndex";

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "nodeContainer";

	/**
	 * Construct the index name using the provided information.
	 * 
	 * <p>
	 * <ul>
	 * <li>Document Index: [:indexPrefixnode-:projectUuid-:branchUuid-:schemaVersionUuid-:versionType]</li>
	 * <li>Example: node-934ef7f2210e4d0e8ef7f2210e0d0ec5-fd26b3cf20fb4f6ca6b3cf20fbdf6cd6-draft</li>
	 * </ul>
	 * <p>
	 * 
	 * @param projectUuid
	 * @param branchUuid
	 * @param schemaContainerVersionUuid
	 * @param type
	 * @return
	 */
	static String composeIndexName(String projectUuid, String branchUuid, String schemaContainerVersionUuid, ContainerType type) {
		Objects.requireNonNull(projectUuid, "The project uuid was not set");
		Objects.requireNonNull(branchUuid, "The release uuid was not set");
		Objects.requireNonNull(schemaContainerVersionUuid, "The schema container version uuid was not set");
		Objects.requireNonNull(type, "The container type was not set");
		// TODO check that only "draft" and "published" are used for version
		StringBuilder indexName = new StringBuilder();
		indexName.append("node");
		indexName.append("-");
		indexName.append(projectUuid);
		indexName.append("-");
		indexName.append(branchUuid);
		indexName.append("-");
		indexName.append(schemaContainerVersionUuid);
		indexName.append("-");
		indexName.append(type.toString().toLowerCase());
		return indexName.toString();
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
		return composeIndexName(projectUuid, branchUuid, getSchemaContainerVersion().getUuid(), type);
	}

	/**
	 * Construct the document id using the given information.
	 *
	 * <p>
	 * Format:
	 * <ul>
	 * <li>Document Id: [:uuid-:languageTag]</li>
	 * <li>Example: 234ef7f2510e4d0e8ef9f2210e0d0ec2-en</li>
	 * </ul>
	 * <p>
	 * 
	 * @param nodeUuid
	 * @param languageTag
	 * @return
	 */
	static String composeDocumentId(String nodeUuid, String languageTag) {
		Objects.requireNonNull(nodeUuid, "The nodeUuid was not set");
		Objects.requireNonNull(languageTag, "The language was was not set");
		StringBuilder id = new StringBuilder();
		id.append(nodeUuid);
		id.append("-");
		id.append(languageTag);
		return id.toString();
	}

	/**
	 * Creates the key for the webroot index.
	 *
	 * @param segmentValue Value of the segment field
	 * @param releaseUuid Uuid of the release
	 * @param parent Parent of the node
	 * @return The composed key
	 */
	static String composeWebrootIndexKey(String segmentValue, String releaseUuid, Node parent) {
		StringBuilder webRootInfo = new StringBuilder(segmentValue);
		webRootInfo.append("-").append(releaseUuid);
		if (parent != null) {
			webRootInfo.append("-").append(parent.getUuid());
		}
		return webRootInfo.toString();
	}

	/**
	 * Return the document id for the container.
	 * 
	 * @return
	 */
	default String getDocumentId() {
		return composeDocumentId(getParentNode().getUuid(), getLanguage().getLanguageTag());
	}

	/**
	 * Delete the field container. This will also delete linked elements like lists. If the container has a "next" container, that container will be deleted as
	 * well.
	 * 
	 * @param batch
	 */
	void delete(SearchQueueBatch batch);

	/**
	 * Delete the field container. This will also delete linked elements like lists.
	 * 
	 * @param batch
	 * @param deleteNext true to also delete all "next" containers, false to only delete this container
	 */
	void delete(SearchQueueBatch batch, boolean deleteNext);

	/**
	 * "Delete" the field container from the branch. This will not actually delete the container itself, but will remove DRAFT and PUBLISHED edges
	 *
	 * @param branch
	 * @param batch
	 */
	void deleteFromRelease(Branch branch, SearchQueueBatch batch);

	/**
	 * Return the display field value for this container.
	 * 
	 * @return
	 */
	String getDisplayFieldValue();

	/**
	 * Get the parent node to which this container belongs.
	 *
	 * @return
	 */
	Node getParentNode();

	/**
	 * Return the parent node the container for a specific release. Although a container always has the same container it may not be available in the specified
	 * release.
	 * 
	 * @param uuid
	 * @return Found node or null if no node could be found.
	 */
	Node getParentNode(String uuid);

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
	 * Check whether the field container has a next version
	 * 
	 * @return true iff the field container has a next version
	 */
	boolean hasNextVersion();

	/**
	 * Get the next versions.
	 * 
	 * @return iterable for all next versions
	 */
	Iterable<? extends NodeGraphFieldContainer> getNextVersions();

	/**
	 * Set the next version.
	 * 
	 * @param container
	 */
	void setNextVersion(NodeGraphFieldContainer container);

	/**
	 * Check whether the field container has a previous version
	 * 
	 * @return true iff the field container has a previous version
	 */
	boolean hasPreviousVersion();

	/**
	 * Get the previous version.
	 * 
	 * @return previous version or null
	 */
	NodeGraphFieldContainer getPreviousVersion();

	/**
	 * Make this container a clone of the given container. Property Vertices are reused.
	 *
	 * @param container
	 *            container
	 */
	void clone(NodeGraphFieldContainer container);

	/**
	 * Check whether this field container is the initial version for any
	 * release.
	 * 
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial() {
		return isType(INITIAL);
	}

	/**
	 * Check whether this field container is the draft version for any release.
	 * 
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft() {
		return isType(DRAFT);
	}

	/**
	 * Check whether this field container is the published version for any release.
	 * 
	 * @return true if it is published, false if not
	 */
	default boolean isPublished() {
		return isType(PUBLISHED);
	}

	/**
	 * Check whether this field container has the given type for any release.
	 * 
	 * @param type
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type);

	/**
	 * Check whether this field container is the initial version for the given
	 * release.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial(String releaseUuid) {
		return isType(INITIAL, releaseUuid);
	}

	/**
	 * Check whether this field container is the draft version for the given release.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft(String releaseUuid) {
		return isType(DRAFT, releaseUuid);
	}

	/**
	 * Check whether this field container is the published version for the given release.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is published, false if not
	 */
	default boolean isPublished(String releaseUuid) {
		return isType(PUBLISHED, releaseUuid);
	}

	/**
	 * Check whether this field container has the given type in the given release.
	 * 
	 * @param type
	 * @param releaseUuid
	 * @return true if it matches the type, false if not
	 */
	boolean isType(ContainerType type, String releaseUuid);

	/**
	 * Get tuples of type and release Uuids specifying for which release the container is a container of a type.
	 *
	 * @return set of tuples (may be empty, but never null)
	 */
	Set<Tuple<String, ContainerType>> getReleaseTypes();

	/**
	 * Get the release Uuids for which this container is the container of given type.
	 * 
	 * @param type
	 *            type
	 * @return set of release Uuids (may be empty, but never null)
	 */
	Set<String> getReleases(ContainerType type);

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
	 * Get all micronode fields that have a micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode fields
	 */
	List<? extends MicronodeGraphField> getMicronodeFields(MicroschemaContainerVersion version);

	/**
	 * Get all micronode list fields that have at least one micronode using the given microschema container version.
	 * 
	 * @param version
	 *            microschema container version
	 * @return list of micronode list fields
	 */
	List<? extends MicronodeGraphFieldList> getMicronodeListFields(MicroschemaContainerVersion version);

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
	 * Returns the segment field value of this container.
	 * 
	 * @return Determined segment field value or null if no segment field was specified or yet set
	 */
	String getSegmentFieldValue();

	/**
	 * Return the URL field values for the container.
	 * 
	 * @return
	 */
	Set<String> getUrlFieldValues();

	/**
	 * Traverse to the base node and build up the path to this container.
	 * 
	 * @param ac
	 * @return
	 */
	Path getPath(InternalActionContext ac);

}