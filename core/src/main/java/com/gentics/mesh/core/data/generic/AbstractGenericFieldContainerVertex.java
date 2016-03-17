package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.syncleus.ferma.traversals.EdgeTraversal;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T, R>>
		extends AbstractMeshCoreVertex<T, R> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(Language language, Class<U> classOfU) {
		return getGraphFieldContainer(language.getLanguageTag(), classOfU);
	}

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, Class<U> classOfU) {
		U container = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag).inV().nextOrDefault(classOfU, null);
		return container;
	}

	/**
	 * Optionally creates a new field container for the given container type and language.
	 * 
	 * @param language
	 *            Language of the field container
	 * @param release release to which the field container belongs (may be null)
	 * @param type type of the field container relation (may be null)
	 * @param classOfU
	 *            Container implementation class to be used for element creation
	 * @return i18n properties vertex entity
	 */
	protected <U extends BasicFieldContainer> U getOrCreateGraphFieldContainer(Language language, Release release, Type type, Class<U> classOfU) {

		// Check all existing containers in order to find existing ones
		U container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (release != null) {
			edgeTraversal = edgeTraversal.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, release.getUuid());
		}
		if (type != null) {
			edgeTraversal = edgeTraversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		}
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfU).nextOrDefault(classOfU, null);
		}

		// Create a new container if no existing one was found
		if (container == null) {
			container = getGraph().addFramedVertex(classOfU);
			container.setLanguage(language);
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
			if (release != null) {
				edge.setReleaseUuid(release.getUuid());
			}
			if (type != null) {
				edge.setType(type);
				// TODO when creating DRAFT or PUBLISHED, check whether INITIAL exists. If not, add INITIAL edge.
			}
		}

		return container;
	}

}