package com.gentics.mesh.util;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.error.GenericRestException;
import io.vertx.core.MultiMap;

public final class HttpQueryUtils {

	/**
	 * Split the query in its parts and return a map which contains the individual query parameters.
	 * 
	 * @param query
	 * @return
	 */
	public static Map<String, String> splitQuery(String query) {
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
				throw new GenericRestException(INTERNAL_SERVER_ERROR, "Could not decode query string pair {" + pair + "}", e);
			}

		}
		return queryPairs;
	}

	/**
	 * Converts a Vert.x multimap to a java map. If multiple entries with the same name are found, the last entry will
	 * be used.
	 *
	 * @param map
	 * @return
	 */
	public static Map<String, String> toMap(MultiMap map) {
		return map.entries().stream().collect(Collectors.toMap(
			Map.Entry::getKey,
			Map.Entry::getValue,
			(a, b) -> b
		));
	}
}
