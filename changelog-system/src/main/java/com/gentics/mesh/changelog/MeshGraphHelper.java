package com.gentics.mesh.changelog;

import java.util.Iterator;

import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

public class MeshGraphHelper {

	private static final String MESH_ROOT_TYPE = "MeshRootImpl";
	private static final String MESH_ROOT_LEGACY_TYPE = "com.gentics.mesh.core.data.root.impl.MeshRootImpl";
	private static final String MESH_SEARCH_QUEUE_ENTRY_TYPE = "SearchQueueEntryImpl";

	/**
	 * Return the mesh root vertex.
	 * 
	 * @return
	 */
	public static Vertex getMeshRootVertex(TransactionalGraph graph) {
		Iterator<Vertex> it = graph.getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, MESH_ROOT_TYPE).iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			Iterator<Vertex> itLegacy = graph.getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, MESH_ROOT_LEGACY_TYPE).iterator();
			if (itLegacy.hasNext()) {
				return itLegacy.next();
			} else {
				// Legacy index less handling
				for (Vertex vertex : graph.getVertices()) {
					if (MESH_ROOT_LEGACY_TYPE.equals(vertex.getProperty("ferma_type"))) {
						return vertex;
					}
				}
				return null;
			}
		}
	}

	/**
	 * Add a new search queue batch and entry which will trigger a full reindex of all elements for the given type.
	 * 
	 * @param elementType
	 */
	public static void addFullReindexEntry(TransactionalGraph graph, String elementType) {
		Vertex meshRootVertex = getMeshRootVertex(graph);
		Vertex searchQueueRoot = meshRootVertex.getVertices(Direction.OUT, "HAS_SEARCH_QUEUE_ROOT").iterator().next();

		// 1. Add batch
		Vertex batch = graph.addVertex(null);
		batch.setProperty("batch_id", UUIDUtil.randomUUID());
		searchQueueRoot.addEdge("HAS_BATCH", batch);

		// 2. Add entry to batch 
		Vertex entry = graph.addVertex(null);
		entry.setProperty("element_type", elementType);
		entry.setProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, MESH_SEARCH_QUEUE_ENTRY_TYPE);
		entry.setProperty("element_action", "reindex_all");
		batch.addEdge("HAS_ITEM", entry);
	}

}
