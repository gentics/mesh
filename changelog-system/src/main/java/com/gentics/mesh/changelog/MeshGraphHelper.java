package com.gentics.mesh.changelog;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

public final class MeshGraphHelper {

	private static final String MESH_ROOT_TYPE = "MeshRootImpl";
	private static final String MESH_ROOT_LEGACY_TYPE = "com.gentics.mesh.core.data.root.impl.MeshRootImpl";

	/**
	 * Return the mesh root vertex.
	 * 
	 * @return
	 */
	public static Vertex getMeshRootVertex(Graph graph) {
		Iterator<Vertex> it = graph.vertices("@class", MESH_ROOT_TYPE);
		if (it.hasNext()) {
			return it.next();
		} else {
			Iterator<Vertex> itLegacy = graph.vertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, MESH_ROOT_LEGACY_TYPE);
			if (itLegacy.hasNext()) {
				return itLegacy.next();
			} else {
				// Legacy index less handling
				for (Vertex vertex : (Iterable<Vertex>)() -> graph.vertices()) {
					String fermaType = vertex.value("ferma_type");
					if (fermaType != null && fermaType.endsWith(MESH_ROOT_TYPE)) {
						return vertex;
					}
				}
				return null;
			}
		}
	}
}
