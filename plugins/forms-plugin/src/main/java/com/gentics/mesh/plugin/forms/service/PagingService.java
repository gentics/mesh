package com.gentics.mesh.plugin.forms.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.SortOrder;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class PagingService {
	@Inject
	public PagingService() {
	}

	/**
	 * Extracts the paging parameters from the query string.
	 * 
	 * @param rc
	 *            the RoutingContext
	 * @return a JsonObject with the paging parameters from the queryStr
	 */
	public JsonObject extractPagingParams(RoutingContext rc) {
		Integer page;
		Integer perPage;
		String sortBy;
		SortOrder sortOrder;
		try {
			page = Integer.parseInt(rc.request().getParam("page"));
		} catch (NumberFormatException ex) {
			page = null;
		}
		try {
			perPage = Integer.parseInt(rc.request().getParam("perPage"));
		} catch (NumberFormatException ex) {
			perPage = null;
		}
		sortBy = rc.request().getParam("sortBy");
		sortOrder = SortOrder.valueOfName(rc.request().getParam("sortOrder"));
		
		JsonObject pagingParams = new JsonObject();
		if (page != null) {
			pagingParams.put("page", page);
		}
		if (perPage != null) {
			pagingParams.put("perPage", perPage);
		}
		if (StringUtils.isNotBlank(sortBy)) {
			pagingParams.put("sortBy", sortBy);
		}
		if (sortOrder != null) {
			pagingParams.put("sortOrder", sortOrder);
		}
		return pagingParams;
	}

	/**
	 * Adds the paging information from the GraphQL response to the client response.
	 * 
	 * @param clientResponse
	 *            the JSON object that will be sent to the client
	 * @param graphQlRespObjWithPagingInfo
	 *            the JSON object from the GraphQL request that contains the paging info
	 */
	public void addPagingInfoToResponse(JsonObject clientResponse, JsonObject graphQlRespObjWithPagingInfo) {
		clientResponse.put("totalCount", graphQlRespObjWithPagingInfo.getValue("totalCount"));
		clientResponse.put("currentPage", graphQlRespObjWithPagingInfo.getValue("currentPage"));
		clientResponse.put("pageCount", graphQlRespObjWithPagingInfo.getValue("pageCount"));
		clientResponse.put("perPage", graphQlRespObjWithPagingInfo.getValue("perPage"));
	}
}
