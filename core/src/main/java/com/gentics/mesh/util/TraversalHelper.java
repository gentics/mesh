package com.gentics.mesh.util;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.madl.wrapper.element.WrappedVertex;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.gentics.madl.tx.Tx;

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
		for (MeshVertexImpl v : traversal.toListExplicit(MeshVertexImpl.class)) {
			System.out.println(v.value("name") + " type: " + v.getFermaType() + " json: " + v.toJson());

		}
	}

	/**
	 * Simple debug method for a edge traversal. All edges will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(EdgeTraversal<?, ?, ?> traversal) {
		for (MeshEdgeImpl e : traversal.toListExplicit(MeshEdgeImpl.class)) {
			System.out.println(e.getElement().id() + "from " + e.inV().next() + " to " + e.outV().next());
			System.out.println(e.label() + " type: " + e.getFermaType() + " json: " + e.toJson());
		}
	}

	/**
	 * Simple debug method for printing all existing vertices.
	 */
	public static void printDebugVertices() {
		for (WrappedVertex frame : Tx.get().v()) {
			System.out.println(
					frame.id() + " " + frame.value("ferma_type") + " " + frame.value("name") + " " + frame.value("uuid"));
		}

	}

}
