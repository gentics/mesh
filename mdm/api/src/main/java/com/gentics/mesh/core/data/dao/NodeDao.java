package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;

/**
 * Dao for {@link HibNode}
 */
public interface NodeDao extends Dao<HibNode>, DaoTransformable<HibNode, NodeResponse>, RootDao<HibProject, HibNode> {
	/**
	 * Return the API path for the node.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibNode element, InternalActionContext ac);

	/**
	 * Create a child node in the latest branch of the project.
	 *
	 * @param parentNode
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @return
	 */
	HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project);

	/**
	 * Create a child node in the given branch
	 *
	 * @param parentNode
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @param branch
	 * @param uuid
	 * @return
	 */
	HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid);

	/**
	 * Return the children for this node for all branches.
	 *
	 * @return
	 */
	Result<? extends HibNode> getChildren(HibNode node);

	/**
	 * Return the children for this node in the given branch.
	 *
	 * @param branchUuid
	 * @return
	 */
	Result<? extends HibNode> getChildren(HibNode node, String branchUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided branch and also checks permissions.
	 */
	Stream<? extends HibNode> getChildrenStream(HibNode node, InternalActionContext ac);

	/**
	 * Returns the parent node of this node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	HibNode getParentNode(HibNode node, String branchUuid);

	/**
	 * Return a page with child nodes that are visible to the given user.
	 *
	 * @param ac
	 *            Context of the operation
	 * @param languageTags
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param pagingParameter
	 * @return
	 */
	Page<? extends HibNode> getChildren(HibNode node, InternalActionContext ac, List<String> languageTags, String branchUuid,
		ContainerType type,
		PagingParameters pagingParameter);

	/**
	 * Set the parent node of this node.
	 *
	 * @param branchUuid
	 * @param parentNode
	 */
	void setParentNode(HibNode node, String branchUuid, HibNode parentNode);

	/**
	 * Return a list of language names for draft versions in the latest branch
	 *
	 * @return
	 */
	List<String> getAvailableLanguageNames(HibNode node);

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 *
	 * @param ac
	 * @return
	 */
	String getDisplayName(HibNode node, InternalActionContext ac);

	/**
	 * Move this node into the target node.
	 *
	 * @param ac
	 * @param targetNode
	 * @param batch
	 */
	void moveTo(HibNode sourceNode, InternalActionContext ac, HibNode targetNode, EventQueueBatch batch);

	/**
	 * Transform the node into a navigation response rest model.
	 *
	 * @param ac
	 * @return
	 */
	NavigationResponse transformToNavigation(HibNode node, InternalActionContext ac);

	/**
	 * Transform the node into a publish status response rest model.
	 *
	 * @param ac
	 * @return
	 */
	PublishStatusResponse transformToPublishStatus(HibNode node, InternalActionContext ac);

	/**
	 * Publish the node (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void publish(HibNode node, InternalActionContext ac, BulkActionContext bac);

	/**
	 * Take the node offline (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac);

	/**
	 * Transform the node language into a publish status response rest model.
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	PublishStatusModel transformToPublishStatus(HibNode node, InternalActionContext ac, String languageTag);

	/**
	 * Publish a language of the node
	 *
	 * @param ac
	 * @param bac
	 * @param languageTag
	 * @return
	 */
	void publish(HibNode node, InternalActionContext ac, BulkActionContext bac, String languageTag);

	/**
	 * Create a new published version of the given language in the branch.
	 *
	 * @param node the node
	 * @param ac Action Context
	 * @param languageTag language
	 * @param branch branch
	 * @param user user
	 * @return published field container
	 */
	HibNodeFieldContainer publish(HibNode node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user);

	/**
	 * Take a language of the node offline.
	 *
	 * @param ac
	 * @param bac
	 * @param branch
	 * @param languageTag
	 */
	void takeOffline(HibNode node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag);

	/**
	 * Return the webroot path to the node in the given language. If more than one language is given, the path will lead to the first available language of the
	 * node.
	 *
	 * @param ac
	 * @param branchUuid
	 *            branch Uuid
	 * @param type
	 *            edge type
	 * @param languageTag
	 *
	 * @return
	 */
	String getPath(HibNode node, ActionContext ac, String branchUuid, ContainerType type, String... languageTag);

	/**
	 * Resolve the given path and return the path object that contains the resolved nodes.
	 *
	 * @param branchUuid
	 * @param type
	 *            edge type
	 * @param nodePath
	 * @param pathStack
	 * @return
	 */
	Path resolvePath(HibNode baseNode, String branchUuid, ContainerType type, Path nodePath, Stack<String> pathStack);

	/**
	 * Delete the node. Please use {@link ContentDao#deleteFromBranch(HibNode, InternalActionContext, HibBranch, BulkActionContext, boolean)}
	 * if you want to delete the node just from a specific branch.
	 *
	 * @param bac
	 * @param ignoreChecks
	 * @param recursive
	 */
	void delete(HibNode node, BulkActionContext bac, boolean ignoreChecks, boolean recursive);

	/**
	 * Return the breadcrumb nodes.
	 *
	 * @param ac
	 * @return Deque with breadcrumb nodes
	 */
	Result<? extends HibNode> getBreadcrumbNodes(HibNode node, InternalActionContext ac);

	/**
	 * Check whether the node is the base node of its project
	 *
	 * @return true for base node
	 */
	boolean isBaseNode(HibNode node);

	/**
	 * Check whether the node is visible in the given branch (that means has at least one DRAFT graphfieldcontainer in the branch)
	 *
	 * @param branchUuid
	 *            branch uuid
	 * @return true if the node is visible in the branch
	 */
	boolean isVisibleInBranch(HibNode node, String branchUuid);

	/**
	 * Transform the node information to a version list response.
	 *
	 * @param ac
	 * @return Versions response
	 */
	NodeVersionsResponse transformToVersionList(HibNode node, InternalActionContext ac);

	/**
	 * Update the node.
	 * 
	 * @param node
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(HibNode node, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Update the tags of the node and return a page of updated tags.
	 * 
	 * @param node
	 * @param ac
	 * @param batch
	 * @return
	 */
	Page<? extends HibTag> updateTags(HibNode node, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Load a stream of nodes with the given perms in the project.
	 * 
	 * @param project
	 * @param ac
	 * @param perm
	 * @return
	 */
	Stream<? extends HibNode> findAllStream(HibProject project, InternalActionContext ac, InternalPermission perm);

	/**
	 * Return the count of nodes for the project.
	 * 
	 * @param project
	 * @return
	 */
	long computeCount(HibProject project);

	/**
	 * Create a new node.
	 * 
	 * @param project
	 * @param user
	 * @param version
	 * @return
	 */
	HibNode create(HibProject project, HibUser user, HibSchemaVersion version);
	
	/**
	 * Find the node by UUID globally.<br>
	 * <b>Attention: this method serves administration purposes. Don't use it for the node manipulation or general retrieval!</b><br>
	 * Use {@link NodeDao#findByUuid(HibCoreElement, String)}} with the valid project binding instead.
	 * 
	 * @param uuid
	 * @return
	 */
	HibNode findByUuidGlobal(String uuid);

	/**
	 * Count all the nodes globally.<br>
	 * <b>Attention: this method serves administration purposes!</b>
	 * 
	 * @return
	 */
	long globalCount();

	/**
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(HibNode node, InternalActionContext ac, HibNodeFieldContainer container, String branchUuid);

	/**
	 * Stream the hierarchical patch of the node.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	Stream<HibNode> getBreadcrumbNodeStream(HibNode node, InternalActionContext ac);

	/**
	 * Get publish status for all languages of the node.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	Map<String, PublishStatusModel> getLanguageInfo(HibNode node, InternalActionContext ac);

	/**
	 * Method called whenever we perform a publish action (publish, take offline, move node)
	 * Should throw an exception if something is not consistent (e.g. publish node has no publish version)
	 * @param node
	 * @param ac
	 * @param branch
	 */
	void assertPublishConsistency(HibNode node, InternalActionContext ac, HibBranch branch);
}
