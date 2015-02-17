package com.gentics.cailun.core.rest.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GlobalI18NValueRepository;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.I18NValue;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.relationship.Translated;

@Component
@Transactional
public class CaiLunNodeServiceImpl {

	@Autowired
	GlobalI18NValueRepository i18nValueRepository;

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
	public void setI18NProperty(CaiLunNode node, Language language, String key, String value) {

		if (StringUtils.isEmpty(key) || language == null) {
			// TODO exception? boolean return?
			return;
		}

		I18NValue i18nValue = node.getI18NValue(language, key);
		if (i18nValue == null) {
			i18nValue = new I18NValue(language, key, value);
			i18nValue = i18nValueRepository.save(i18nValue);
			node.getI18nTranslations().add(new Translated(node, i18nValue, language));
		} else {
			i18nValue.setValue(value);
		}

	}

	public void setName(CaiLunNode node, Language language, String name) {
		setI18NProperty(node, language, CaiLunNode.NAME_KEYWORD, name);
	}
}
