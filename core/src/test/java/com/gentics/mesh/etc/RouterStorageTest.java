package com.gentics.mesh.etc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.test.SpringTestConfiguration;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouteImpl;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
public class RouterStorageTest {

	@Autowired
	RouterStorage storage;

	@Test
	public void testFailureHandler() throws Exception {
		RoutingContext rc = mock(RoutingContext.class);
		Route currentRoute = mock(RouteImpl.class);
		when(currentRoute.getPath()).thenReturn("/blub");
		when(rc.currentRoute()).thenReturn(currentRoute);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn("?blub");
		when(request.method()).thenReturn(HttpMethod.GET);
		when(rc.request()).thenReturn(request);
		when(rc.normalisedPath()).thenReturn("/blub");
		storage.getRootRouter().handleFailure(rc);
	}
}
