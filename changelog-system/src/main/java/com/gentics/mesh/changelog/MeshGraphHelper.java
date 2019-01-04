package com.gentics.mesh.changelog;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.tx.Tx;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

public final class MeshGraphHelper {

	private static final String MESH_ROOT_TYPE = "MeshRootImpl";
	private static final String MESH_ROOT_LEGACY_TYPE = "com.gentics.mesh.core.data.root.impl.MeshRootImpl";

	/**
	 * Return the mesh root vertex.
	 * 
	 * @return
	 */
	public static Vertex getMeshRootVertex(Tx tx) {
		Iterator<Vertex> it = tx.vertices("@class", MESH_ROOT_TYPE).iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			Iterator<Vertex> itLegacy = tx.vertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, MESH_ROOT_LEGACY_TYPE).iterator();
			if (itLegacy.hasNext()) {
				return itLegacy.next();
			} else {
				// Legacy index less handling
				for (Vertex vertex : tx.vertices()) {
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
