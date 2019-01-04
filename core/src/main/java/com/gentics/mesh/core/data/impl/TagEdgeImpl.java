package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.madl.wrapper.element.WrappedVertex;
import com.gentics.madl.annotation.GraphElement;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see TagEdge
 */
@GraphElement
public class TagEdgeImpl extends MeshEdgeImpl implements TagEdge {
	public static final String BRANCH_UUID_KEY = "branchUuid";

	public static void init(LegacyDatabase db) {
		db.addEdgeType(TagEdgeImpl.class.getSimpleName(), (Class<?>) null, TagEdgeImpl.BRANCH_UUID_KEY);
		db.addEdgeType(HAS_TAG, TagEdgeImpl.class);
	}

	/**
	 * Get the traversal for the tags assigned to the given vertex for the given branch
	 * 
	 * @param vertex
	 * @param branch
	 * @return Traversal
	 */
	public static VertexTraversal<?, ?, ?> getTagTraversal(WrappedVertex vertex, Branch branch) {
		return vertex.outE(HAS_TAG).has(BRANCH_UUID_KEY, branch.getUuid()).inV();
	}

	/**
	 * Get the traversal for nodes that have been tagged with the given tag in the given branch
	 * 
	 * @param tag
	 * @param branch
	 * @return Traversal
	 */
	public static VertexTraversal<?, ?, ?> getNodeTraversal(Tag tag, Branch branch) {
		return tag.inE(HAS_TAG).has(BRANCH_UUID_KEY, branch.getUuid()).outV();
	}

	@Override
	public String getBranchUuid() {
		return property(BRANCH_UUID_KEY);
	}

	@Override
	public void setBranchUuid(String uuid) {
		property(BRANCH_UUID_KEY, uuid);
	}

}
