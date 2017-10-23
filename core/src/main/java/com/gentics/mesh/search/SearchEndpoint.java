package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.rest.EndpointRoute;

public interface SearchEndpoint extends Endpoint {

	/**
	 * Register the search handler which will directly return the search response without any post processing by mesh.
	 * 
	 * @param typeName
	 * @param nodeSearchHandler2
	 */
	default void registerRawSearchHandler(String typeName, SearchHandler<?, ?> searchHandler) {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return the raw response.");
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
