package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.query.impl.NodeRequestParameter.EXPANDFIELDS_QUERY_PARAM_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.core.data.service.I18NUtil;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractActionContext implements ActionContext {

	private static final Logger log = LoggerFactory.getLogger(AbstractActionContext.class);

	public static final String QUERY_MAP_DATA_KEY = "queryMap";
	public static final String EXPANDED_FIELDNAMED_DATA_KEY = "expandedFieldnames";

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		Object obj = data().get(key);
		return (T) obj;
	}

	@Override
	public ActionContext put(String key, Object obj) {
		data().put(key, obj);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T fromJson(Class<?> classOfT) throws HttpStatusCodeErrorException {
		try {
			String body = getBodyAsString();
			return (T) JsonUtil.getMapper().readValue(body, classOfT);
		} catch (Exception e) {
			// throw new HttpStatusCodeErrorException(400, new I18NService().get(rc, "error_parse_request_json_error"), e);
			throw new HttpStatusCodeErrorException(BAD_REQUEST, "Error while parsing json.", e);
		}
	}

	protected Map<String, String> splitQuery() {
		data().computeIfAbsent(QUERY_MAP_DATA_KEY, map -> {
			String query = query();
			Map<String, String> queryPairs = new LinkedHashMap<String, String>();
			if (query == null) {
				return queryPairs;
			}
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				int idx = pair.indexOf("=");
				if (idx == -1) {
					continue;
				}
				try {
					queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, "Could not decode query string pair {" + pair + "}", e);
				}

			}
			return queryPairs;
		});
		return (Map<String, String>) data().get(QUERY_MAP_DATA_KEY);
	}

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @return List of languages. List can be empty.
	 */
	@Override
	public List<String> getExpandedFieldnames() {
		data().computeIfAbsent(EXPANDED_FIELDNAMED_DATA_KEY, map -> {
			List<String> expandFieldnames = new ArrayList<>();
			Map<String, String> queryPairs = splitQuery();
			if (queryPairs == null) {
				return new ArrayList<>();
			}

			String value = queryPairs.get(EXPANDFIELDS_QUERY_PARAM_KEY);
			if (value != null) {
				expandFieldnames = new ArrayList<>(Arrays.asList(value.split(",")));
			}
			return expandFieldnames;
		});
		return (List<String>) data().get(EXPANDED_FIELDNAMED_DATA_KEY);
	}

	public static Locale getLocale(String header) {
		Locale bestMatchingLocale = I18NUtil.DEFAULT_LOCALE;
		Double highesQ = 0.;
		if (header == null) {
			return I18NUtil.DEFAULT_LOCALE;
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
	public String i18n(String i18nKey, String... parameters) {
		return I18NUtil.get(this, i18nKey, parameters);
	}
}
