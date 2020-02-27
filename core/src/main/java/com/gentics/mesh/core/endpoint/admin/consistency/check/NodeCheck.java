package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Node specific consistency checks.
 */
public class NodeCheck extends AbstractConsistencyCheck {

	// TODO NodeCheck should be created for each consistency run
	private Set<String> branchUuids;
	private boolean attemptRepair;

	@Override
	public String getName() {
		return "nodes";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		Stream<BranchImpl> branchStream = toStream(db.getVerticesForType(BranchImpl.class));
		branchUuids = branchStream
			.map(MeshVertexImpl::getUuid)
			.collect(Collectors.toSet());
		this.attemptRepair = attemptRepair;
		return processForType(db, NodeImpl.class, (node, result) -> {
			checkNode(node, result);
		}, attemptRepair, tx);
	}

	private void checkNode(Node node, ConsistencyCheckResult result) {
		String uuid = node.getUuid();

/*
		checkOut(node, ASSIGNED_TO_PROJECT, ProjectImpl.class, result, HIGH);
		if (node.getSchemaContainer() == null) {
			result.addInconsistency("The node is not assigned to a schema", uuid, HIGH);
		}
*/
		// checkOut(node, HAS_CREATOR, UserImpl.class, response, MEDIUM);

		boolean isBaseNode = false;
		Project project = node.getProject();
		if (project == null) {
			result.addInconsistency("The node has no project", uuid, HIGH);
		} else {
			Project rootNodeProject = node.in(HAS_ROOT_NODE).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
			if (rootNodeProject != null) {
				isBaseNode = true;
				if (!project.equals(rootNodeProject)) {
					result.addInconsistency(
						String.format("The node is root node of project %s but assigned to project %s", rootNodeProject.getUuid(), project.getUuid()),
						uuid,
						HIGH);
				}
			}
		}

		if (node.getCreationDate() == null) {
			result.addInconsistency("The node has no creation date", uuid, MEDIUM);
		}

		Iterable<? extends NodeGraphFieldContainer> initialIterator = node.getGraphFieldContainers(ContainerType.INITIAL);
		if (!initialIterator.iterator().hasNext()) {
			result.addInconsistency("The node has no initial field containers", uuid, HIGH);
		}
		for (ContainerType type : ContainerType.values()) {
			checkGraphFieldContainerUniqueness(node, type, result);
		}

		checkContentsForValidBranch(node, result);

		// if the node is not the project root, it must have a parent node for every branch in which it has an initial graph field container
		if (!isBaseNode) {
			checkParentNodes(node, result);
		}
	}

	/**
	 * Check that the node has not more than one GFC of the type for each branch
	 * @param node node
	 * @param type GFC type
	 * @param result check response
	 */
	private void checkGraphFieldContainerUniqueness(Node node, ContainerType type, ConsistencyCheckResult result) {
		String uuid = node.getUuid();

		Set<String> languageAndBranchSet = new HashSet<>();
		for (GraphFieldContainerEdgeImpl edge : node.outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode())
				.frameExplicit(GraphFieldContainerEdgeImpl.class)) {
			String languageAndBranch = String.format("%s - %s", edge.getBranchUuid(), edge.getLanguageTag());
			if (languageAndBranchSet.contains(languageAndBranch)) {
				result.addInconsistency(String.format("The node has more than one GFC of type %s, language %s for branch %s", type,
						edge.getLanguageTag(), edge.getBranchUuid()), uuid, HIGH);
			} else {
				languageAndBranchSet.add(languageAndBranch);
			}
		}
	}

	/**
	 * Check existence of parent nodes in all relevant branches
	 * @param node node
	 * @param result check response
	 */
	private void checkParentNodes(Node node, ConsistencyCheckResult result) {
		Set<String> branchUuids = new HashSet<>();
		for (GraphFieldContainerEdgeImpl edge : node.outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode())
				.frameExplicit(GraphFieldContainerEdgeImpl.class)) {
			branchUuids.add(edge.getBranchUuid());
		}

		for (String branchUuid : branchUuids) {
			Node branchParent = node.getParentNode(branchUuid);
			// parent node has to exist and has to have at least one DRAFT graphfieldcontainer in the branch
			if (branchParent == null) {
				result.addInconsistency(String.format("The node does not have a parent node in branch %s", branchUuid), node.getUuid(), HIGH);
			} else if (!branchParent.isBaseNode() && !branchParent.isVisibleInBranch(branchUuid)) {
				result.addInconsistency(String.format(
						"The node references parent node %s in branch %s, but the parent node does not have any DRAFT graphfieldcontainer in the branch",
						branchParent.getUuid(), branchUuid), node.getUuid(), HIGH);
			}
		}
	}

	/**
	 * Check that each initial, draft and published version is in an existing branch.
	 * @param node
	 * @return
	 */
	private void checkContentsForValidBranch(Node node, ConsistencyCheckResult result) {
		for (GraphFieldContainerEdgeImpl edge : node.outE(HAS_FIELD_CONTAINER, GraphFieldContainerEdgeImpl.class)) {
			String branchUuid = edge.getBranchUuid();
			if (!branchUuids.contains(branchUuid)) {
				InconsistencyInfo info = new InconsistencyInfo();
				info.setDescription(String.format("The node has a content of type {%s} in the non-existing branch with uuid {%s}", edge.getType().getHumanCode(), edge.getBranchUuid()))
					.setElementUuid(node.getUuid())
					.setSeverity(InconsistencySeverity.LOW);
				if (attemptRepair) {
					edge.remove();
					info.setRepaired(true)
						.setRepairAction(RepairAction.RECOVER);
				}
				result.addInconsistency(info);
			}
		}
	}
}
