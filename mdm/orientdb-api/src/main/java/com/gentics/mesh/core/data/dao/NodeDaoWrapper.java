package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
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

public interface NodeDaoWrapper extends NodeDao, DaoWrapper<HibNode>, DaoTransformable<HibNode, NodeResponse> {

	HibNode loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm);

	HibNode loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Finds a node in a project by its uuid.
	 * 
	 * @param project
	 * @param uuid
	 * @return The found node. Null if the node could not be found in the project.
	 */
	HibNode findByUuid(HibProject project, String uuid);

	/**
	 * Return the node by name.
	 * 
	 * @param project
	 * @param name
	 * @return
	 */
	// TODO remove this method. It has no meaning for nodes.
	HibNode findByName(HibProject project, String name);

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
	 * @return
	 */
	default HibNode create(HibNode parentNode, HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch) {
		return create(parentNode, creator, schemaVersion, project, branch, null);
	}

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
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(HibNode node, InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid);

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
	 * Delete the node. Please use {@link ContentDaoWrapper#deleteFromBranch(InternalActionContext, HibBranch, BulkActionContext, boolean)} if you want to
	 * delete the node just from a specific branch.
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
	 * Return the etag for the node.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	String getETag(HibNode node, InternalActionContext ac);

	/**
	 * Return API path for the node.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibNode node, InternalActionContext ac);

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
	 * Create a new node.
	 * 
	 * @param project
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibNode create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Load all nodes for the project.
	 * 
	 * @param project
	 * @return
	 */
	Result<? extends HibNode> findAll(HibProject project);

	/**
	 * Load a page of nodes.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibNode> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of nodes.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @param filter
	 * @return
	 */
	Page<? extends HibNode> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibNode> filter);

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

}
