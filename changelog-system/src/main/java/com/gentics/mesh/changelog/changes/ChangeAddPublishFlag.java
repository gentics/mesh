package com.gentics.mesh.changelog.changes;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.util.StreamUtil;

/**
 * Changelog entry which migrates the publish flag. 
 */
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
		Vertex nodeRoot = meshRoot.vertices(Direction.OUT, "HAS_NODE_ROOT").next();

		log.info("Migrating node publish flag..");
		long i = 0;
		// Iterate over all nodes
		for (Vertex node : StreamUtil.toIterable(nodeRoot.vertices(Direction.OUT, "HAS_NODE"))) {
			migrateNode(node);
			i++;
		}
		log.info("Completed migration of {" + i + "} nodes.");

	}

	private void migrateNode(Vertex node) {

		// Extract and remove the published flag from the node
		String publishFlag = node.<String>property("published").orElse(null);
		node.property("published").remove();

		// Set the published flag to all node graph field containers
		Iterable<Vertex> containers = StreamUtil.toIterable(node.vertices(Direction.OUT, "HAS_FIELD_CONTAINER"));
		for (Vertex container : containers) {
			if (publishFlag != null) {
				container.property("published", publishFlag);
			}
		}
		log.info("Migrated node {" + node.<String>property("uuid") + "}");
	}

}
