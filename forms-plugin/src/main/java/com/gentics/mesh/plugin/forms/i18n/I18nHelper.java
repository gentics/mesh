package com.gentics.mesh.plugin.forms.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for i18n
 */
public class I18nHelper {
	public static final Locale DEFAULT_LOCALE = new Locale("en", "US");

	private static final Logger log = LoggerFactory.getLogger(I18nHelper.class);

	/**
	 * Return the i18n string for the given locale and i18n string parameters.
	 * @param locale
	 *            Locale that will be used to identify the used language. The default locale will be used when no specified.
	 * @param key
	 *            I18n String key
	 * @param parameters
	 *            I18n String parameters
	 * 
	 * @return Translated i18n message or just i18n key if the translation could not be obtained
	 */
	public static String get(Locale locale, String key, String... parameters) {
		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		String i18nMessage = "";
		try {
			ResourceBundle labels = ResourceBundle.getBundle("i18n.translations", locale);
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
}
