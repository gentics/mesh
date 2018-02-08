package com.gentics.mesh.core.data;

import static com.gentics.mesh.search.SearchProvider.INDEX_PREFIX;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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

/**
 * A node field container is an aggregation node that holds localized fields (e.g.: StringField, NodeField...)
 */
public interface NodeGraphFieldContainer extends GraphFieldContainer, EditorTrackingVertex {

	// Webroot index

	public static final String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	public static final String WEBROOT_INDEX_NAME = "webrootPathInfoIndex";

	public static final String PUBLISHED_WEBROOT_PROPERTY_KEY = "publishedWebrootPathInfo";

	public static final String PUBLISHED_WEBROOT_INDEX_NAME = "publishedWebrootPathInfoIndex";

	// Url Field index

	public static final String WEBROOT_URLFIELD_PROPERTY_KEY = "webrootUrlInfo";

	public static final String WEBROOT_URLFIELD_INDEX_NAME = "webrootUrlInfoIndex";

	public static final String PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY = "publishedWebrootUrlInfo";

	public static final String PUBLISHED_WEBROOT_URLFIELD_INDEX_NAME = "publishedWebrootInfoIndex";

	/**
	 * Type Value: {@value #TYPE}
	 */
	static final String TYPE = "nodeContainer";

	/**
	 * Construct the index name using the provided information.
	 * 
	 * <p>
	 * <ul>
	 * <li>Document Index: [node-:projectUuid-:releaseUuid-:schemaVersionUuid-:versionType]</li>
	 * <li>Example: node-934ef7f2210e4d0e8ef7f2210e0d0ec5-fd26b3cf20fb4f6ca6b3cf20fbdf6cd6-draft</li>
	 * </ul>
	 * <p>
	 * 
	 * @param projectUuid
	 * @param releaseUuid
	 * @param schemaContainerVersionUuid
	 * @param type
	 * @return
	 */
	static String composeIndexName(String projectUuid, String releaseUuid, String schemaContainerVersionUuid, ContainerType type) {
		Objects.requireNonNull(projectUuid, "The project uuid was not set");
		Objects.requireNonNull(releaseUuid, "The release uuid was not set");
		Objects.requireNonNull(schemaContainerVersionUuid, "The schema container version uuid was not set");
		Objects.requireNonNull(type, "The container type was not set");
		// TODO check that only "draft" and "published" are used for version
		StringBuilder indexName = new StringBuilder();
		indexName.append(INDEX_PREFIX);
		indexName.append("node");
		indexName.append("-");
		indexName.append(projectUuid);
		indexName.append("-");
		indexName.append(releaseUuid);
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
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	default String getIndexName(String projectUuid, String releaseUuid, ContainerType type) {
		return composeIndexName(projectUuid, releaseUuid, getSchemaContainerVersion().getUuid(), type);
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
	 * Make this container a clone of the given container. Property Vertices are reused.
	 *
	 * @param container
	 *            container
	 */
	void clone(NodeGraphFieldContainer container);

	/**
	 * Check whether this field container is the draft version for the given release.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is the draft, false if not
	 */
	boolean isDraft(String releaseUuid);

	/**
	 * Check whether this field container is the published version for the given release.
	 * 
	 * @param releaseUuid
	 *            release Uuid
	 * @return true if it is published, false if not
	 */
	boolean isPublished(String releaseUuid);

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
	 * Get all nodes that are in any way referenced by this node. This includes the following cases:
	 * * Node fields
	 * * Node list fields
	 * * Micronode fields with node fields or node list fields
	 * * Micronode list fields with node fields or node list fields
	 */
	Iterable<? extends Node> getReferencedNodes();

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
	 * Find the specified version of the container.
	 * 
	 * @param version
	 * @return
	 */
	NodeGraphFieldContainer findVersion(String version);

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