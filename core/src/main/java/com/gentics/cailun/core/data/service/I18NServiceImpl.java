package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.core.RoutingContext;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gentics.cailun.cli.CaiLun;

@Component
public class I18NServiceImpl implements I18NService {

	private static final Logger log = LoggerFactory.getLogger(I18NServiceImpl.class);

	private static final Locale DEFAULT_LOCALE = new Locale("en", "US");

	@Override
	public String get(Locale locale, String key) {
		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		ResourceBundle labels = ResourceBundle.getBundle("i18n.translations", locale);
		return labels.getString(key);
	}

	@Override
	public String get(Locale locale, String key, String... parameters) {
		if (locale == null) {
			locale = DEFAULT_LOCALE;
		}
		ResourceBundle labels = ResourceBundle.getBundle("i18n.translations", locale);
		MessageFormat formatter = new MessageFormat("");
		formatter.setLocale(locale);
		formatter.applyPattern(labels.getString("group_not_found"));
		return formatter.format(parameters);
	}

	public Locale getLocale(RoutingContext rc) {
		String header = rc.request().headers().get("Accept-Language");
		return getLocale(header);
	}

	protected Locale getLocale(String header) {
		Locale bestMatchingLocale = DEFAULT_LOCALE;
		Double highesQ = 0.;
		if (header == null) {
			return DEFAULT_LOCALE;
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Parsing accept language header value {" + header + "}");
			}
			for (String str : header.split(",")) {
				String[] arr = str.trim().replace("-", "_").split(";");

				// Parse the locale
				String[] l = arr[0].split("_");
				// We only care for german and english
				if ((!l[0].startsWith("de")) && (!l[0].startsWith("en"))) {
					if (log.isDebugEnabled()) {
						log.debug("Skipping language {" + l[0] + "}. We only support german or english.");
					}
					continue;
				}

				// Parse the q-value
				Double q = 1.0D;
				for (String s : arr) {
					s = s.trim();
					if (s.startsWith("q=")) {
						q = Double.parseDouble(s.substring(2).trim());
						break;
					}
				}
				if (q > highesQ) {
					highesQ = q;
				} else {
					continue;
				}

				switch (l.length) {
				case 2:
					bestMatchingLocale = new Locale(l[0], l[1]);
					break;
				case 3:
					bestMatchingLocale = new Locale(l[0], l[1], l[2]);
					break;
				default:
					bestMatchingLocale = new Locale(l[0]);
					break;
				}

			}
			if (log.isDebugEnabled()) {
				log.debug("Found best matching locale {" + bestMatchingLocale + "} with q value {" + highesQ + "}");
			}
		}
		return bestMatchingLocale;
	}

	@Override
	public String get(RoutingContext rc, String key, String... parameters) {
		return get(rc.get("locale"), key, parameters);
	}

	@Override
	public String get(RoutingContext rc, String key) {
		return get(rc.get("locale"), key);
	}
}
