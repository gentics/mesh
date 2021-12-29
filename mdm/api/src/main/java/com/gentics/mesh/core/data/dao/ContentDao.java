package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.Errors;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.util.VersionNumber;

/**
 * DAO for contained data.
 */
public interface ContentDao {
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
		return composeIndexName(projectUuid, branchUuid, schemaContainerVersionUuid, type, null);
	}

	/**
	 * Construct the index name using the provided information.
	 *
	 * <p>
	 * <ul>
	 * <li>Document Index: ":indexPrefixnode-:projectUuid-:branchUuid-:schemaVersionUuid-:versionType[-:language]"</li>
	 * <li>Example: node-934ef7f2210e4d0e8ef7f2210e0d0ec5-fd26b3cf20fb4f6ca6b3cf20fbdf6cd6-draft</li>
	 * <li>Example with language: node-934ef7f2210e4d0e8ef7f2210e0d0ec5-fd26b3cf20fb4f6ca6b3cf20fbdf6cd6-draft-en</li>
	 * </ul>
	 * <p>
	 *
	 * @param projectUuid
	 * @param branchUuid
	 * @param schemaContainerVersionUuid
	 * @param type
	 * @param language
	 * @return
	 */
	static String composeIndexName(String projectUuid, String branchUuid, String schemaContainerVersionUuid, ContainerType type, String language) {
		Objects.requireNonNull(projectUuid, "The project uuid was not set");
		Objects.requireNonNull(branchUuid, "The branch uuid was not set");
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
		if (language != null) {
			indexName.append("-");
			indexName.append(language.toLowerCase());
		}
		return indexName.toString();
	}

	/**
	 * Construct an index name pattern catching all node indices of a specific project, branch and schema.
	 *
	 * @param projectUuid
	 * @param branchUuid
	 * @param schemaContainerVersionUuid
	 * @return
	 */
	static String composeIndexPattern(String projectUuid, String branchUuid, String schemaContainerVersionUuid) {
		Objects.requireNonNull(projectUuid, "The project uuid was not set");
		Objects.requireNonNull(branchUuid, "The branch uuid was not set");
		Objects.requireNonNull(schemaContainerVersionUuid, "The schema container version uuid was not set");
		return String.format("node-%s-%s-%s-*", projectUuid, branchUuid, schemaContainerVersionUuid);
	}

	/**
	 * Construct an index name pattern catching all node indices of a specific project, branch and version.
	 *
	 * @param projectUuid
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	static String composeIndexPattern(String projectUuid, String branchUuid, ContainerType type) {
		Objects.requireNonNull(projectUuid, "The project uuid was not set");
		Objects.requireNonNull(branchUuid, "The branch uuid was not set");
		Objects.requireNonNull(type, "The container type was not set");
		return String.format("node-%s-%s-*-%s*", projectUuid, branchUuid, type.toString().toLowerCase());
	}

	/**
	 * Construct an index name pattern catching all node indices of a specific project.
	 *
	 * @param projectUuid
	 * @return
	 */
	static String composeIndexPattern(String projectUuid) {
		Objects.requireNonNull(projectUuid, "The project uuid was not set");
		return String.format("node-%s-*", projectUuid);
	}

	/**
	 * Construct an index name pattern catching all node indices of a specific version.
	 *
	 * @param type
	 * @return
	 */
	static String composeIndexPattern(ContainerType type) {
		Objects.requireNonNull(type, "The container type was not set");
		return String.format("node-*-*-*-%s*", type.toString().toLowerCase());
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
	 * Return the path segment value of this node preferable in the given language.
	 *
	 * If more than one language is given, the path will lead to the first available language of the node.
	 *
	 * When no language matches and <code>anyLanguage</code> is <code>true</code> the results language is nondeterministic.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param anyLanguage
	 *            whether to return the path segment value of this node in any language, when none in <code>langaugeTag</code> match
	 * @param languageTag
	 *
	 * @return
	 */
	String getPathSegment(HibNode node, String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag);

	/**
	 * Return the path segment value of this node in the given language. If more than one language is given, the path will lead to the first available language
	 * of the node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 *
	 * @return
	 */
	default String getPathSegment(HibNode node, String branchUuid, ContainerType type, String... languageTag) {
		return getPathSegment(node, branchUuid, type, false, languageTag);
	}

	/**
	 * Return the mount of elements.
	 * 
	 * @return
	 */
	long globalCount();


	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	HibNodeFieldContainer getLatestDraftFieldContainer(HibNode node, String languageTag);

	/**
	 * Return the field container for the given language, type and branch.
	 *
	 * @param languageTag
	 * @param branch
	 * @param type
	 *            type
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, HibBranch branch, ContainerType type);

	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag);

	/**
	 * Return the field container for the given language, type and branch Uuid.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, String branchUuid, ContainerType type);

	/**
	 * Create a new field container for the given language and assign the schema version of the branch to the container. The field container will be
	 * the (only) DRAFT version for the language/branch. If this is the first container for the language, it will also be the INITIAL version. Otherwise the
	 * container will be a clone of the last draft and will have the next version number.
	 *
	 * @param languageTag
	 * @param branch
	 *            branch
	 * @param user
	 *            user
	 * @return
	 */
	HibNodeFieldContainer createFieldContainer(HibNode node, String languageTag, HibBranch branch, HibUser user);

	/**
	 * Like {@link #createFieldContainer(HibNode, String, HibBranch, HibUser)}, but let the new field container be a clone of the given original (if not
	 * null).
	 *
	 * @param languageTag
	 * @param branch
	 * @param editor
	 *            User which will be set as editor
	 * @param original
	 *            Container to be used as a source for the new container
	 * @param handleDraftEdge
	 *            Whether to move the existing draft edge or create a new draft edge to the new container
	 * @return Created container
	 */
	HibNodeFieldContainer createFieldContainer(HibNode node, String languageTag, HibBranch branch, HibUser editor,
		HibNodeFieldContainer original,
		boolean handleDraftEdge);

	/**
	 * Return the draft field containers of the node in the latest branch.
	 *
	 * @return
	 */
	Result<HibNodeFieldContainer> getDraftFieldContainers(HibNode node);

	/**
	 * Return a traversal of field containers of given type for the node in the given branch.
	 *
	 * @param branch
	 * @param type
	 * @return
	 */
	default Result<HibNodeFieldContainer> getFieldContainers(HibNode node, HibBranch branch, ContainerType type) {
		return getFieldContainers(node, branch.getUuid(), type);
	}

	/**
	 * Return traversal of field containers of given type for the node in the given branch.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	Result<HibNodeFieldContainer> getFieldContainers(HibNode node, String branchUuid, ContainerType type);

	/**
	 * Return containers of the given type
	 *
	 * @param type
	 * @return
	 */
	Result<HibNodeFieldContainer> getFieldContainers(HibNode node, ContainerType type);

	/**
	 * Return the number of field containers of the node of type DRAFT or PUBLISHED in any branch.
	 *
	 * @return
	 */
	long getFieldContainerCount(HibNode node);

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter. When a user requests a node using ?lang=de,en and there
	 * is no de version the en version will be selected and returned.
	 *
	 * @param languageTags
	 * @param branchUuid
	 *            branch Uuid
	 * @param version
	 *            requested version. This must either be "draft" or "published" or a version number with pattern [major.minor]
	 * @return Next matching field container or null when no language matches
	 */

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter. When a user requests a node using ?lang=de,en and there
	 * is no de version the en version will be selected and returned.
	 *
	 * @param node
	 * @param languageTags
	 * @param branchUuid
	 *            branch Uuid
	 * @param version
	 *            requested version. This must either be "draft" or "published" or a version number with pattern [major.minor]
	 * @return Next matching field container or null when no language matches
	 */
	default HibNodeFieldContainer findVersion(HibNode node, List<String> languageTags, String branchUuid, String version) {
		HibNodeFieldContainer fieldContainer = null;

		// TODO refactor the type handling and don't return INITIAL.
		ContainerType type = forVersion(version);

		for (String languageTag : languageTags) {

			// Don't start the version lookup using the initial version. Instead start at the end of the chain and use the DRAFT version instead.
			fieldContainer = getFieldContainer(node, languageTag, branchUuid, type == INITIAL ? DRAFT : type);

			// Traverse the chain downwards and stop once we found our target version or we reached the end.
			if (fieldContainer != null && type == INITIAL) {
				while (fieldContainer != null && !version.equals(fieldContainer.getVersion().toString())) {
					fieldContainer = fieldContainer.getPreviousVersion();
				}
			}

			// We found a container for one of the languages
			if (fieldContainer != null) {
				break;
			}
		}
		return fieldContainer;
	}

	/**
	 * Iterate the version chain from the back in order to find the given version.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param version
	 * @return Found version or null when no version could be found.
	 */
	default HibNodeFieldContainer findVersion(HibNode node, String languageTag, String branchUuid, String version) {
		return findVersion(node, Arrays.asList(languageTag), branchUuid, version);
	}

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter.
	 *
	 * @param ac
	 * @param languageTags
	 * @return Next matching field container or null when no language matches
	 */
	default HibNodeFieldContainer findVersion(HibNode node, InternalActionContext ac, List<String> languageTags, String version) {
		Tx tx = Tx.get();
		return findVersion(node, languageTags, tx.getBranch(ac).getUuid(), version);
	}

	/**
	 * Find the content that matches the given parameters (languages, type).
	 *
	 * @param ac
	 * @param languageTags
	 * @param type
	 * @return
	 */
	default HibNodeFieldContainer findVersion(HibNode node, InternalActionContext ac, List<String> languageTags, ContainerType type) {
		return findVersion(node, ac, languageTags, type.getHumanCode());
	}

	/**
	 * Delete the language container for the given language from the branch. This will remove all PUBLISHED, DRAFT and INITIAL edges to GFCs for the language
	 * and branch, and will then delete all "dangling" GFC (GFCs, which are not used by another branch).
	 *
	 * @param ac
	 * @param branch
	 * @param languageTag
	 *            Language which will be used to find the field container which should be deleted
	 * @param bac
	 * @param failForLastContainer
	 *            Whether to execute the last container check and fail or not.
	 */
	void deleteLanguageContainer(HibNode node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
		boolean failForLastContainer);


	/**
	 * Create a new published version of the given language in the branch.
	 *
	 * @param ac
	 *            Action Context
	 * @param languageTag
	 *            language
	 * @param branch
	 *            branch
	 * @param user
	 *            user
	 * @return published field container
	 */
	HibNodeFieldContainer publish(HibNode node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user);

	/**
	 * Gets all NodeField edges that reference this node.
	 * 
	 * @return
	 */
	Stream<HibNodeField> getInboundReferences(HibNode node);

	/**
	 * Return the index name for the given parameters.
	 *
	 * @param projectUuid
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	default String getIndexName(HibNodeFieldContainer content, String projectUuid, String branchUuid, ContainerType type) {
		return ContentDao.composeIndexName(projectUuid, branchUuid, getSchemaContainerVersion(content).getUuid(), type);
	}

	/**
	 * Return the document id for the container.
	 *
	 * @return
	 */
	default String getDocumentId(HibNodeFieldContainer content) {
		return ContentDao.composeDocumentId(getNode(content).getUuid(), getLanguageTag(content));
	}

	/**
	 * Delete the field container. This will also delete linked elements like lists. If the container has a "next" container, that container will be deleted as
	 * well.
	 *
	 * @param bac
	 */
	void delete(HibNodeFieldContainer content, BulkActionContext bac);

	/**
	 * Delete the field container. This will also delete linked elements like lists.
	 *
	 * @param bac
	 * @param deleteNext
	 *            true to also delete all "next" containers, false to only delete this container
	 */
	void delete(HibNodeFieldContainer content, BulkActionContext bac, boolean deleteNext);

	/**
	 * "Delete" the field container from the branch. This will not actually delete the container itself, but will remove DRAFT and PUBLISHED edges
	 *
	 * @param branch
	 * @param bac
	 */
	void deleteFromBranch(HibNodeFieldContainer content, HibBranch branch, BulkActionContext bac);

	/**
	 * Return the display field value for this container.
	 *
	 * @return
	 */
	String getDisplayFieldValue(HibNodeFieldContainer content);

	/**
	 * Get the parent node to which this container belongs.
	 *
	 * @return
	 */
	HibNode getNode(HibNodeFieldContainer content);

	/**
	 * Update the property webroot path info. This will also check for uniqueness conflicts of the webroot path and will throw a
	 * {@link Errors#conflict(String, String, String, String...)} if one found.
	 *
	 * @param ac
	 * @param branchUuid
	 *            branch Uuid
	 * @param conflictI18n
	 *            key of the message in case of conflicts
	 */
	void updateWebrootPathInfo(HibNodeFieldContainer content, InternalActionContext ac, String branchUuid, String conflictI18n);

	/**
	 * Update the property webroot path info. This will also check for uniqueness conflicts of the webroot path and will throw a
	 * {@link Errors#conflict(String, String, String, String...)} if one found.
	 *
	 * @param branchUuid
	 * @param conflictI18n
	 */
	default void updateWebrootPathInfo(HibNodeFieldContainer content, String branchUuid, String conflictI18n) {
		updateWebrootPathInfo(content, null, branchUuid, conflictI18n);
	}

	/**
	 * Get the Version Number or null if no version set.
	 *
	 * @return Version Number
	 */
	VersionNumber getVersion(HibNodeFieldContainer content);

	/**
	 * Set the Version Number.
	 *
	 * @param version
	 */
	void setVersion(HibNodeFieldContainer content, VersionNumber version);

	/**
	 * Check whether the field container has a next version
	 *
	 * @return true iff the field container has a next version
	 */
	boolean hasNextVersion(HibNodeFieldContainer content);

	/**
	 * Get the next versions.
	 *
	 * @return iterable for all next versions
	 */
	Iterable<HibNodeFieldContainer> getNextVersions(HibNodeFieldContainer content);

	/**
	 * Set the next version.
	 *
	 * @param current
	 * @param next
	 */
	void setNextVersion(HibNodeFieldContainer current, HibNodeFieldContainer next);

	/**
	 * Check whether the field container has a previous version
	 *
	 * @return true if the field container has a previous version
	 */
	boolean hasPreviousVersion(HibNodeFieldContainer content);

	/**
	 * Get the previous version.
	 *
	 * @return previous version or null
	 */
	HibNodeFieldContainer getPreviousVersion(HibNodeFieldContainer content);

	/**
	 * Make this container a clone of the given container. Property Vertices are reused.
	 *
	 * @param dest
	 * @param src
	 */
	void clone(HibNodeFieldContainer dest, HibNodeFieldContainer src);

	/**
	 * Check whether this field container is the initial version for any branch.
	 *
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial(HibNodeFieldContainer content) {
		return isType(content, INITIAL);
	}

	/**
	 * Check whether this field container is the draft version for any branch.
	 *
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft(HibNodeFieldContainer content) {
		return isType(content, DRAFT);
	}

	/**
	 * Check whether this field container is the published version for any branch.
	 *
	 * @return true if it is published, false if not
	 */
	default boolean isPublished(HibNodeFieldContainer content) {
		return isType(content, PUBLISHED);
	}

	/**
	 * Check whether this field container has the given type for any branch.
	 *
	 * @param type
	 * @return true if it matches the type, false if not
	 */
	boolean isType(HibNodeFieldContainer content, ContainerType type);

	/**
	 * Check whether this field container is the initial version for the given branch.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is the initial, false if not
	 */
	default boolean isInitial(HibNodeFieldContainer content, String branchUuid) {
		return isType(content, INITIAL, branchUuid);
	}

	/**
	 * Check whether this field container is the draft version for the given branch.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is the draft, false if not
	 */
	default boolean isDraft(HibNodeFieldContainer content, String branchUuid) {
		return isType(content, DRAFT, branchUuid);
	}

	/**
	 * Check whether this field container is the published version for the given branch.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return true if it is published, false if not
	 */
	default boolean isPublished(HibNodeFieldContainer content, String branchUuid) {
		return isType(content, PUBLISHED, branchUuid);
	}

	/**
	 * Check whether this field container has the given type in the given branch.
	 *
	 * @param type
	 * @param branchUuid
	 * @return true if it matches the type, false if not
	 */
	boolean isType(HibNodeFieldContainer content, ContainerType type, String branchUuid);

	/**
	 * Get the branch Uuids for which this container is the container of given type.
	 *
	 * @param type
	 *            type
	 * @return set of branch Uuids (may be empty, but never null)
	 */
	Set<String> getBranches(HibNodeFieldContainer content, ContainerType type);

	/**
	 * Compare the container values of both containers and return a list of differences.
	 *
	 * @param container
	 */
	List<FieldContainerChange> compareTo(HibNodeFieldContainer content, HibNodeFieldContainer container);

	/**
	 * Compare the values of this container with the values of the given fieldmap and return a list of detected differences.
	 *
	 * @param fieldMap
	 * @return
	 */
	List<FieldContainerChange> compareTo(HibNodeFieldContainer content, FieldMap fieldMap);

	/**
	 * Return the schema version for the given content
	 * 
	 * @param content
	 * @return
	 */
	HibSchemaVersion getSchemaContainerVersion(HibNodeFieldContainer content);

	/**
	 * Get all micronode fields that have a micronode using the given microschema container version.
	 *
	 * @param version
	 *            microschema container version
	 * @return list of micronode fields
	 */
	List<HibMicronodeField> getMicronodeFields(HibNodeFieldContainer content, HibMicroschemaVersion version);

	/**
	 * Get all micronode list fields that have at least one micronode using the given microschema container version.
	 *
	 * @param version
	 *            microschema container version
	 * @return list of micronode list fields
	 */
	Result<HibMicronodeFieldList> getMicronodeListFields(HibNodeFieldContainer content, HibMicroschemaVersion version);

	/**
	 * Return the ETag for the field container.
	 *
	 * @param ac
	 * @return Generated entity tag
	 */
	String getETag(HibNodeFieldContainer content, InternalActionContext ac);

	/**
	 * Determine the display field value by checking the schema and the referenced field and store it as a property.
	 */
	void updateDisplayFieldValue(HibNodeFieldContainer content);

	/**
	 * Returns the segment field value of this container.
	 *
	 * @return Determined segment field value or null if no segment field was specified or yet set
	 */
	String getSegmentFieldValue(HibNodeFieldContainer content);

	/**
	 * Update the current segment field and increment any found postfix number.
	 */
	void postfixSegmentFieldValue(HibNodeFieldContainer content);

	/**
	 * Return the URL field values for the container.
	 *
	 * @return
	 */
	Stream<String> getUrlFieldValues(HibNodeFieldContainer content);

	/**
	 * Traverse to the base node and build up the path to this container.
	 *
	 * @param ac
	 * @return
	 */
	Path getPath(HibNodeFieldContainer content, InternalActionContext ac);

	/**
	 * Create the specific delete event.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeMeshEventModel onDeleted(HibNodeFieldContainer content, String branchUuid, ContainerType type);

	/**
	 * Create the specific create event.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeMeshEventModel onCreated(HibNodeFieldContainer content, String branchUuid, ContainerType type);

	/**
	 * Create the specific update event.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeMeshEventModel onUpdated(HibNodeFieldContainer content, String branchUuid, ContainerType type);

	/**
	 * Create the taken offline event.
	 *
	 * @param branchUuid
	 * @return
	 */
	NodeMeshEventModel onTakenOffline(HibNodeFieldContainer content, String branchUuid);

	/**
	 * Create the publish event.
	 *
	 * @param branchUuid
	 * @return
	 */
	NodeMeshEventModel onPublish(HibNodeFieldContainer content, String branchUuid);

	/**
	 * Transform the container into a version info object.
	 *
	 * @param ac
	 * @return
	 */
	VersionInfo transformToVersionInfo(HibNodeFieldContainer content, InternalActionContext ac);

	/**
	 * A container is purgeable when it is not being utilized as draft, published or initial version in any branch.
	 *
	 * @return
	 */
	boolean isPurgeable(HibNodeFieldContainer content);

	/**
	 * Check whether auto purge is enabled globally or for the schema of the container.
	 *
	 * @return
	 */
	boolean isAutoPurgeEnabled(HibNodeFieldContainer content);

	/**
	 * Purge the container from the version history and ensure that the links between versions are consistent.
	 *
	 * @param bac
	 *            Action context for the deletion process
	 */
	void purge(HibNodeFieldContainer content, BulkActionContext bac);

	/**
	 * Purge the container from the version without the use of a Bulk Action Context.
	 */
	default void purge(HibNodeFieldContainer content) {
		purge(content, new DummyBulkActionContext());
	}

	/**
	 * Return all versions.
	 *
	 * @return
	 */
	Result<HibNodeFieldContainer> versions(HibNodeFieldContainer content);

	/**
	 * Return the language tag of the field container.
	 *
	 * @return
	 */
	String getLanguageTag(HibNodeFieldContainer content);

	/**
	 * Set the language for the field container.
	 *
	 * @param languageTag
	 */
	void setLanguageTag(HibNodeFieldContainer content, String languageTag);

	/**
	 * Return an iterator over the edges for the given type and branch.
	 * @param type
	 * @param branchUuid
	 * @return
	 */
	Iterator<? extends HibNodeFieldContainerEdge> getContainerEdge(HibNodeFieldContainer container, ContainerType type, String branchUuid);

	/**
	 * Retrieve a conflicting edge for the given segment info, branch uuid and type, or null if there's no conflicting
	 * edge
	 *
	 * @param content
	 * @param segmentInfo
	 * @param branchUuid
	 * @param type
	 * @param edge
	 * @return
	 */
	HibNodeFieldContainerEdge getConflictingEdgeOfWebrootPath(HibNodeFieldContainer content, String segmentInfo, String branchUuid, ContainerType type, HibNodeFieldContainerEdge edge);

	/**
	 * 	Retrieve a conflicting edge for the given urlFieldValue, branch uuid and type, or null if there's no conflicting
	 * 	edge
	 * @param content
	 * @param edge
	 * @param urlFieldValue
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	HibNodeFieldContainerEdge getConflictingEdgeOfWebrootField(HibNodeFieldContainer content, HibNodeFieldContainerEdge edge, String urlFieldValue, String branchUuid, ContainerType type);

	/**
	 * Set the segment info which consists of :nodeUuid + "-" + segment. The property is indexed and used for the webroot path resolving mechanism.
	 *
	 * @param parentNode
	 * @param segment
	 */
	String composeSegmentInfo(HibNode parentNode, String segment);

	/**
	 * Return the field edges for the given node, branch and container type
	 * @param node
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	Result<? extends HibNodeFieldContainerEdge> getFieldEdges(HibNode node, String branchUuid, ContainerType type);
}
