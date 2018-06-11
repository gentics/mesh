package com.gentics.mesh.mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mockito.Mockito;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class MockUtil {

	public static MeshRestClient mockClient() {
		return Mockito.mock(MeshRestClient.class);
	}

	public static PluginContext mockPluginContext(String uri, String query) {
		return mockContext(PluginContext.class, uri, query);
	}

	public static RoutingContext mockRoutingContext(String uri, String query) {
		return mockContext(RoutingContext.class, uri, query);
	}

	public static <T extends RoutingContext> T mockContext(Class<T> clazz, String uri, String query) {
		Map<String, Object> map = new HashMap<>();
		T rc = mock(clazz);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);
		when(request.query()).thenReturn(query);
		Map<String, String> paramMap = new HashMap<>();
		MultiMap paramMultiMap = MultiMap.caseInsensitiveMultiMap();
		for (Entry<String, String> entry : paramMap.entrySet()) {
			paramMultiMap.add(entry.getKey(), entry.getValue());
		}
		when(request.absoluteURI()).thenReturn(uri);
		when(request.params()).thenReturn(paramMultiMap);
		when(request.getParam(Mockito.anyString())).thenAnswer(in -> {
			String key = (String) in.getArguments()[0];
			return paramMap.get(key);
		});
		paramMap.entrySet().stream().forEach(entry -> when(request.getParam(entry.getKey())).thenReturn(entry.getValue()));

		when(rc.data()).thenReturn(map);
		MultiMap headerMap = mock(MultiMap.class);
		when(headerMap.get("Accept-Language")).thenReturn("en, en-gb;q=0.8, en;q=0.72");
		when(request.headers()).thenReturn(headerMap);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);

		// Response
		HttpServerResponse response = mock(HttpServerResponse.class);
		when(response.setStatusCode(Mockito.anyInt())).thenReturn(response);
		when(response.putHeader((CharSequence) Mockito.anyObject(), (CharSequence) Mockito.anyObject())).thenReturn(response);
		when(rc.response()).thenReturn(response);
		return rc;
	}
}
