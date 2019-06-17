package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class ChangeAddPublishFlag extends AbstractChange {

	@Override
	public String getUuid() {
		return "07F0975BD47249C6B0975BD472E9C6A4";
	}

	@Override
	public String getName() {
		return "Migrate published flag";
	}

	@Override
	public String getDescription() {
		return "Migrated the published flag from node to node language variant.";
	}

	@Override
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex nodeRoot = meshRoot.getVertices(Direction.OUT, "HAS_NODE_ROOT").iterator().next();

		log.info("Migrating node publish flag..");
		long i = 0;
		// Iterate over all nodes
		for (Vertex node : nodeRoot.getVertices(Direction.OUT, "HAS_NODE")) {
			migrateNode(node);
			i++;
		}
		log.info("Completed migration of {" + i + "} nodes.");

	}

	private void migrateNode(Vertex node) {

		// Extract and remove the published flag from the node
		String publishFlag = node.getProperty("published");
		node.removeProperty("published");

		// Set the published flag to all node graph field containers
		Iterable<Vertex> containers = node.getVertices(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Vertex container : containers) {
			if (publishFlag != null) {
				container.setProperty("published", publishFlag);
			}
		}
		log.info("Migrated node {" + node.getProperty("uuid") + "}");
	}

}
