package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Translated;
import com.gentics.mesh.core.data.impl.TranslatedImpl;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.syncleus.ferma.traversals.EdgeTraversal;

public abstract class GenericFieldContainerNode<T extends AbstractResponse> extends AbstractGenericVertex<T> {

	protected <T extends BasicFieldContainer> T getFieldContainer(Language language, Class<T> classOfT) {
		T container = outE(HAS_FIELD_CONTAINER).has("languageTag", language.getLanguageTag()).inV().nextOrDefault(classOfT, null);
		return container;
	}

	/**
	 * Optionally creates a new field container for the given container and language.
	 * 
	 * @param language
	 * @return i18n properties vertex entity
	 */
	protected <T extends BasicFieldContainer> T getOrCreateFieldContainer(Language language, Class<T> classOfT) {

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
