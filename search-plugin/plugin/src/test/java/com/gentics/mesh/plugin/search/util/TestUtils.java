package com.gentics.mesh.plugin.search.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.vertx.http.HttpResponseStatus;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Session;

public class TestUtils {

	public static PluginContext mockContext(String project, String uri, String query) {
		Map<String, Object> map = new HashMap<>();
		PluginContext pc = mock(PluginContext.class);
		Session session = mock(Session.class);
		HttpServerRequest request = mock(HttpServerRequest.class);

		when(pc.projectName()).thenReturn(project);

		when(request.query()).thenReturn(query);

		MultiMap params = createParameterMap(query);
		Route routeMock = mock(Route.class);

		when(routeMock.getPath()).thenReturn(uri);
		when(pc.currentRoute()).thenReturn(routeMock);
		when(request.absoluteURI()).thenReturn(uri);
		when(pc.queryParams()).thenReturn(params);
		when(request.params()).thenReturn(params);
		when(request.getParam(anyString())).thenAnswer(in -> params.get(in.getArgumentAt(0, String.class)));

		when(pc.data()).thenReturn(map);
		when(pc.put(anyString(), any())).thenAnswer(in -> {
			map.put(in.getArgumentAt(0, String.class), in.getArgumentAt(1, Object.class));
			return pc;
		});
		when(pc.get(anyString())).thenAnswer(in -> map.get(in.getArgumentAt(0, String.class)));

		MultiMap headerMap = MultiMap.caseInsensitiveMultiMap();

		headerMap.add("Accept-Language", "en, en-gb;q=0.8, en;q=0.72");
		when(request.headers()).thenReturn(headerMap);
		when(pc.request()).thenReturn(request);
		when(pc.session()).thenReturn(session);

		// Response
		HttpServerResponse response = mock(HttpServerResponse.class);
		when(response.setStatusCode(anyInt())).thenReturn(response);
		when(response.putHeader(anyObject(), (CharSequence) anyObject())).thenReturn(response);
		when(response.setStatusCode(anyInt())).thenReturn(response);
		when(response.setStatusMessage(anyString())).thenReturn(response);
		when(pc.response()).thenReturn(response);

		MeshRestClient client = mock(MeshRestClient.class);

		when(pc.client()).thenReturn(client);

		return pc;
	}

	/**
	 * Mock a request that will return the given response.
	 * @param response The mocked response
	 * @return mocked request
	 */
	public static <T> MeshRequest<T> mockRequest(T response) {
		return mockRequest(response, HttpResponseStatus.OK.code());
	}

	/**
	 * Mock a request that will return the given response and status
	 * @param response response
	 * @param status The response status code
	 * @return mocked request
	 */
	public static <T> MeshRequest<T> mockRequest(T response, int status) {
		@SuppressWarnings("unchecked")
		MeshRequest<T> requestMock = mock(MeshRequest.class);
		Map<String, List<String>> requestHeaders = new HashMap<>();

		when(requestMock.toObservable()).thenReturn(Observable.just(response));
		when(requestMock.toSingle()).thenReturn(Single.just(response));
		when(requestMock.toCompletable()).thenReturn(Completable.fromSingle(Single.just(response)));

		@SuppressWarnings("unchecked")
		MeshResponse<T> meshResponse = mock(MeshResponse.class);

		when(meshResponse.getStatusCode()).thenReturn(status);
		when(meshResponse.getHeaders()).thenReturn(Collections.emptyMap());
		when(meshResponse.getHeader(anyString())).thenReturn(Optional.empty());
		when(meshResponse.getBody()).thenReturn(response);
		when(requestMock.getResponse()).thenReturn(Single.just(meshResponse));

		return requestMock;
	}

	/**
	 * Create a parameter {@Link MultiMap} from the specified query string.
	 *
	 * @param query The query part of an URI
	 * @return A parameter map containing the elements in the specified query string
	 */
	private static MultiMap createParameterMap(String query) {
		Collector<Map.Entry<String, String>, MultiMap, MultiMap> multiMapCollector = Collector.of(
			MultiMap::caseInsensitiveMultiMap,
			(map, item) -> map.add(item.getKey(), decodeUrlPart(item.getValue())),
			(u, v) -> u.addAll(v));

		return Stream.of(query.split("&"))
				   .map(p -> p.split("="))
				   .map(p -> Pair.of(p[0], p.length > 1 ? p[1] : ""))
				   .collect(multiMapCollector);
	}

	private static String decodeUrlPart(String part) {
		try {
			return URLDecoder.decode(part, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return null;
	}
}
