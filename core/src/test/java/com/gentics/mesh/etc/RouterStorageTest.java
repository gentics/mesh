package com.gentics.mesh.etc;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.router.RouterStorage;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouteImpl;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouterStorageTest {

	@Test
	public void testFailureHandler() throws Exception {
		MeshAuthChain chain = Mockito.mock(MeshAuthChain.class);
		RouterStorage storage = new RouterStorage(null, chain, null, null, null, () -> {
			return Mockito.mock(Database.class);
		}, mockVersionhandler());

		RoutingContext rc = mock(RoutingContext.class);
		Route currentRoute = mock(RouteImpl.class);
		when(currentRoute.getPath()).thenReturn("/blub");
		when(rc.currentRoute()).thenReturn(currentRoute);
		ParsedHeaderValues headerValues = mock(ParsedHeaderValues.class);
		when(rc.parsedHeaders()).thenReturn(headerValues);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn("?blub");
		when(request.method()).thenReturn(HttpMethod.GET);
		when(request.uri()).thenReturn("");
		when(rc.request()).thenReturn(request);
		when(rc.normalisedPath()).thenReturn("/blub");
		when(rc.queryParams()).thenReturn(new CaseInsensitiveHeaders());
		storage.root().getRouter().handleFailure(rc);
	}

	private VersionHandler mockVersionhandler() {
		VersionHandler versionHandler = mock(VersionHandler.class);
		when(VersionHandler.generateVersionMountpoints()).then(ignore -> Stream.of(
			"/api/v1",
			"/api/v2"
		));
		return versionHandler;
	}
}
