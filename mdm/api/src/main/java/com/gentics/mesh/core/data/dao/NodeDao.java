package com.gentics.mesh.core.data.dao;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
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
	 * Remove published edges for each container found
	 * @param node
	 * @param branchUuid
	 * @param bac
	 */
	void removePublishedEdges(HibNode node, String branchUuid, BulkActionContext bac);

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
	 * Check whether the node is visible in the given branch (that means has at least one DRAFT field container in the branch)
	 *
	 * @param branchUuid
	 *            branch uuid
	 * @return true if the node is visible in the branch
	 */
	boolean isVisibleInBranch(HibNode node, String branchUuid);

	/**
	 * Check if the node has a content with status = published within the given branch
	 * 
	 * @param node
	 * @param branchUuid
	 * @return
	 */
	boolean hasPublishedContent(HibNode node, String branchUuid);

	/**
	 * Transform the node information to a version list response.
	 *
	 * @param ac
	 * @return Versions response
	 */
	NodeVersionsResponse transformToVersionList(HibNode node, InternalActionContext ac);

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
	 * Update the tags of the node using the provides list of tag references.
	 *
	 * @param node
	 * @param ac
	 * @param batch
	 * @param list
	 * @return
	 */
	void updateTags(HibNode node, InternalActionContext ac, EventQueueBatch batch, List<TagReference> list);

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
	 * Set the field container to be the (only) published for the given branch.
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


	/**
	 * Delete the node from the given branch. This will also delete children from the branch.
	 *
	 * If the node is deleted from its last branch, it is (permanently) deleted from the db.
	 *
	 * @param node
	 * @param ac
	 * @param branch
	 * @param bac
	 * @param ignoreChecks
	 */
	void deleteFromBranch(HibNode node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks);

	/**
	 * Remove branch parent of the node
	 * @param node
	 * @param branchUuid
	 */
	void removeParent(HibNode node, String branchUuid);

	/**
	 * Adds reference update events to the context for all draft and published contents that reference this node.
	 *
	 * @param node
	 * @param bac
	 */
	void addReferenceUpdates(HibNode node, BulkActionContext bac);

	/**
	 * Delete the given element
	 * @param node
	 */
	void removeElement(HibNode node);

	/**
	 * Remove all edges to field container with type {@link ContainerType#INITIAL} for the specified branch uuid
	 *
	 * @param node
	 * @param initial
	 * @param branchUUID
	 */
	void removeInitialFieldContainerEdge(HibNode node, HibNodeFieldContainer initial, String branchUUID);

	/**
	 * Remove the published edge for the given language tag and branch UUID
	 *
	 * @param node
	 * @param languageTag
	 * @param branchUuid
	 */
	void removePublishedEdge(HibNode node, String languageTag, String branchUuid);

	/**
	 * Create a node tagged / untagged event.
	 *
	 * @param tag
	 * @param branch
	 * @param assignment
	 *            Type of the assignment
	 * @return
	 */
	NodeTaggedEventModel onTagged(HibNode node, HibTag tag, HibBranch branch, Assignment assignment);
	
	/**
	 * Find all the existing nodes.<br>
	 * <b>Attention: this method serves administration purposes. Don't use it for the node manipulation or general retrieval!</b><br>
	 * Use {@link NodeDao#findAll(HibProject)} with the valid project binding instead.
	 * 
	 * @return
	 */
	Stream<? extends HibNode> findAllGlobal();

	/**
	 * Get ETag part of node.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	String getSubETag(HibNode node, InternalActionContext ac);

	/**
	 * Transform the node to a reference.
	 * 
	 * @param node
	 * @param ac
	 * @return
	 */
	NodeReference transformToReference(HibNode node, InternalActionContext ac);

	/**
	 * Get the edges for the given segment info, branch uuid and container type
	 *
	 * @param baseNode
	 * @param segmentInfo
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	Iterator<? extends HibNodeFieldContainerEdge> getEdges(HibNode baseNode, String segmentInfo, String branchUuid, ContainerType type);
}
