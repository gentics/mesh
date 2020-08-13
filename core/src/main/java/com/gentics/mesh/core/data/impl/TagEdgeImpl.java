package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.MeshVertex.UUID_KEY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.madl.field.FieldType;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see TagEdge
 */
@GraphElement
public class TagEdgeImpl extends MeshEdgeImpl implements TagEdge {
	public static final String BRANCH_UUID_KEY = "branchUuid";

	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(edgeType(TagEdgeImpl.class.getSimpleName())
			.withField(TagEdgeImpl.BRANCH_UUID_KEY, FieldType.STRING));
		type.createType(edgeType(HAS_TAG).withSuperClazz(TagEdgeImpl.class));
	}

	/**
	 * Get the traversal for the tags assigned to the given vertex for the given branch
	 * 
	 * @param vertex
	 * @param branch
	 * @return Traversal
	 */
	public static VertexTraversal<?, ?, ?> getTagTraversal(VertexFrame vertex, HibBranch branch) {
		return vertex.outE(HAS_TAG).has(BRANCH_UUID_KEY, branch.getUuid()).inV();
	}

	/**
	 * Get the traversal for the tag assigned to the given vertex for the given branch
	 *
	 * @param vertex
	 * @param tag
	 * @param branch
	 * @return Traversal
	 */
	public static boolean hasTag(VertexFrame vertex, Tag tag, HibBranch branch) {
		return vertex.outE(HAS_TAG).has(BRANCH_UUID_KEY, branch.getUuid()).inV().has(UUID_KEY, tag.getUuid()).hasNext();
	}

	/**
	 * Get the traversal for nodes that have been tagged with the given tag in the given branch
	 * 
	 * @param tag
	 * @param branch
	 * @return Traversal
	 */
	public static VertexTraversal<?, ?, ?> getNodeTraversal(HibTag tag, HibBranch branch) {
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
