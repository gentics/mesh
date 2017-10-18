package com.gentics.mesh.core.data.asset;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ASSET_BINARY;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;

/**
 * Aggregation vertex for vertices which represent the binary of an asset.
 */
public interface AssetBinaryRoot extends MeshVertex {

	Database database();

	/**
	 * Return an iterator of all elements. Only use this method if you know that the root->item relation only yields a specific kind of item.
	 * 
	 * @return
	 */
	default public Iterable<? extends AssetBinary> findAll() {
		return out(getRootLabel()).frameExplicit(getPersistanceClass());
	}

	/**
	 * Add the given item to the this root vertex.
	 * 
	 * @param item
	 */
	default public void addItem(AssetBinary item) {
		FramedGraph graph = getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout",
				database().createComposedIndexKey(item.getId(), getId()));
		if (!edges.iterator().hasNext()) {
			linkOut(item, getRootLabel());
		}
	}

	default public String getRootLabel() {
		return HAS_ASSET_BINARY;
	}

	/**
	 * Remove the given item from this root vertex.
	 * 
	 * @param item
	 */
	default public void removeItem(AssetBinary item) {
		unlinkOut(item, getRootLabel());
	}

	public Class<? extends AssetBinary> getPersistanceClass();

}
