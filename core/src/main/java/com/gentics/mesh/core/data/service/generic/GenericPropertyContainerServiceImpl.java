package com.gentics.mesh.core.data.service.generic;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.I18NProperties;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.ObjectSchema;
import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.Translated;

@Transactional(readOnly = true)
public class GenericPropertyContainerServiceImpl<T extends GenericPropertyContainer> extends GenericNodeServiceImpl<T> {

	public void setProperty(T node, Language language, String key, String value) {

		if (node == null || StringUtils.isEmpty(key) || language == null) {
			// TODO exception? boolean return?
			return;
		}

		I18NProperties i18nProperties = getI18NProperties(node, language);
		if (i18nProperties == null) {
			i18nProperties = new I18NProperties(language);
			i18nProperties.setProperty(key, value);
			i18nProperties = i18nPropertyRepository.save(i18nProperties);
			node.getI18nTranslations().add(new Translated(node, i18nProperties, language));
		} else {
			i18nProperties.setProperty(key, value);
			i18nProperties = i18nPropertyRepository.save(i18nProperties);
		}

	}

	public void setName(T node, Language language, String name) {
		if (language == null) {
			throw new NullPointerException("The language for the name can't be null");
		}
		setProperty(node, language, ObjectSchema.NAME_KEYWORD, name);
	}

	public void setContent(T node, Language language, String text) {
		setProperty(node, language, ObjectSchema.CONTENT_KEYWORD, text);
	}

	public void setFilename(T node, Language language, String filename) {
		setProperty(node, language, ObjectSchema.FILENAME_KEYWORD, filename);
	}

	public String getName(T node, Language language) {
		return getProperty(node, language, ObjectSchema.NAME_KEYWORD);
	}

	public String getContent(T node, Language language) {
		return getProperty(node, language, ObjectSchema.CONTENT_KEYWORD);
	}

	public String getTeaser(T node, Language language) {
		return getProperty(node, language, ObjectSchema.TEASER_KEY);
	}

	public String getTitle(T node, Language language) {
		return getProperty(node, language, ObjectSchema.TITLE_KEY);
	}

	public String getFilename(T node, Language language) {
		return getProperty(node, language, ObjectSchema.FILENAME_KEYWORD);
	}

	public String getProperty(T node, Language language, String key) {
		node = neo4jTemplate.fetch(node);
		if (language == null || StringUtils.isEmpty(key)) {
			return null;
		}
		for (Translated translation : node.getI18nTranslations()) {
			translation = neo4jTemplate.fetch(translation);
			if (translation.getLanguageTag().equalsIgnoreCase(language.getLanguageTag())) {
				I18NProperties i18nProperties = neo4jTemplate.fetch(translation.getI18nProperties());
				return i18nProperties.getProperty(key);
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
				I18NProperties i18nProperties = translation.getI18nProperties();
				i18nProperties = neo4jTemplate.fetch(i18nProperties);
				return i18nProperties;
			}
		}
		return null;
	}
}
