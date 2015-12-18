package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Translated;
import com.gentics.mesh.core.data.impl.TranslatedImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.syncleus.ferma.traversals.EdgeTraversal;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;

public abstract class AbstractGenericFieldContainerVertex<T extends AbstractResponse, R extends MeshCoreVertex<T, R>>
		extends AbstractMeshCoreVertex<T, R> {

	protected <T extends BasicFieldContainer> T getGraphFieldContainer(Language language, Class<T> classOfT) {
		T container = outE(HAS_FIELD_CONTAINER).has(TranslatedImpl.LANGUAGE_TAG_KEY, language.getLanguageTag()).inV().nextOrDefault(classOfT, null);
		return container;
	}

	/**
	 * Optionally creates a new field container for the given container and language.
	 * 
	 * @param language
	 * @return i18n properties vertex entity
	 */
	protected <T extends BasicFieldContainer> T getOrCreateGraphFieldContainer(Language language, Class<T> classOfT) {

		T container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(TranslatedImpl.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfT).nextOrDefault(classOfT, null);
		}

		if (container == null) {
			container = getGraph().addFramedVertex(classOfT);
			container.setLanguage(language);
			Translated edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), TranslatedImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

}