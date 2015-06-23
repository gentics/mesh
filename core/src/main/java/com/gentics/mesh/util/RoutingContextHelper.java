package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.mesh.core.data.model.tinkerpop.MeshAuthUser;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.gentics.mesh.paging.PagingInfo;

public final class RoutingContextHelper {

	private static final Object LANGUAGES_QUERY_PARAM_KEY = "lang";

	public static final String QUERY_MAP_DATA_KEY = "queryMap";

//	private static final String DEPTH_PARAM_KEY = "depth";
//	private static final String TAGS_PARAM_KEY = "tags";
//	private static final String CONTENTS_PARAM_KEY = "contents";
//	private static final String CHILD_TAGS_PARAM_KEY = "childTags";

	public static MeshAuthUser getUser(RoutingContext routingContext) {
		if (routingContext.user() instanceof MeshAuthUser) {
			MeshAuthUser user = (MeshAuthUser) routingContext.user();
			return user;
		}
		//TODO i18n
		throw new HttpStatusCodeErrorException(500, "Could not load request user");
	}

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @param rc
	 * @return List of languages. List can be empty.
	 */
	public static List<String> getSelectedLanguageTags(RoutingContext rc) {
		List<String> languageTags = new ArrayList<>();
		Map<String, String> queryPairs = splitQuery(rc);
		if (queryPairs == null) {
			return new ArrayList<>();
		}
		String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
		if (value != null) {
			languageTags = new ArrayList<>(Arrays.asList(value.split(",")));
		}
		languageTags.add(MeshSpringConfiguration.getConfiguration().getDefaultLanguage());
		return languageTags;

	}

	public static Map<String, String> splitQuery(RoutingContext rc) {
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
	public static PagingInfo getPagingInfo(RoutingContext rc) {
		MultiMap params = rc.request().params();
		int page = NumberUtils.toInt(params.get("page"), 1);
		int perPage = NumberUtils.toInt(params.get("per_page"), MeshConfiguration.DEFAULT_PAGE_SIZE);
		if (page < 1) {
			throw new HttpStatusCodeErrorException(400, getI18n().get(rc, "error_invalid_paging_parameters"));
		}
		if (perPage <= 0) {
			throw new HttpStatusCodeErrorException(400, getI18n().get(rc, "error_invalid_paging_parameters"));
		}
		return new PagingInfo(page, perPage);
	}

	//	public Future<Integer> getDepthParameter(RoutingContext rc) {
	//
	//		Future<Integer> depthFut = Future.future();
	//
	//		Map<String, String> queryPairs = splitQuery(rc);
	//		String value = queryPairs.get(DEPTH_PARAM_KEY);
	//
	//		int depth = NumberUtils.toInt(value, 0);
	//		int maxDepth = MeshSpringConfiguration.getConfiguration().getMaxDepth();
	//		if (depth > maxDepth) {
	//			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_depth_max_exceeded", String.valueOf(depth), String.valueOf(maxDepth)));
	//		}
	//		depthFut.complete(depth);
	//
	//		return depthFut;
	//	}

	//	public Future<Boolean> getTagsIncludeParameter(RoutingContext rc) {
	//		Future<Boolean> tagsFut = Future.future();
	//		Map<String, String> queryPairs = splitQuery(rc);
	//		String value = queryPairs.get(TAGS_PARAM_KEY);
	//		tagsFut.complete(BooleanUtils.toBoolean(value));
	//		return tagsFut;
	//	}
	//
	//	public Future<Boolean> getChildTagIncludeParameter(RoutingContext rc) {
	//		Future<Boolean> childTagsFut = Future.future();
	//		Map<String, String> queryPairs = splitQuery(rc);
	//		String value = queryPairs.get(CHILD_TAGS_PARAM_KEY);
	//		childTagsFut.complete(BooleanUtils.toBoolean(value));
	//		return childTagsFut;
	//	}

	//	public Future<Boolean> getContentsIncludeParameter(RoutingContext rc) {
	//		Future<Boolean> contentsFut = Future.future();
	//		Map<String, String> queryPairs = splitQuery(rc);
	//		String value = queryPairs.get(CONTENTS_PARAM_KEY);
	//		contentsFut.complete(BooleanUtils.toBoolean(value));
	//		return contentsFut;
	//	}

}
