package com.gentics.mesh.util;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

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
	public static void debug(VertexTraversal<?, ?, ?> traversal) {
		for (MeshVertexImpl v : traversal.frameExplicit(MeshVertexImpl.class)) {
			System.out.println(v.getProperty("name") + " type: " + v.getFermaType() + " json: " + v.toJson());

		}
	}

	/**
	 * Simple debug method for a edge traversal. All edges will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(EdgeTraversal<?, ?, ?> traversal) {
		for (MeshEdgeImpl e : traversal.frameExplicit(MeshEdgeImpl.class)) {
			System.out.println(e.getElement().getId() + "from " + e.inV().next() + " to " + e.outV().next());
			System.out.println(e.label() + " type: " + e.getFermaType() + " json: " + e.toJson());
		}
	}

	/**
	 * Simple debug method for printing all existing vertices.
	 */
	public static void printDebugVertices() {
		for (VertexFrame frame : Tx.getActive().getGraph().v()) {
			System.out.println(
					frame.getId() + " " + frame.getProperty("ferma_type") + " " + frame.getProperty("name") + " " + frame.getProperty("uuid"));
		}

	}

}
