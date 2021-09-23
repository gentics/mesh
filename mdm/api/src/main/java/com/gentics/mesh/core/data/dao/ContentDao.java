package com.gentics.mesh.core.data.dao;

import java.util.Objects;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibContent;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.common.ContainerType;

/**
 * DAO for {@link HibContent}.
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
	 * Delete the node from the given branch. This will also delete children from the branch.
	 *
	 * If the node is deleted from its last branch, it is (permanently) deleted from the db.
	 *
	 * @param ac
	 * @param branch
	 * @param bac
	 * @param ignoreChecks
	 */
	void deleteFromBranch(HibNode node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks);

	/**
	 * Return the mount of elements.
	 * 
	 * @return
	 */
	long globalCount();
}
