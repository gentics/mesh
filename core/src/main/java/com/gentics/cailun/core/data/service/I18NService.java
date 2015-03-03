package com.gentics.cailun.core.data.service;

import java.util.Locale;

public interface I18NService {

	public String get(Locale locale, String key);

	public String get(Locale locale, String key, String... parameters);

}
