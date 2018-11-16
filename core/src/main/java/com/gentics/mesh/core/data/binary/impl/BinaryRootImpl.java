package com.gentics.mesh.core.data.binary.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BINARY;

import java.util.Iterator;

import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.BinaryRoot;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * @see BinaryRoot
 */
public class BinaryRootImpl extends MeshVertexImpl implements BinaryRoot {

	public static void init(Database database) {
		database.addVertexType(BinaryRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_BINARY, true, false, true);
	}

	@Override
	public Database database() {
		return MeshInternal.get().database();
	}

	@Override
	public Class<? extends Binary> getPersistanceClass() {
		return BinaryImpl.class;
	}

	@Override
	public Binary create(String sha512sum, Long size) {
		Binary binary = getGraph().addFramedVertex(BinaryImpl.class);
		binary.setSHA512Sum(sha512sum);
		binary.setSize(size);
		addItem(binary);
		return binary;
	}

	@Override
	public Binary findByHash(String hash) {

		FramedGraph graph = Tx.getActive().getGraph();
		// 1. Find the element with given uuid within the whole graph
		Iterator<Vertex> it = database().getVertices(getPersistanceClass(), new String[] { Binary.SHA512SUM_KEY }, new String[] { hash });
		if (it.hasNext()) {
			Vertex potentialElement = it.next();
			// 2. Use the edge index to determine whether the element is part of this root vertex
			Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout", database().createComposedIndexKey(potentialElement
					.getId(), id()));
			if (edges.iterator().hasNext()) {
				return graph.frameElementExplicit(potentialElement, getPersistanceClass());
			}
		}
		return null;
		// TODO use index
		// return out(HAS_BINARY).has(Binary.SHA512SUM_KEY, hash).nextOrDefaultExplicit(BinaryImpl.class, null);
	}

}
