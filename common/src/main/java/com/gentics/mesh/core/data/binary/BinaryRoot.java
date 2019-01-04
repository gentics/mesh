package com.gentics.mesh.core.data.binary;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import org.apache.tinkerpop.gremlin.structure.Edge;

/**
 * Aggregation vertex for vertices which represent the binary.
 */
public interface BinaryRoot extends MeshVertex {

	LegacyDatabase database();

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
		Database graph = getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout", database().createComposedIndexKey(item.id(),
				id()));
		if (!edges.iterator().hasNext()) {
			addEdgeOut(item, getRootLabel());
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
		removeEdgeOut(item, getRootLabel());
	}

	public Class<? extends Binary> getPersistanceClass();

	/**
	 * Create a new binary.
	 * 
	 * @param hash
	 *            Hash sum of the binary
	 * @param size
	 *            Size in bytes
	 * @return
	 */
	Binary create(String hash, Long size);

}