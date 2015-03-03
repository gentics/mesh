package com.gentics.cailun.core.data.service;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

@Component
public class I18NServiceImpl implements I18NService {

	@Override
	public String get(Locale locale, String key) {
		ResourceBundle labels = ResourceBundle.getBundle("i18n.translations", locale);
		return labels.getString(key);
	}

	@Override
	public String get(Locale locale, String key, String... parameters) {
		ResourceBundle labels = ResourceBundle.getBundle("i18n.translations", locale);
		MessageFormat formatter = new MessageFormat("");
		formatter.setLocale(locale);
		formatter.applyPattern(labels.getString("group_not_found"));
		return formatter.format(parameters);
	}

}
