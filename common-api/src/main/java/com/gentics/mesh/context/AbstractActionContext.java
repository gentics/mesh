package com.gentics.mesh.context;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.HttpQueryUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for action context.
 * 
 * @see ActionContext
 */
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

	/**
	 * Convert the response body to a POJO.
	 */
	@SuppressWarnings("unchecked")
	public <T> T fromJson(Class<?> classOfT) throws GenericRestException {
		try {
			String body = getBodyAsString();
			return (T) JsonUtil.getMapper().readValue(body, classOfT);
		} catch (Exception e) {
			// throw new HttpStatusCodeErrorException(400, new I18NService().get(rc, "error_parse_request_json_error"), e);
			throw new GenericRestException(BAD_REQUEST, "Error while parsing json.", e);
		}
	}

	@Override
	public Map<String, String> splitQuery() {
		data().computeIfAbsent(QUERY_MAP_DATA_KEY, map -> {
			return HttpQueryUtils.splitQuery(query());
		});
		return (Map<String, String>) data().get(QUERY_MAP_DATA_KEY);
	}

	/**
	 * Examine the given <code>Accept-Language</code> header and determine the local that best matches the accept language header value.
	 * 
	 * @param header
	 *            Accept-Language header value
	 * @return Best matching local or default locale if no language matches
	 */
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

				// Parse the q-value in order to determine which language has the highest priority
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
