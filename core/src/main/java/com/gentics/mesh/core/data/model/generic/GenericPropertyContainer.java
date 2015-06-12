package com.gentics.mesh.core.data.model.generic;

import java.util.List;
import java.util.NoSuchElementException;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;

public class GenericPropertyContainer extends GenericNode {

	// public List<Translated> getI18nTranslations() {
	// return outE(BasicRelationships.HAS_OBJECT_SCHEMA).toList(Translated.class);
	// }

	// TODO may be better to use I18nProperties directly
	// @Adjacency(label = BasicRelationships.HAS_OBJECT_SCHEMA, direction = com.tinkerpop.blueprints.Direction.OUT)
	// public void addI18nTranslation(Translated translation) {
	// addEdge(BasicRelationships.HAS_OBJECT_SCHEMA, Translated.class);
	// }
	// public Translated addI18nTranslation(I18NProperties tagProps, Language language) {
	// Translated translated = addEdge(BasicRelationships.HAS_I18N_PROPERTIES, tagProps, Translated.class);
	// translated.setLanguageTag(language.getLanguageTag());
	// return translated;
	// }

	public void setSchema(Schema schema) {
		setLinkOut(schema, BasicRelationships.HAS_OBJECT_SCHEMA);
	}

	public Schema getSchema() {
		return out(BasicRelationships.HAS_OBJECT_SCHEMA).next(Schema.class);
	}

	public void setProperty(Language language, String string, String string2) {

	}

	public String getDisplayName(Language language) {
		I18NProperties properties = getI18nProperties(language);

		if (properties != null) {
			return properties.getProperty("displayName");
		}
		return null;
	}

	public String getName(Language language) {
		I18NProperties properties = getI18nProperties(language);
		if (properties != null) {
			return properties.getProperty("name");
		}
		return null;
	}

	public void setName(Language language, String name) {
		I18NProperties properties = getI18nProperties(language);
		if (properties != null) {
			properties.setProperty("name", name);
		}
	}

	public String getContent(Language language) {
		I18NProperties properties = getI18nProperties(language);
		if (properties != null) {
			return properties.getProperty("content");
		}
		return null;
	}

	public void setContent(Language language, String content) {
		I18NProperties properties = getI18nProperties(language);
		if (properties != null) {
			properties.setProperty("content", content);
		}
	}

	public I18NProperties getI18nProperties(Language language) {
		I18NProperties properties = outE(BasicRelationships.HAS_I18N_PROPERTIES).has("languageTag", language.getLanguageTag()).inV()
				.next(I18NProperties.class);
		return properties;
	}

	public List<? extends I18NProperties> getI18nProperties() {
		return out(BasicRelationships.HAS_I18N_PROPERTIES).toList(I18NProperties.class);
	}

	/**
	 * Optionally creates a new set of i18n properties for the given container and language.
	 * 
	 * @param language
	 * @return i18n properties vertex entity
	 */
	public I18NProperties createI18nProperties(Language language) {
		I18NProperties properties;
		try {
			properties = outE(BasicRelationships.HAS_I18N_PROPERTIES).has(Translated.LANGUAGE_TAG_KEY, language.getLanguageTag()).next().outV()
					.next(I18NProperties.class);
		} catch (NoSuchElementException e) {
			properties = getGraph().addFramedVertex(I18NProperties.class);
			properties.setLanguage(language);
			Translated edge = addFramedEdge(BasicRelationships.HAS_I18N_PROPERTIES, properties, Translated.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return properties;
	}

	public void addI18nProperties(I18NProperties properties) {
		linkOut(properties, BasicRelationships.HAS_I18N_PROPERTIES);
		Translated edge = addFramedEdge(BasicRelationships.HAS_I18N_PROPERTIES, properties, Translated.class);
		edge.setLanguageTag(properties.getLanguage().getLanguageTag());
	}

	public void setDisplayName(Language language, String name) {
		I18NProperties properties = createI18nProperties(language);
		properties.setProperty("displayName", name);
	}

	// public I18NProperties getI18NProperties(T node, Language language) {
	// if (language == null) {
	// return null;
	// }
	// for (Translated translation : node.getI18nTranslations()) {
	// if (translation.getLanguageTag() == null) {
	// continue;
	// }
	// if (translation.getLanguageTag().equals(language.getLanguageTag())) {
	// I18NProperties i18nProperties = translation.getI18NProperties();
	// // i18nProperties = neo4jTemplate.fetch(i18nProperties);
	// return i18nProperties;
	// }
	// }
	// return null;
	// }
	//
	//
	// public void setProperty(T node, Language language, String key, String value) {
	//
	// if (language == null) {
	// throw new NullPointerException("The language for the property can't be null");
	// }
	//
	// if (node == null || StringUtils.isEmpty(key)) {
	// // TODO exception? boolean return?
	// return;
	// }
	//
	// I18NProperties i18nProperties = getI18NProperties(node, language);
	// if (i18nProperties == null) {
	// i18nProperties = create(language);
	// i18nProperties.setProperty(key, value);
	// // i18nProperties = i18nPropertyService.save(i18nProperties);
	// // node.addI18nTranslation(node, i18nProperties, language);
	// } else {
	// i18nProperties.setProperty(key, value);
	// // i18nProperties = i18nPropertyService.save(i18nProperties);
	// }
	//
	// }

	// public void setTeaser(MeshNode content, Language language, String text) {
	// setProperty(content, language, Schema.TEASER_KEYWORD, text);
	// }
	//
	// public void setTitle(MeshNode content, Language language, String text) {
	// setProperty(content, language, Schema.TITLE_KEYWORD, text);
	// }
	//
	// private I18NProperties addI18nProperties(Language language) {
	//
	//
	// I18NProperties props = .addVertex(I18NProperties.class);
	// props.setLanguage(language);
	// return props;
	// }
	// public void setContent(Language language, String text) {
	// setProperty(node, language, Schema.CONTENT_KEYWORD, text);
	// }
	//
	// public void setName(Language language, String name) {
	// setProperty(node, language, Schema.NAME_KEYWORD, name);
	// }
	//
	// public String getName( Language language) {
	// return getProperty(node, language, Schema.NAME_KEYWORD);
	// }
	//
	// public String getContent( Language language) {
	// return getProperty(node, language, Schema.CONTENT_KEYWORD);
	// }

	// public String getTeaser( Language language) {
	// return getProperty(node, language, Schema.TEASER_KEYWORD);
	// }
	//
	// public String getTitle( Language language) {
	// return getProperty(node, language, Schema.TITLE_KEYWORD);
	// }
	//
	// public String getDisplayName( Language language) {
	// return getProperty(node, language, Schema.DISPLAY_NAME_KEYWORD);
	// }

	// public void setDisplayName( Language language, String name) {
	// setProperty(node, language, Schema.DISPLAY_NAME_KEYWORD, name);
	// }

	// public String getProperty( Language language, String key) {
	// // node = neo4jTemplate.fetch(node);
	// if (language == null || StringUtils.isEmpty(key)) {
	// return null;
	// }
	// I18NProperties properties = node.getI18nProperties(language);
	// if (properties != null) {
	// return properties.getProperty(key);
	// }
	// return null;
	// }

	// public Translated create(T node, I18NProperties props, Language language) {
	// // TODO Auto-generated method stub
	// return null;
	// }

}
