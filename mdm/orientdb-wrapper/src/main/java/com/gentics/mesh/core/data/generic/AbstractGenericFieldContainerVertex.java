package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.BRANCH_UUID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.EDGE_TYPE_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.LANGUAGE_TAG_KEY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.util.StreamUtil;
import com.syncleus.ferma.FramedGraph;

/**
 * Abstract implementation for field containers. This can be used by {@link NodeImpl}
 * 
 * TODO: This is a dedicated class due to history reasons and should be merged with NodeImpl
 * 
 * @param <T>
 * @param <R>
 */
public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T>> extends
	AbstractMeshCoreVertex<T> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, HibBranch branch, ContainerType type, Class<U> classOfU) {
		return getGraphFieldContainer(languageTag, branch != null ? branch.getUuid() : null, type, classOfU);
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
		Edge edge = outE(HAS_FIELD_CONTAINER)
				.has(BRANCH_UUID_KEY, branchUuid)
				.has(EDGE_TYPE_KEY, type.getCode())
				.has(LANGUAGE_TAG_KEY, languageTag)
				.nextOrNull();
		if (edge != null) {
			FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
			Vertex in = edge.inVertex();
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
		EdgeTraversal<?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag);
		if (edgeTraversal.hasNext()) {
			container = StreamUtil.toStream(edgeTraversal.next().vertices(Direction.IN))
					.filter(v -> classOfU.getSimpleName().equals(v.<String>property(TYPE_RESOLUTION_KEY).orElse(null)))
					.map(v -> getGraph().frameElementExplicit(v, classOfU))
					.findAny().orElse(null);
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