package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static org.apache.tinkerpop.gremlin.structure.Direction.IN;

import java.util.Iterator;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.syncleus.ferma.Database;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.gentics.madl.tx.Tx;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T, R>> extends
		AbstractMeshCoreVertex<T, R> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(Language language, Branch branch, ContainerType type, Class<U> classOfU) {
		return getGraphFieldContainer(language.getLanguageTag(), branch != null ? branch.getUuid() : null, type, classOfU);
	}

	protected Edge getGraphFieldContainerEdge(String languageTag, String branchUuid, ContainerType type) {
		LegacyDatabase db = MeshInternal.get().database();
		Tx graph = Tx.get();
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
			Database graph = Tx.getActive().getGraph();
			Vertex in = edge.getVertex(IN);
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
			container = createVertex(classOfU);
			container.setLanguage(language);
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

}