package com.gentics.mesh.core.data.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central I18N Util which manages the i18n handling of messages.
 */
public class I18NUtil {

	private static final Logger log = LoggerFactory.getLogger(I18NUtil.class);

	// TODO rename translations to mesh-core-translations
	public static final String MESH_CORE_BUNDLENAME = "translations";

	public static final Locale DEFAULT_LOCALE = new Locale("en", "US");

	/**
	 * Return the i18n string for the given bundle, local and i18n key.
	 * 
	 * @param bundleName
	 *            Bundle to check for i18n entries
	 * @param locale
	 *            Locale used to determine the language
	 * @param key
	 *            I18n key
	 * @return Localized string
	 */
	public static String get(String bundleName, Locale locale, String key) {

		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		ResourceBundle labels = ResourceBundle.getBundle("i18n." + bundleName, locale, new UTF8Control());
		return labels.getString(key);
	}

	/**
	 * Return the i18n string for the given locale and key.
	 * 
	 * @param locale
	 *            Locale used to determine the language
	 * @param key
	 *            I18n key
	 * @return Translated i18n message
	 */
	public static String get(Locale locale, String key) {
		return get(MESH_CORE_BUNDLENAME, locale, key);
	}

	/**
	 * Return the i18n string for the given locale and parameters.
	 * 
	 * @param locale
	 *            Local used to determine the language
	 * @param key
	 *            I18n string key
	 * @param parameters
	 *            I18n parameters
	 * @return Translated i18n message
	 */
	public static String get(Locale locale, String key, String... parameters) {
		return get(MESH_CORE_BUNDLENAME, locale, key, parameters);
	}

	/**
	 * Return the i18n string for the given bundle name, local and i18n string parameters.
	 * 
	 * @param bundleName
	 *            Name of the bundle resource that should be used.
	 * @param locale
	 *            Locale that will be used to identify the used language. The default locale will be used when no specified.
	 * @param key
	 *            I18n String key
	 * @param parameters
	 *            I18n String parameters
	 * @return Translated i18n message or just i18n key if the translation could not be obtained
	 */
	public static String get(String bundleName, Locale locale, String key, String... parameters) {
		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		String i18nMessage = "";
		try {
			ResourceBundle labels = ResourceBundle.getBundle("i18n." + bundleName, locale, new UTF8Control());
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

	/**
	 * Return the i18n string for the i18n string and parameters. The locale information will be extracted from the action context.
	 * 
	 * @param ac
	 *            Action context which contains the locale information used to pick the right language
	 * @param key
	 *            I18n key
	 * @param parameters
	 *            I18n parameters
	 * @return Translated i18n string
	 */
	public static String get(ActionContext ac, String key, String... parameters) {
		return get(MESH_CORE_BUNDLENAME, ac.getLocale(), key, parameters);
	}

	/**
	 * Return the i18n string for the i18n string, parameters and bundle. The locale information will be extracted from the action context.
	 * 
	 * @param bundleName
	 *            Bundle to query for i18n entries
	 * @param ac
	 *            Action context
	 * @param key
	 *            I18n key
	 * @param parameters
	 *            I18n parameters
	 * @return Translated i18n string
	 */
	public static String get(String bundleName, ActionContext ac, String key, String... parameters) {
		return get(bundleName, ac.getLocale(), key, parameters);
	}

}
