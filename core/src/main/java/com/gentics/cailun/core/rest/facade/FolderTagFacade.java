package com.gentics.cailun.core.rest.facade;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.repository.GlobalI18NValueRepository;
import com.gentics.cailun.core.rest.model.FolderTag;
import com.gentics.cailun.core.rest.model.I18NValue;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.relationship.Translated;

@Component
public class FolderTagFacade extends FolderTag {

	@Autowired
	private GlobalI18NValueRepository i18nValueRepository;

	public FolderTagFacade() {
		super();
	}

	public FolderTagFacade(Language language, String name) {
		super(language, name);
		setNameF(language, name);
	}

	private void setNameF(Language language, String name) {
		setI18NProperty(language, NAME_KEYWORD, name);
	}

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
	public void setI18NProperty(Language language, String key, String value) {

		if (StringUtils.isEmpty(key) || language == null) {
			// TODO exception? boolean return?
			return;
		}

		I18NValue i18nValue = getI18NValue(language, key);
		if (i18nValue == null) {
			i18nValue = new I18NValue(language, key, value);
			i18nValue = i18nValueRepository.save(i18nValue);
			getI18nTranslations().add(new Translated(this, i18nValue, language));
		} else {
			i18nValue.setValue(value);
		}

	}

	private static final long serialVersionUID = -7294973142680141977L;

}
