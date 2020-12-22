package com.gentics.mesh.changelog;

import java.util.Iterator;

import com.syncleus.ferma.ElementFrame;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

/**
 * Graph helper which can be used to return the mesh root vertex which is often the startpoint for changelog operation.
 */
public final class MeshGraphHelper {

	private static final String MESH_ROOT_TYPE = "MeshRootImpl";
	private static final String MESH_ROOT_LEGACY_TYPE = "com.gentics.mesh.core.data.root.impl.MeshRootImpl";

	/**
	 * Return the mesh root vertex.
	 * 
	 * @return
	 */
	public static Vertex getMeshRootVertex(TransactionalGraph graph) {
		Iterator<Vertex> it = graph.getVertices("@class", MESH_ROOT_TYPE).iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			Iterator<Vertex> itLegacy = graph.getVertices(ElementFrame.TYPE_RESOLUTION_KEY, MESH_ROOT_LEGACY_TYPE).iterator();
			if (itLegacy.hasNext()) {
				return itLegacy.next();
			} else {
				// Legacy index less handling
				for (Vertex vertex : graph.getVertices()) {
					String fermaType = vertex.getProperty("ferma_type");
					if (fermaType != null && fermaType.endsWith(MESH_ROOT_TYPE)) {
						return vertex;
					}
				}
				return null;
			}
		}
	}
}
