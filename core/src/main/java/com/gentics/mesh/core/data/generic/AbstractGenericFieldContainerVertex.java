package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.tinkerpop.blueprints.Direction.IN;

import java.util.Iterator;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T, R>> extends
		AbstractMeshCoreVertex<T, R> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(Language language, Release release, ContainerType type, Class<U> classOfU) {
		return getGraphFieldContainer(language.getLanguageTag(), release != null ? release.getUuid() : null, type, classOfU);
	}

	/**
	 * Locate the field container using the provided information.
	 * 
	 * @param languageTag
	 *            Language tag of the field container
	 * @param releaseUuid
	 *            Optional release to search within
	 * @param type
	 *            Optional type of the field container (published, draft)
	 * @param classOfU
	 * @return
	 */
	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, String releaseUuid, ContainerType type,
			Class<U> classOfU) {

		Database db = MeshInternal.get().database();
		FramedGraph graph = Tx.getActive().getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_release_type_lang", db.createComposedIndexKey(getId(),
				releaseUuid, type.getCode(), languageTag));
		Iterator<Edge> it = edges.iterator();
		if (it.hasNext()) {
			Vertex in = it.next().getVertex(IN);
			return graph.frameElementExplicit(in, classOfU);
		} else {
			return null;
		}

	}

	/**
	 * Optionally creates a new field container for the given container type and language.
	 * 
	 * @param language
	 *            Language of the field container
	 * @param classOfU
	 *            Container implementation class to be used for element creation
	 * @return Located field container or created field container
	 */
	protected <U extends BasicFieldContainer> U getOrCreateGraphFieldContainer(Language language, Class<U> classOfU) {

		// Check all existing containers in order to find existing ones
		U container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfU).nextOrDefault(classOfU, null);
		}

		// Create a new container if no existing one was found
		if (container == null) {
			container = getGraph().addFramedVertex(classOfU);
			container.setLanguage(language);
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

}