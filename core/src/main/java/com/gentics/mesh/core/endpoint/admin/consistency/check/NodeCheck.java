package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Node specific consistency checks.
 */
public class NodeCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Node> it = db.getVerticesForType(NodeImpl.class);
		while (it.hasNext()) {
			checkNode(it.next(), response);
		}
	}

	private void checkNode(Node node, ConsistencyCheckResponse response) {
		String uuid = node.getUuid();

		checkOut(node, ASSIGNED_TO_PROJECT, ProjectImpl.class, response, HIGH);
		checkOut(node, HAS_SCHEMA_CONTAINER, SchemaContainerImpl.class, response, HIGH);
		//checkOut(node, HAS_CREATOR, UserImpl.class, response, MEDIUM);

		boolean isBaseNode = false;
		Project project = node.out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
		if (project != null) {
			Project rootNodeProject = node.in(HAS_ROOT_NODE).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
			if (rootNodeProject != null) {
				isBaseNode = true;
				if (!project.equals(rootNodeProject)) {
					response.addInconsistency(
							String.format("The node is root node of project %s but assigned to project %s", rootNodeProject.getUuid(), project.getUuid()), uuid,
							HIGH);
				}
			}
		}

		if (!isBaseNode) {
			checkOut(node, HAS_PARENT_NODE, NodeImpl.class, response, HIGH);
		}

		if (node.getCreationDate() == null) {
			response.addInconsistency("The node has no creation date", uuid, MEDIUM);
		}

		Iterable<? extends NodeGraphFieldContainer> initialIterator = node.getGraphFieldContainersIt(ContainerType.INITIAL);
		if (!initialIterator.iterator().hasNext()) {
			response.addInconsistency("The node has no initial field containers", uuid, HIGH);
		}
		for (ContainerType type : ContainerType.values()) {
			checkGraphFieldContainerUniqueness(node, type, response);
		}

		// if the node is not the project root, it must have a parent node for every branch in which it has an initial graph field container
		if (!isBaseNode) {
			checkParentNodes(node, response);
		}
	}

	/**
	 * Check that the node has not more than one GFC of the type for each branch
	 * @param node node
	 * @param type GFC type
	 * @param response check response
	 */
	private void checkGraphFieldContainerUniqueness(Node node, ContainerType type, ConsistencyCheckResponse response) {
		String uuid = node.getUuid();

		Set<String> languageAndBranchSet = new HashSet<>();
		for (GraphFieldContainerEdgeImpl edge : node.outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode())
				.frameExplicit(GraphFieldContainerEdgeImpl.class)) {
			String languageAndBranch = String.format("%s - %s", edge.getBranchUuid(), edge.getLanguageTag());
			if (languageAndBranchSet.contains(languageAndBranch)) {
				response.addInconsistency(String.format("The node has more than one GFC of type %s, language %s for branch %s", type,
						edge.getLanguageTag(), edge.getBranchUuid()), uuid, HIGH);
			} else {
				languageAndBranchSet.add(languageAndBranch);
			}
		}
	}

	/**
	 * Check existence of parent nodes in all relevant branches
	 * @param node node
	 * @param response check response
	 */
	private void checkParentNodes(Node node, ConsistencyCheckResponse response) {
		Set<String> branchUuids = new HashSet<>();
		for (GraphFieldContainerEdgeImpl edge : node.outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode())
				.frameExplicit(GraphFieldContainerEdgeImpl.class)) {
			branchUuids.add(edge.getBranchUuid());
		}

		for (String branchUuid : branchUuids) {
			Node branchParent = node.getParentNode(branchUuid);
			// parent node has to exist and has to have at least one DRAFT graphfieldcontainer in the branch
			if (branchParent == null) {
				response.addInconsistency(String.format("The node does not have a parent node in branch %s", branchUuid), node.getUuid(), HIGH);
			} else if (!branchParent.isBaseNode() && !branchParent.isVisibleInBranch(branchUuid)) {
				response.addInconsistency(String.format(
						"The node references parent node %s in branch %s, but the parent node does not have any DRAFT graphfieldcontainer in the branch",
						branchParent.getUuid(), branchUuid), node.getUuid(), HIGH);
			}
		}
	}
}
