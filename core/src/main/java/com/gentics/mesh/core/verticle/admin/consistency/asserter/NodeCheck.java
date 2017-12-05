package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
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

		checkOut(node, HAS_CREATOR, UserImpl.class, response, MEDIUM);

		Project project = node.getProject();
		if (project == null) {
			response.addInconsistency("The project link for the node could not be found", uuid, HIGH);
		}

		Node baseNode = project.getBaseNode();
		boolean isBaseNode = false;
		if (uuid.equals(baseNode.getUuid())) {
			isBaseNode = true;
		}

		if (node.getCreationDate() == null) {
			response.addInconsistency("The node has no creation date", uuid, MEDIUM);
		}

		// Verify that the node has a parent for all releases
		for (Release release : project.getReleaseRoot().findAllIt()) {
			Node parentNode = node.getParentNode(release.getUuid());
			if (isBaseNode && parentNode != null) {
				response.addInconsistency("The project base node must not have a parent.", uuid, HIGH);
			} else if (!isBaseNode && parentNode == null) {
				response.addInconsistency("The node must have a parent node for release {" + release.getUuid() + "}", node.getUuid(), HIGH);
			}
		}

		if (node.getSchemaContainer() == null) {
			response.addInconsistency("The node has no schema container linked to it", uuid, MEDIUM);
		}

		List<? extends NodeGraphFieldContainer> initialContainers = node.getAllInitialGraphFieldContainers();
		if (initialContainers.isEmpty()) {
			response.addInconsistency("The node has no initial field containers", uuid, HIGH);
		}

		for (Release release : project.getReleaseRoot().findAllIt()) {
			List<? extends NodeGraphFieldContainer> draftContainers = node.getGraphFieldContainers(release, ContainerType.DRAFT);
			if (draftContainers.isEmpty()) {
				response.addInconsistency("The node did not have any draft containers within release {" + release.getUuid() + "}", uuid, HIGH);
			}
		}

	}

}
