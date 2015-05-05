package com.gentics.cailun.core.data.service;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.RoutingContext;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class RoutingContextServiceImpl implements RoutingContextService {

	@Autowired
	private I18NService i18n;

	private static final Logger log = LoggerFactory.getLogger(RoutingContextServiceImpl.class);

	private static final Object LANGUAGES_QUERY_PARAM_KEY = "lang";
	private static final String DEPTH_PARAM_KEY = "depth";
	private static final String TAGS_PARAM_KEY = "tags";
	private static final String CONTENTS_PARAM_KEY = "contents";
	private static final String CHILD_TAGS_PARAM_KEY = "childTags";
	public static final int DEFAULT_PER_PAGE = 25;
	public static final String QUERY_MAP_DATA_KEY = "queryMap";

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @param rc
	 * @return List of languages. List can be empty.
	 */
	public List<String> getSelectedLanguageTags(RoutingContext rc) {
		Map<String, String> queryPairs = splitQuery(rc);
		String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
		if (value == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(value.split(",")));

	}

	public Map<String, String> splitQuery(RoutingContext rc) {
		rc.data().computeIfAbsent(QUERY_MAP_DATA_KEY, map -> {
			String query = rc.request().query();
			Map<String, String> queryPairs = new LinkedHashMap<String, String>();
			if (query == null) {
				return queryPairs;
			}
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				int idx = pair.indexOf("=");

				try {
					queryPairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new HttpStatusCodeErrorException(500, "Could not decode query string pair {" + pair + "}", e);
				}

			}
			return queryPairs;
		});
		return (Map<String, String>) rc.data().get(QUERY_MAP_DATA_KEY);
	}

	/**
	 * Extract the paging information from the request parameters. The paging information contains information about the number of the page that is currently
	 * requested and the amount of items that should be included in a single page.
	 * 
	 * @param rc
	 * @return Paging information
	 */
	public PagingInfo getPagingInfo(RoutingContext rc) {
		MultiMap params = rc.request().params();
		int page = NumberUtils.toInt(params.get("page"), 1);
		int perPage = NumberUtils.toInt(params.get("per_page"), DEFAULT_PER_PAGE);
		if (page < 1) {
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_invalid_paging_parameters"));
		}
		if (perPage <= 0) {
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_invalid_paging_parameters"));
		}
		return new PagingInfo(page, perPage);
	}

	public Future<Integer> getDepthParameter(RoutingContext rc) {

		Future<Integer> depthFut = Future.future();

		Map<String, String> queryPairs = splitQuery(rc);
		String value = queryPairs.get(DEPTH_PARAM_KEY);

		int depth = NumberUtils.toInt(value, 0);
		int maxDepth = CaiLunSpringConfiguration.getConfiguration().getMaxDepth();
		if (depth > maxDepth) {
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_depth_max_exceeded", String.valueOf(depth), String.valueOf(maxDepth)));
		}
		depthFut.complete(depth);

		return depthFut;
	}

	public Future<Boolean> getTagsIncludeParameter(RoutingContext rc) {
		Future<Boolean> tagsFut = Future.future();
		Map<String, String> queryPairs = splitQuery(rc);
		String value = queryPairs.get(TAGS_PARAM_KEY);
		tagsFut.complete(BooleanUtils.toBoolean(value));
		return tagsFut;
	}

	@Override
	public Future<Boolean> getChildTagIncludeParameter(RoutingContext rc) {
		Future<Boolean> childTagsFut = Future.future();
		Map<String, String> queryPairs = splitQuery(rc);
		String value = queryPairs.get(CHILD_TAGS_PARAM_KEY);
		childTagsFut.complete(BooleanUtils.toBoolean(value));
		return childTagsFut;
	}

	@Override
	public Future<Boolean> getContentsIncludeParameter(RoutingContext rc) {
		Future<Boolean> contentsFut = Future.future();
		Map<String, String> queryPairs = splitQuery(rc);
		String value = queryPairs.get(CONTENTS_PARAM_KEY);
		contentsFut.complete(BooleanUtils.toBoolean(value));
		return contentsFut;
	}

}
