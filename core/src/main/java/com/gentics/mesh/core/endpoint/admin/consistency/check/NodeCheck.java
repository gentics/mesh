package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

/**
 * Node specific consistency checks.
 */
public class NodeCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "nodes";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, NodeImpl.class, (node, result) -> {
			checkNode(node, result);
		}, attemptRepair, tx);
	}

	private void checkNode(Node node, ConsistencyCheckResult result) {
		String uuid = node.getUuid();

		checkOut(node, ASSIGNED_TO_PROJECT, ProjectImpl.class, result, HIGH);
		checkOut(node, HAS_SCHEMA_CONTAINER, SchemaContainerImpl.class, result, HIGH);
		// checkOut(node, HAS_CREATOR, UserImpl.class, response, MEDIUM);

		boolean isBaseNode = false;
		Project project = node.out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
		if (project != null) {
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

		if (!isBaseNode) {
			checkOut(node, HAS_PARENT_NODE, NodeImpl.class, result, HIGH);
		}

		if (node.getCreationDate() == null) {
			result.addInconsistency("The node has no creation date", uuid, MEDIUM);
		}

		Iterable<? extends NodeGraphFieldContainer> initialIterator = node.getGraphFieldContainersIt(ContainerType.INITIAL);
		if (!initialIterator.iterator().hasNext()) {
			result.addInconsistency("The node has no initial field containers", uuid, HIGH);
		}
		for (ContainerType type : ContainerType.values()) {
			checkGraphFieldContainerUniqueness(node, type, result);
		}
	}

	/**
	 * Check that the node has not more than one GFC of the type for each release
	 * 
	 * @param node
	 *            node
	 * @param type
	 *            GFC type
	 * @param response
	 *            check response
	 */
	private void checkGraphFieldContainerUniqueness(Node node, ContainerType type, ConsistencyCheckResult result) {
		String uuid = node.getUuid();

		Set<String> languageAndReleaseSet = new HashSet<>();
		for (GraphFieldContainerEdgeImpl edge : node.outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode())
			.frameExplicit(GraphFieldContainerEdgeImpl.class)) {
			String languageAndRelease = String.format("%s - %s", edge.getReleaseUuid(), edge.getLanguageTag());
			if (languageAndReleaseSet.contains(languageAndRelease)) {
				result.addInconsistency(String.format("The node has more than one GFC of type %s, language %s for release %s", type,
					edge.getLanguageTag(), edge.getReleaseUuid()), uuid, HIGH);
			} else {
				languageAndReleaseSet.add(languageAndRelease);
			}
		}
	}
}
