package com.gentics.mesh.mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Stream;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.web.Route;
import org.apache.commons.lang3.tuple.Pair;
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

		MultiMap params = createParameterMap(query);
		Route routeMock = mock(Route.class);

		when(routeMock.getPath()).thenReturn(uri);
		when(rc.currentRoute()).thenReturn(routeMock);
		when(request.absoluteURI()).thenReturn(uri);
		when(request.params()).thenReturn(params);
		when(request.getParam(anyString())).thenAnswer(in -> params.get(in.getArgumentAt(0, String.class)));

		when(rc.data()).thenReturn(map);
		when(rc.put(anyString(), any())).thenAnswer(in -> {
			map.put(in.getArgumentAt(0, String.class), in.getArgumentAt(1, Object.class));
			return rc;
		});
		when(rc.get(anyString())).thenAnswer(in -> map.get(in.getArgumentAt(0, String.class)));

		MultiMap headerMap = new CaseInsensitiveHeaders();

		headerMap.add("Accept-Language", "en, en-gb;q=0.8, en;q=0.72");
		when(request.headers()).thenReturn(headerMap);
		when(rc.request()).thenReturn(request);
		when(rc.session()).thenReturn(session);

		// Response
		HttpServerResponse response = mock(HttpServerResponse.class);
		when(response.setStatusCode(anyInt())).thenReturn(response);
		when(response.putHeader(anyObject(), (CharSequence) anyObject())).thenReturn(response);
		when(response.setStatusCode(anyInt())).thenReturn(response);
		when(response.setStatusMessage(anyString())).thenReturn(response);
		when(rc.response()).thenReturn(response);

		return rc;
	}

	/**
	 * Create a parameter {@link MultiMap} from the specified query string.
	 *
	 * @param query The query part of an URI
	 * @return A parameter map containing the elements in the specified query string
	 */
	private static MultiMap createParameterMap(String query) {
		Collector<Entry<String, String>, MultiMap, MultiMap> multiMapCollector = Collector.of(
			MultiMap::caseInsensitiveMultiMap,
			(map, item) -> map.add(item.getKey(), item.getValue()),
			(u, v) -> u.addAll(v));

		return Stream.of(query.split("&"))
			.map(p -> p.split("="))
			.map(p -> Pair.of(p[0], p.length > 1 ? p[1] : null))
			.collect(multiMapCollector);
	}
}
