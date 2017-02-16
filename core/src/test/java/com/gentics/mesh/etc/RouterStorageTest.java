package com.gentics.mesh.etc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouteImpl;

public class RouterStorageTest {

	private RouterStorage storage = new RouterStorage(null, null);

	@Test
	public void testFailureHandler() throws Exception {
		RoutingContext rc = mock(RoutingContext.class);
		Route currentRoute = mock(RouteImpl.class);
		when(currentRoute.getPath()).thenReturn("/blub");
		when(rc.currentRoute()).thenReturn(currentRoute);
		ParsedHeaderValues headerValues = mock(ParsedHeaderValues.class);
		when(rc.parsedHeaders()).thenReturn(headerValues);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn("?blub");
		when(request.method()).thenReturn(HttpMethod.GET);
		when(rc.request()).thenReturn(request);
		when(rc.normalisedPath()).thenReturn("/blub");
		storage.getRootRouter().handleFailure(rc);
	}
}
