package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.tinkerpop.blueprints.Direction.IN;

import java.util.Iterator;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T>> extends
		AbstractMeshCoreVertex<T> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, HibBranch branch, ContainerType type, Class<U> classOfU) {
		return getGraphFieldContainer(languageTag, branch != null ? branch.getUuid() : null, type, classOfU);
	}

	protected Edge getGraphFieldContainerEdge(String languageTag, String branchUuid, ContainerType type) {
		Database db = mesh().database();
		FramedGraph graph = Tx.get().getGraph();
		Iterator<Edge> iterator = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_branch_type_lang", db.createComposedIndexKey(id(),
			branchUuid, type.getCode(), languageTag)).iterator();
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			return null;
		}
	}

	/**
	 * Locate the field container using the provided information.
	 * 
	 * @param languageTag
	 *            Language tag of the field container
	 * @param branchUuid
	 *            Optional branch to search within
	 * @param type
	 *            Optional type of the field container (published, draft)
	 * @param classOfU
	 * @return
	 */
	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, String branchUuid, ContainerType type,
			Class<U> classOfU) {
		Edge edge = getGraphFieldContainerEdge(languageTag, branchUuid, type);
		if (edge != null) {
			FramedGraph graph = Tx.get().getGraph();
			Vertex in = edge.getVertex(IN);
			return graph.frameElementExplicit(in, classOfU);
		} else {
			return null;
		}

	}

	/**
	 * Optionally creates a new field container for the given container type and language.
	 * 
	 * @param languageTag
	 *            Language of the field container
	 * @param classOfU
	 *            Container implementation class to be used for element creation
	 * @return Located field container or created field container
	 */
	protected <U extends BasicFieldContainer> U getOrCreateGraphFieldContainer(String languageTag, Class<U> classOfU) {

		// Check all existing containers in order to find existing ones
		U container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag);
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfU).nextOrDefault(classOfU, null);
		}

		// Create a new container if no existing one was found
		if (container == null) {
			container = getGraph().addFramedVertex(classOfU);
			container.setLanguageTag(languageTag);
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(languageTag);
		}

		return container;
	}

}