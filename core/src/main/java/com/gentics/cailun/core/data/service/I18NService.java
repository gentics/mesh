package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.core.RoutingContext;

import java.util.Locale;

public interface I18NService {

	public String get(Locale locale, String key);
	
	public String get(RoutingContext rc, String key);

	public String get(Locale locale, String key, String... parameters);

	public String get(RoutingContext rc, String key, String... parameters);

	public Locale getLocale(RoutingContext rc);

}
