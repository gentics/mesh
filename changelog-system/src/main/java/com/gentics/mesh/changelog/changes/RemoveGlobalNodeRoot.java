package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.util.StreamUtil;

/**
 * Changelog entry which removed the global node root vertex.
 */
public class RemoveGlobalNodeRoot extends AbstractChange {

	@Override
	public String getName() {
		return "Global Node Root Removal";
	}

	@Override
	public String getDescription() {
		return "Remove the no longer needed global node root supernode";
	}

	@Override
	public String getUuid() {
		return "947FA1AB31FC4705BFA1AB31FCC705D7";
	}

	@Override
	public void applyInTx() {
		Vertex root = getMeshRootVertex();
		Iterable<Vertex> nodeRoots = StreamUtil.toIterable(root.vertices(OUT, "HAS_NODE_ROOT"));
		for (Vertex nodeRoot : nodeRoots) {
			nodeRoot.remove();
		}
	}

}
