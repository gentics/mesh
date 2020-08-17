package com.gentics.mesh.core.data.dao;

import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface NodeDaoWrapper extends NodeDao, DaoWrapper<Node>, DaoTransformable<Node, NodeResponse> {

	/**
	 * Create a child node in the latest branch of the project.
	 *
	 * @param parentNode
	 * @param creator
	 * @param schemaVersion
	 * @param project
	 * @return
	 */
	Node create(Node parentNode, HibUser creator, SchemaVersion schemaVersion, HibProject project);

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
	default Node create(Node parentNode, HibUser creator, SchemaVersion schemaVersion, HibProject project, HibBranch branch) {
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
	Node create(Node parentNode, HibUser creator, SchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid);

	/**
	 * Return the children for this node for all branches.
	 *
	 * @return
	 */
	TraversalResult<Node> getChildren(Node node);

	/**
	 * Return the children for this node in the given branch.
	 *
	 * @param branchUuid
	 * @return
	 */
	TraversalResult<Node> getChildren(Node node, String branchUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided branch and also checks permissions.
	 */
	Stream<Node> getChildrenStream(Node node, InternalActionContext ac);

	/**
	 * Returns the parent node of this node.
	 *
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Node getParentNode(Node node, String branchUuid);

	/**
	 * Set the parent node of this node.
	 *
	 * @param branchUuid
	 * @param parentNode
	 */
	void setParentNode(Node node, String branchUuid, Node parentNode);

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
	TransformablePage<Node> getChildren(Node node, InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingParameter);

	/**
	 * Return a list of language names for draft versions in the latest branch
	 *
	 * @return
	 */
	List<String> getAvailableLanguageNames(Node node);

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 *
	 * @param ac
	 * @return
	 */
	String getDisplayName(Node node, InternalActionContext ac);

	/**
	 * Move this node into the target node.
	 *
	 * @param ac
	 * @param targetNode
	 * @param batch
	 */
	void moveTo(Node sourceNode, InternalActionContext ac, Node targetNode, EventQueueBatch batch);

	/**
	 * Transform the node into a navigation response rest model.
	 *
	 * @param ac
	 * @return
	 */
	NavigationResponse transformToNavigation(Node node, InternalActionContext ac);

	/**
	 * Transform the node into a publish status response rest model.
	 *
	 * @param ac
	 * @return
	 */
	PublishStatusResponse transformToPublishStatus(Node node, InternalActionContext ac);

	/**
	 * Publish the node (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void publish(Node node, InternalActionContext ac, BulkActionContext bac);

	/**
	 * Take the node offline (all languages)
	 *
	 * @param ac
	 * @param bac
	 * @return
	 */
	void takeOffline(Node node, InternalActionContext ac, BulkActionContext bac);

	/**
	 * Transform the node language into a publish status response rest model.
	 *
	 * @param ac
	 * @param languageTag
	 * @return
	 */
	PublishStatusModel transformToPublishStatus(Node node, InternalActionContext ac, String languageTag);

	/**
	 * Publish a language of the node
	 *
	 * @param ac
	 * @param bac
	 * @param languageTag
	 * @return
	 */
	void publish(Node node, InternalActionContext ac, BulkActionContext bac, String languageTag);

	/**
	 * Set the graph field container to be the (only) published for the given branch.
	 *
	 * @param ac
	 * @param container
	 * @param branchUuid
	 */
	void setPublished(Node node, InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid);

	/**
	 * Take a language of the node offline.
	 *
	 * @param ac
	 * @param bac
	 * @param branch
	 * @param languageTag
	 */
	void takeOffline(Node node, InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag);

}
