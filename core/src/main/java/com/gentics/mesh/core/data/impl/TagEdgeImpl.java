package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;

import com.gentics.ferma.annotation.GraphElement;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see TagEdge
 */
@GraphElement
public class TagEdgeImpl extends AbstractEdgeFrame implements TagEdge {
	public static final String RELEASE_UUID_KEY = "releaseUuid";

	public static void init(Database db) {
		db.addEdgeType(TagEdgeImpl.class.getSimpleName(), (Class<?>) null, TagEdgeImpl.RELEASE_UUID_KEY);
		db.addEdgeType(HAS_TAG, TagEdgeImpl.class);
	}

	/**
	 * Get the traversal for the tags assigned to the given vertex for the given release
	 * 
	 * @param vertex
	 * @param release
	 * @return
	 */
	public static VertexTraversal<?, ?, ?> getTagTraversal(VertexFrame vertex, Release release) {
		return vertex.outE(HAS_TAG).has(RELEASE_UUID_KEY, release.getUuid()).inV().has(TagImpl.class);
	}

	/**
	 * Get the traversal for nodes that have been tagged with the given tag in the given release
	 * 
	 * @param tag
	 * @param release
	 * @return
	 */
	public static VertexTraversal<?, ?, ?> getNodeTraversal(Tag tag, Release release) {
		return tag.inE(HAS_TAG).has(RELEASE_UUID_KEY, release.getUuid()).outV().has(NodeImpl.class);
	}

	@Override
	public String getReleaseUuid() {
		return getProperty(RELEASE_UUID_KEY);
	}

	@Override
	public void setReleaseUuid(String uuid) {
		setProperty(RELEASE_UUID_KEY, uuid);
	}

}
