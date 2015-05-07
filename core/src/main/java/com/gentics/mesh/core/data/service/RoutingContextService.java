package com.gentics.mesh.core.data.service;

import io.vertx.core.Future;
import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import com.gentics.mesh.paging.PagingInfo;

public interface RoutingContextService {

	public List<String> getSelectedLanguageTags(RoutingContext rc);

	public Future<Boolean> getTagsIncludeParameter(RoutingContext rc);

	public Future<Integer> getDepthParameter(RoutingContext rc);

	public PagingInfo getPagingInfo(RoutingContext rc);

	public Future<Boolean> getChildTagIncludeParameter(RoutingContext rc);

	public Future<Boolean> getContentsIncludeParameter(RoutingContext rc);

}
