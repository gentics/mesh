package com.gentics.mesh.core.data.model.generic;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_I18N_PROPERTIES;

import java.util.List;

import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;
import com.syncleus.ferma.traversals.EdgeTraversal;

public class GenericPropertyContainer extends GenericNode {

	public void setI18NProperty(Language language, String name, String value) {
		I18NProperties properties = getOrCreateI18nProperties(language);
		properties.setProperty(name, value);
	}

	//	public String getDisplayName(Language language) {
	//		I18NProperties properties = getI18nProperties(language);
	//
	//		if (properties != null) {
	//			return properties.getProperty("displayName");
	//		}
	//		return null;
	//	}

	//	public String getName(Language language) {
	//		I18NProperties properties = getI18nProperties(language);
	//		if (properties != null) {
	//			return properties.getProperty("name");
	//		}
	//		return null;
	//	}
	//
	//	public void setName(Language language, String name) {
	//		I18NProperties properties = getOrCreateI18nProperties(language);
	//		properties.setProperty("name", name);
	//	}

	//	public String getContent(Language language) {
	//		I18NProperties properties = getI18nProperties(language);
	//		if (properties != null) {
	//			return properties.getProperty("content");
	//		}
	//		return null;
	//	}

	//	public void setContent(Language language, String content) {
	//		I18NProperties properties = getOrCreateI18nProperties(language);
	//		properties.setProperty("content", content);
	//	}

	public I18NProperties getI18nProperties(Language language) {
		I18NProperties properties = outE(HAS_I18N_PROPERTIES).has("languageTag", language.getLanguageTag()).inV()
				.nextOrDefault(I18NProperties.class, null);
		return properties;
	}

	public List<? extends I18NProperties> getI18nProperties() {
		return out(HAS_I18N_PROPERTIES).has(I18NProperties.class).toListExplicit(I18NProperties.class);
	}

	/**
	 * Optionally creates a new set of i18n properties for the given container and language.
	 * 
	 * @param language
	 * @return i18n properties vertex entity
	 */
	public I18NProperties getOrCreateI18nProperties(Language language) {

		I18NProperties properties = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_I18N_PROPERTIES).has(Translated.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (edgeTraversal.hasNext()) {
			properties = edgeTraversal.next().inV().has(I18NProperties.class).nextOrDefault(I18NProperties.class, null);
		}

		if (properties == null) {
			properties = getGraph().addFramedVertex(I18NProperties.class);
			properties.setLanguage(language);
			Translated edge = addFramedEdge(HAS_I18N_PROPERTIES, properties, Translated.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return properties;
	}

	public void addI18nProperties(I18NProperties properties) {
		linkOut(properties, HAS_I18N_PROPERTIES);
		Translated edge = addFramedEdge(HAS_I18N_PROPERTIES, properties, Translated.class);
		edge.setLanguageTag(properties.getLanguage().getLanguageTag());
	}

	//	public void setDisplayName(Language language, String name) {
	//		I18NProperties properties = getOrCreateI18nProperties(language);
	//		properties.setProperty("displayName", name);
	//	}

	public String getI18nProperty(Language language, String key) {
		I18NProperties properties = getI18nProperties(language);
		if (properties == null) {
			return null;
		} else {
			return properties.getProperty(key);
		}
	}

}
