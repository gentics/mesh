package com.gentics.mesh.core.data.dao;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface ContentDaoWrapper extends ContentDao {
	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	NodeGraphFieldContainer getLatestDraftFieldContainer(Node node, String languageTag);

	/**
	 * Return the field container for the given language, type and branch.
	 *
	 * @param languageTag
	 * @param branch
	 * @param type
	 *            type
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Node node, String languageTag, HibBranch branch, ContainerType type);

	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Node node, String languageTag);

	/**
	 * Return the field container for the given language, type and branch Uuid.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Node node, String languageTag, String branchUuid, ContainerType type);

	/**
	 * Create a new graph field container for the given language and assign the schema version of the branch to the container. The graph field container will be
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
	NodeGraphFieldContainer createGraphFieldContainer(Node node, String languageTag, HibBranch branch, HibUser user);

	/**
	 * Like {@link #createGraphFieldContainer(Node, String, HibBranch, HibUser)}, but let the new graph field container be a clone of the given original (if not null).
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
	NodeGraphFieldContainer createGraphFieldContainer(Node node, String languageTag, HibBranch branch, HibUser editor, NodeGraphFieldContainer original,
		boolean handleDraftEdge);

	/**
	 * Return the draft field containers of the node in the latest branch.
	 *
	 * @return
	 */
	TraversalResult<NodeGraphFieldContainer> getDraftGraphFieldContainers(Node node);

	/**
	 * Return a traversal of graph field containers of given type for the node in the given branch.
	 *
	 * @param branch
	 * @param type
	 * @return
	 */
	default TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(Node node, HibBranch branch, ContainerType type) {
		return getGraphFieldContainers(node, branch.getUuid(), type);
	}

	/**
	 * Return traversal of graph field containers of given type for the node in the given branch.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(Node node, String branchUuid, ContainerType type);

	/**
	 * Return containers of the given type
	 *
	 * @param type
	 * @return
	 */
	TraversalResult<NodeGraphFieldContainer> getGraphFieldContainers(Node node, ContainerType type);

	/**
	 * Return the number of field containers of the node of type DRAFT or PUBLISHED in any branch.
	 *
	 * @return
	 */
	long getGraphFieldContainerCount(Node node);

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
	NodeGraphFieldContainer findVersion(Node node, List<String> languageTags, String branchUuid, String version);

	/**
	 * Iterate the version chain from the back in order to find the given version.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param version
	 * @return Found version or null when no version could be found.
	 */
	default NodeGraphFieldContainer findVersion(Node node, String languageTag, String branchUuid, String version) {
		return findVersion(node, Arrays.asList(languageTag), branchUuid, version);
	}

	/**
	 * Find a node field container that matches the nearest possible value for the language parameter.
	 *
	 * @param ac
	 * @param languageTags
	 * @return Next matching field container or null when no language matches
	 */
	default NodeGraphFieldContainer findVersion(Node node, InternalActionContext ac, List<String> languageTags, String version) {
		return findVersion(node, languageTags, ac.getBranch().getUuid(), version);
	}

	/**
	 * Find the content that matches the given parameters (languages, type).
	 *
	 * @param ac
	 * @param languageTags
	 * @param type
	 * @return
	 */
	default NodeGraphFieldContainer findVersion(Node node, InternalActionContext ac, List<String> languageTags, ContainerType type) {
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
	void deleteLanguageContainer(Node node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac, boolean failForLastContainer);

	/**
	 * Return the path segment value of this node preferable in the given language.
	 *
	 * If more than one language is given, the path will lead to the first available language
	 * of the node.
	 *
	 * When no language matches and <code>anyLanguage</code> is <code>true</code> the results language
	 * is nondeterministic.
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
	String getPathSegment(Node node, String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag);

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
	default String getPathSegment(Node node, String branchUuid, ContainerType type, String... languageTag) {
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
	void deleteFromBranch(Node node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks);

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
	NodeGraphFieldContainer publish(Node node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user);

	/**
	 * Gets all NodeGraphField edges that reference this node.
	 * @return
	 */
	Stream<NodeGraphField> getInboundReferences(Node node);
}
