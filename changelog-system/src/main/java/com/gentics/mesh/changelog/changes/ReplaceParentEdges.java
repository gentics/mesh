package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.BranchParentEntry.branchParentEntry;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.BRANCH_PARENTS_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PARENTS_KEY_PROPERTY;

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
			for (Edge edge : StreamUtil.toIterable(vertex.edges(Direction.OUT, "HAS_PARENT_NODE"))) {
				String parentUuid = edge.inVertex().<String>property("uuid").orElse(null);
				String branchUuid = edge.<String>property("branchUuid").orElse(null);
				if (branchUuid == null) {
					log.warn("Parent edge from child {} to parent {} does not have a branch uuid. Skipping this edge.",
						vertex.<String>property("uuid").orElse(null), branchUuid);
					continue;
				}
				parents.add(parentUuid);
				branchParents.add(branchParentEntry(branchUuid, parentUuid).encode());
				edge.remove();
			}
			vertex.property(PARENTS_KEY_PROPERTY, parents);
			vertex.property(BRANCH_PARENTS_KEY_PROPERTY, branchParents);
		});
	}
}
