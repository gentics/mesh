package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.Locale;

/**
 * The I18NService is responsible for handling internal internationalization and localization related tasks. The service is _not_ responsible for translating
 * neo4j data entities. Supported locales are currently en_US and de_DE. The default locale is en_US.
 * 
 * @author johannes2
 *
 */
public interface I18NService {

	/**
	 * * Return the i18n string for the given locale and key.
	 * 
	 * @param locale
	 *            The selected locale. The default locale en_US will be selected when the parameter is null
	 * @param key
	 *            Key of the i18n property
	 * @return locale specific i18n string
	 */
	public String get(Locale locale, String key);

	/**
	 * Return the i18n string for the given locale and key. The parameters will be used to replace variables inside the i18n string.
	 * 
	 * @param locale
	 *            The selected locale. The default locale en_US will be selected when the parameter is null
	 * @param key
	 *            Key for the i18n property
	 * @param parameters
	 *            Parameters for the i18n string variables
	 * @return locale specific i18n string
	 */
	public String get(Locale locale, String key, String... parameters);

	/**
	 * Return the i18n string key. The locale will be determined by examining the routing context locale data parameter. The parameters will be used to replace
	 * variables inside the i18n string.
	 * 
	 * @param rc
	 *            Context from which to extract the locale field
	 * @param key
	 *            Key of the i18n property
	 * @param parameters
	 *            Parameters for the i18n string variables
	 * @return locale specific i18n string
	 */
	public String get(RoutingContext rc, String key, String... parameters);

	/**
	 * Return the locale from the routing context by parsing the Accept-Language header.
	 * 
	 * @param rc
	 *            Context from which to extract the header
	 * @return best matching locale for the accept-language header or default locale when non is matching
	 */
	public Locale getLocale(RoutingContext rc);

}
