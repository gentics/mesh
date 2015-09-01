package com.gentics.mesh.core.data.service;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gentics.mesh.handler.ActionContext;

@Component
public class I18NService {

	private static final Logger log = LoggerFactory.getLogger(I18NService.class);

	public static I18NService instance;

	// TODO rename translations to mesh-core-translations
	public static final String MESH_CORE_BUNDLENAME = "translations";

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static I18NService getI18n() {
		return instance;
	}

	public static final Locale DEFAULT_LOCALE = new Locale("en", "US");

	public String get(String bundleName, Locale locale, String key) {

		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		ResourceBundle labels = ResourceBundle.getBundle("i18n." + bundleName, locale);
		return labels.getString(key);
	}

	public String get(Locale locale, String key) {
		return get(MESH_CORE_BUNDLENAME, locale, key);
	}

	public String get(Locale locale, String key, String... parameters) {
		return get(MESH_CORE_BUNDLENAME, locale, key, parameters);
	}

	public String get(String bundleName, Locale locale, String key, String... parameters) {
		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		String i18nMessage = "";
		try {
			ResourceBundle labels = ResourceBundle.getBundle("i18n." + bundleName, locale);
			MessageFormat formatter = new MessageFormat("");
			formatter.setLocale(locale);
			formatter.applyPattern(labels.getString(key));
			i18nMessage = formatter.format(parameters);
		} catch (Exception e) {
			log.error("Could not format i18n message for key {" + key + "}", e);
			i18nMessage = key;
		}
		return i18nMessage;
	}

	public String get(ActionContext ac, String key, String... parameters) {
		return get(MESH_CORE_BUNDLENAME, (Locale) ac.get("locale"), key, parameters);
	}

	public String get(String bundleName, ActionContext ac, String key, String... parameters) {
		return get(bundleName, (Locale) ac.get("locale"), key, parameters);
	}

}
