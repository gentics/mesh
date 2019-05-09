package com.gentics.mesh.core.data.binary;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

/**
 * Aggregation vertex for vertices which represent the binary.
 */
public interface BinaryRoot extends MeshVertex {

	Database database();

	/**
	 * Return an iterator of all elements. Only use this method if you know that the root->item relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	default public Iterable<? extends Binary> findAll() {
		return out(getRootLabel()).frameExplicit(getPersistanceClass());
	}

	/**
	 * Add the given item to the this root vertex.
	 * 
	 * @param item
	 */
	default public void addItem(Binary item) {
		FramedGraph graph = getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout", database().createComposedIndexKey(item.id(),
			id()));
		if (!edges.iterator().hasNext()) {
			linkOut(item, getRootLabel());
		}
	}

	/**
	 * Find the binary with the given hashsum.
	 * 
	 * @param hash
	 * @return
	 */
	Binary findByHash(String hash);

	default public String getRootLabel() {
		return HAS_BINARY;
	}

	/**
	 * Remove the given item from this root vertex.
	 * 
	 * @param item
	 */
	default public void removeItem(Binary item) {
		unlinkOut(item, getRootLabel());
	}

	public Class<? extends Binary> getPersistanceClass();

	/**
	 * Create a new binary.
	 * 
	 * @param uuid
	 *            Uuid of the binary
	 * @param hash
	 *            Hash sum of the binary
	 * @param size
	 *            Size in bytes
	 * @return
	 */
	Binary create(String uuid, String hash, Long size);

	default Binary create(String hash, long size) {
		return create(UUIDUtil.randomUUID(), hash, size);
	}

}