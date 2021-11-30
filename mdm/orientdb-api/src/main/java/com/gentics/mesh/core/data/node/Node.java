package com.gentics.mesh.core.data.node;

import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.Taggable;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.syncleus.ferma.EdgeFrame;

/**
 * The Node Domain Model interface.
 *
 * A node is the main building block for project structures. Each project has one base node which is basically a folder. Additional child nodes can be added to
 * this node and to the created nodes in order to create a project data structure. Each node may be linked to one or more {@link NodeGraphFieldContainer}
 * vertices which contain the language specific data.
 */
public interface Node extends MeshCoreVertex<NodeResponse>, CreatorTrackingVertex, Taggable, ProjectElement, HibNode, GraphDBBucketableElement {

	String BRANCH_UUID_KEY = "branchUuid";

	@Override
	default boolean hasPublishPermissions() {
		return true;
	}

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
	EdgeFrame getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type);
}
