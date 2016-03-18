package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.syncleus.ferma.traversals.EdgeTraversal;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T, R>>
		extends AbstractMeshCoreVertex<T, R> {

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(Language language, Release release, Type type, Class<U> classOfU) {
		return getGraphFieldContainer(language.getLanguageTag(), release != null ? release.getUuid() : null, type, classOfU);
	}

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, String releaseUuid, Type type, Class<U> classOfU) {
		EdgeTraversal<?,?,?> traversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag);
		if (releaseUuid != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid);
		}
		if (type != null) {
			traversal = traversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		}
		U container = traversal.inV().nextOrDefault(classOfU, null);
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
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

}