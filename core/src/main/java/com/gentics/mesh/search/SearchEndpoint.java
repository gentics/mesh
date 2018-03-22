package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.rest.InternalEndpoint;
import com.gentics.mesh.rest.InternalEndpointRoute;

public interface SearchEndpoint extends InternalEndpoint {

	/**
	 * Register the search handler which will directly return the search response without any post processing by mesh.
	 * 
	 * @param typeName
	 * @param searchHandler
	 */
	default void registerRawSearchHandler(String typeName, SearchHandler<?, ?> searchHandler) {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return the unmodified Elasticsearch response. Note that the query will be executed using the multi search API of Elasticsearch.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createSearchResponse(), "Raw search response.");
		endpoint.exampleRequest(miscExamples.getSearchQueryExample());
		endpoint.handler(rc -> {
			try {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				searchHandler.rawQuery(ac);
			} catch (Exception e) {
				rc.fail(e);
			}
		});
	}
}
