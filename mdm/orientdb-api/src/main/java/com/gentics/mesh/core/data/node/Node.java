package com.gentics.mesh.core.data.node;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * The Node Domain Model interface.
 *
 * A node is the main building block for project structures. Each project has one base node which is basically a folder. Additional child nodes can be added to
 * this node and to the created nodes in order to create a project data structure. Each node may be linked to one or more {@link NodeGraphFieldContainer}
 * vertices which contain the language specific data.
 */
public interface Node extends MeshCoreVertex<NodeResponse>, CreatorTrackingVertex, ProjectElement, HibNode, GraphDBBucketableElement {

	String BRANCH_UUID_KEY = "branchUuid";

	/**
	 * Get all graph field edges.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	Result<? extends HibNodeFieldContainerEdge> getFieldContainerEdges(String branchUuid, ContainerType type);

	Result<HibNode> getChildren();

	Result<HibNode> getChildren(String branchUuid);

	/**
	 * Return the children for this node. Only fetches nodes from the provided branch and also checks permissions.
	 */
	Stream<Node> getChildrenStream(InternalActionContext ac);

	/**
	 * Gets all NodeGraphField edges that reference this node.
	 * @return
	 */
	Stream<HibNodeField> getInboundReferences();

	/**
	 * Get an existing edge.
	 *
	 * @param languageTag
	 *            language tag
	 * @param branchUuid
	 *            branch uuid
	 * @param type
	 *            edge type
	 * @return existing edge or null
	 */
	GraphFieldContainerEdge getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type);

	/**
	 * Return a page of all visible tags that are assigned to the node.
	 *
	 * @param user
	 * @param params
	 * @param branch
	 * @return Page which contains the result
	 */
	Page<? extends HibTag> getTags(HibUser user, PagingParameters params, HibBranch branch);

	/**
	 * Tests if the node is tagged with the given tag.
	 *
	 * @param tag
	 * @param branch
	 * @return
	 */
	boolean hasTag(HibTag tag, HibBranch branch);

	/**
	 * Get edges for the given webroot segment info, branch uuid and container type
	 * @param segmentInfo
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	Iterator<? extends HibNodeFieldContainerEdge> getWebrootEdges(String segmentInfo, String branchUuid, ContainerType type);

	/**
	 * Remove the element.
	 */
	void removeElement();

	/**
	 * Unparent a node from the given branch.
	 * 
	 * @param branchUuid
	 */
	void removeParent(String branchUuid);

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
	Page<HibNode> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingParameter);
	
	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	HibNodeFieldContainer getLatestDraftFieldContainer(String languageTag);

	/**
	 * Return containers of the given type
	 *
	 * @param type
	 * @return
	 */
	Result<HibNodeFieldContainer> getFieldContainers(ContainerType type);

	/**
	 * Return the field container for the given language, type and branch.
	 *
	 * @param languageTag
	 * @param branch
	 * @param type
	 *            type
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(String languageTag, HibBranch branch, ContainerType type);

    /**
     * Return traversal of graph field containers of given type for the node in the given branch.
     *
     * @param branchUuid
     * @param type
     * @return
     */
    Result<HibNodeFieldContainer> getFieldContainers(String branchUuid, ContainerType type);

	/**
	 * Return the draft field container for the given language in the latest branch.
	 *
	 * @param languageTag
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(String languageTag);

	/**
	 * Return the field container for the given language, type and branch Uuid.
	 *
	 * @param languageTag
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	HibNodeFieldContainer getFieldContainer(String languageTag, String branchUuid, ContainerType type);

	/**
	 * Return the number of field containers of the node of type DRAFT or PUBLISHED in any branch.
	 *
	 * @return
	 */
	long getFieldContainerCount();
}
