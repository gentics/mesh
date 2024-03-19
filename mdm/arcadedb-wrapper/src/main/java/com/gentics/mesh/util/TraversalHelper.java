package com.gentics.mesh.util;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.db.GraphDBTx;

/**
 * This class contains a collection of traversal methods that can be used for pagination and other traversals.
 */
public final class TraversalHelper {

	/**
	 * Simple debug method for a vertex traversal. All vertices will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 *            Traversal to be debugged
	 */
	public static void debug(VertexTraversal<?, ?> traversal) {
		for (MeshVertexImpl v : traversal.frameExplicit(MeshVertexImpl.class)) {
			System.out.println(v.getProperty("name") + " type: " + v.getFermaType() + " json: " + v.toJson());

		}
	}

	/**
	 * Simple debug method for a edge traversal. All edges will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(EdgeTraversal<?, ?> traversal) {
		for (MeshEdgeImpl e : traversal.toListExplicit(MeshEdgeImpl.class)) {
			System.out.println(e.getElement().id() + "from " + e.inV().next() + " to " + e.outV().next());
			System.out.println(e.label() + " type: " + e.getFermaType() + " json: " + e.toJson());
		}
	}

	/**
	 * Simple debug method for printing all existing vertices.
	 */
	public static void printDebugVertices() {
		for (Vertex frame : GraphDBTx.getGraphTx().getGraph().getVertices()) {
			System.out.println(
					frame.id() + " " + frame.property("ferma_type").orElse(null) + " " + frame.property("name").orElse(null) + " " + frame.property("uuid").orElse(null));
		}

	}

}
