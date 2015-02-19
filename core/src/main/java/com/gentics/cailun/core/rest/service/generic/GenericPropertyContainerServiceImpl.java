package com.gentics.cailun.core.rest.service.generic;

import org.apache.commons.lang3.StringUtils;

import com.gentics.cailun.core.rest.model.I18NProperties;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericNode;
import com.gentics.cailun.core.rest.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.rest.model.relationship.Translated;

public class GenericPropertyContainerServiceImpl<T extends GenericPropertyContainer> extends GenericNodeServiceImpl<T> {
	/**
	 * Adds or updates the i18n value for the given language and key with the given value.
	 * 
	 * @param language
	 *            Language for the i18n value
	 * @param key
	 *            Key of the value
	 * @param value
	 *            The i18n text value
	 */
	public void setProperty(T node, Language language, String key, String value) {

		if (node == null || StringUtils.isEmpty(key) || language == null) {
			// TODO exception? boolean return?
			return;
		}

		I18NProperties i18nProperties = node.getI18NProperties(language);
		if (i18nProperties == null) {
			i18nProperties = new I18NProperties(language);
			i18nProperties.addProperty(key, value);
			i18nProperties = i18nPropertyRepository.save(i18nProperties);
			node.getI18nTranslations().add(new Translated(node, i18nProperties, language));
		} else {
			i18nProperties.addProperty(key, value);
		}

	}

	public void setName(T node, Language language, String name) {
		setProperty(node, language, GenericPropertyContainer.NAME_KEYWORD, name);
	}
}
