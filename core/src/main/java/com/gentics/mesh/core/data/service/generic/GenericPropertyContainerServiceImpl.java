package com.gentics.mesh.core.data.service.generic;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;
import com.gentics.mesh.core.repository.action.PropertyContainerActions;

public class GenericPropertyContainerServiceImpl<T extends GenericPropertyContainer> extends GenericNodeServiceImpl<T> implements
		PropertyContainerActions<T> {

	public void setProperty(T node, Language language, String key, String value) {

		if (language == null) {
			throw new NullPointerException("The language for the property can't be null");
		}

		if (node == null || StringUtils.isEmpty(key)) {
			// TODO exception? boolean return?
			return;
		}

		I18NProperties i18nProperties = getI18NProperties(node, language);
		if (i18nProperties == null) {
			i18nProperties = create(language);
			i18nProperties.setProperty(key, value);
//			i18nProperties = i18nPropertyService.save(i18nProperties);
			node.addI18nTranslation(create(node, i18nProperties, language));
		} else {
			i18nProperties.setProperty(key, value);
//			i18nProperties = i18nPropertyService.save(i18nProperties);
		}

	}

	private I18NProperties create(Language language) {
		return null;
	}

	public void setContent(T node, Language language, String text) {
		setProperty(node, language, ObjectSchema.CONTENT_KEYWORD, text);
	}

	public void setName(T node, Language language, String name) {
		setProperty(node, language, ObjectSchema.NAME_KEYWORD, name);
	}

	public String getName(T node, Language language) {
		return getProperty(node, language, ObjectSchema.NAME_KEYWORD);
	}

	public String getContent(T node, Language language) {
		return getProperty(node, language, ObjectSchema.CONTENT_KEYWORD);
	}

	public String getTeaser(T node, Language language) {
		return getProperty(node, language, ObjectSchema.TEASER_KEYWORD);
	}

	public String getTitle(T node, Language language) {
		return getProperty(node, language, ObjectSchema.TITLE_KEYWORD);
	}

	public String getDisplayName(T node, Language language) {
		return getProperty(node, language, ObjectSchema.DISPLAY_NAME_KEYWORD);
	}

	public void setDisplayName(T node, Language language, String name) {
		setProperty(node, language, ObjectSchema.DISPLAY_NAME_KEYWORD, name);
	}

	public String getProperty(T node, Language language, String key) {
		//		node = neo4jTemplate.fetch(node);
		if (language == null || StringUtils.isEmpty(key)) {
			return null;
		}
		for (Translated translation : node.getI18nTranslations()) {
			//			translation = neo4jTemplate.fetch(translation);
			if (translation.getLanguageTag().equalsIgnoreCase(language.getLanguageTag())) {
				//				I18NProperties i18nProperties = neo4jTemplate.fetch(translation.getI18NProperties());
				return translation.getI18NProperties().getProperty(key);
			}
		}
		return null;
	}

	public I18NProperties getI18NProperties(T node, Language language) {
		if (language == null) {
			return null;
		}
		for (Translated translation : node.getI18nTranslations()) {
			if (translation.getLanguageTag() == null) {
				continue;
			}
			if (translation.getLanguageTag().equals(language.getLanguageTag())) {
				I18NProperties i18nProperties = translation.getI18NProperties();
				//				i18nProperties = neo4jTemplate.fetch(i18nProperties);
				return i18nProperties;
			}
		}
		return null;
	}

	public Translated create(T node, I18NProperties props, Language language) {
		// TODO Auto-generated method stub
		return null;
	}

}
