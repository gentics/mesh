package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

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
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex nodeRoot = meshRoot.vertices(Direction.OUT, "HAS_NODE_ROOT").next();

		log.info("Migrating node publish flag..");
		long i = 0;
		// Iterate over all nodes
		for (Vertex node : (Iterable<Vertex>) () -> nodeRoot.vertices(Direction.OUT, "HAS_NODE")) {
			migrateNode(node);
			i++;
		}
		log.info("Completed migration of {" + i + "} nodes.");

	}

	private void migrateNode(Vertex node) {

		// Extract and remove the published flag from the node
		String publishFlag = node.value("published");
		node.property("published").remove();

		// Set the published flag to all node graph field containers
		Iterator<Vertex> containers = node.vertices(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Vertex container : (Iterable<Vertex>) () -> containers) {
			if (publishFlag != null) {
				container.property("published", publishFlag);
			}
		}
		log.info("Migrated node {" + node.property("uuid") + "}");
	}

}
