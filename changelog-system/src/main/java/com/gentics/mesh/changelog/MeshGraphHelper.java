package com.gentics.mesh.changelog;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.madl.frame.ElementFrame;

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
	public static Vertex getMeshRootVertex(Graph graph) {
		Iterator<Vertex> it = graph.traversal().V().hasLabel(MESH_ROOT_TYPE);
		if (it.hasNext()) {
			return it.next();
		} else {
			Iterator<Vertex> itLegacy = graph.traversal().V().hasLabel(MESH_ROOT_LEGACY_TYPE);
			if (itLegacy.hasNext()) {
				return itLegacy.next();
			} else {
				// Legacy index less handling
				Iterable<Vertex> vertices = () -> graph.vertices();
				for (Vertex vertex : vertices) {
					String fermaType = vertex.<String>property(ElementFrame.TYPE_RESOLUTION_KEY).orElse(null);
					if (fermaType != null && fermaType.endsWith(MESH_ROOT_TYPE)) {
						return vertex;
					}
				}
				return null;
			}
		}
	}
}
