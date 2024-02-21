package com.gentics.mesh.changelog.changes;

import java.util.HashSet;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.util.StreamUtil;

/**
 * Changelog entry which removes the HAS_PARENT_NODE edges.
 */
public class ReplaceParentEdges extends AbstractChange {

	@Override
	public String getUuid() {
		return "FC6F16B67721469BAF16B67721069B88";
	}

	@Override
	public String getName() {
		return "ReplaceParentEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces parent edges of all nodes.";
	}

	@Override
	public void applyInTx() {
		iterateWithCommit(StreamUtil.toIterable(getGraph().vertices("@class", "NodeImpl")), vertex -> {
			Set<String> parents = new HashSet<>();
			Set<String> branchParents = new HashSet<>();
			for (Edge edge : vertex.getEdges(Direction.OUT, "HAS_PARENT_NODE")) {
				String parentUuid = edge.getVertex(Direction.IN).getProperty("uuid");
				String branchUuid = edge.getProperty("branchUuid");
				if (branchUuid == null) {
					log.warn("Parent edge from child {} to parent {} does not have a branch uuid. Skipping this edge.",
						vertex.getProperty("uuid"), branchUuid);
					continue;
				}
				parents.add(parentUuid);
				branchParents.add(branchParentEntry(branchUuid, parentUuid).encode());
				edge.remove();
			}
			vertex.setProperty(PARENTS_KEY_PROPERTY, parents);
			vertex.setProperty(BRANCH_PARENTS_KEY_PROPERTY, branchParents);
		});
	}
}
