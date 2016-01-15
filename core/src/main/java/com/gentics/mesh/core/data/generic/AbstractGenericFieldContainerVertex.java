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

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(Language language, Class<U> classOfU) {
		return getGraphFieldContainer(language.getLanguageTag(), classOfU);
	}

	protected <U extends BasicFieldContainer> U getGraphFieldContainer(String languageTag, Class<U> classOfU) {
		U container = outE(HAS_FIELD_CONTAINER).has(TranslatedImpl.LANGUAGE_TAG_KEY, languageTag).inV().nextOrDefault(classOfU, null);
		return container;
	}

	/**
	 * Optionally creates a new field container for the given container and language.
	 * 
	 * @param language
	 * @return i18n properties vertex entity
	 */
	protected <U extends BasicFieldContainer> U getOrCreateGraphFieldContainer(Language language, Class<U> classOfU) {

		U container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(TranslatedImpl.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfU).nextOrDefault(classOfU, null);
		}

		if (container == null) {
			container = getGraph().addFramedVertex(classOfU);
			container.setLanguage(language);
			Translated edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), TranslatedImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

}