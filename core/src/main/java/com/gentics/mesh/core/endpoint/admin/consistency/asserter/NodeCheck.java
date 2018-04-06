package com.gentics.mesh.core.endpoint.admin.consistency.asserter;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Node specific consistency checks.
 */
public class NodeCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
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

		List<? extends NodeGraphFieldContainer> initialContainers = node.getAllInitialGraphFieldContainers();
		if (initialContainers.isEmpty()) {
			response.addInconsistency("The node has no initial field containers", uuid, HIGH);
		}
	}

}
