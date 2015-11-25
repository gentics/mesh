package com.gentics.mesh.handler.impl;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Test;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class InternalHttpActionContextImplTest {

	@Test
	public void testSplitQuery() throws Exception {
		RoutingContext rc = mock(RoutingContext.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn("bla=123&blub=123");
		when(rc.request()).thenReturn(request);
		when(rc.data()).thenReturn(new HashMap<>());
		InternalHttpActionContextImpl context = new InternalHttpActionContextImpl(rc);
		System.out.println(context.splitQuery());
		assertNotNull(context.splitQuery());
	}
}
