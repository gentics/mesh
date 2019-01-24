package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.HttpMethod;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class MeshOkHttpReqeuestImpl<T> implements MeshRequest<T> {
	private final OkHttpClient client;
	private final Class<? extends T> resultClass;

	private final String method;
	private final String url;
	private final Map<String, String> headers;
	private final RequestBody requestBody;

	private MeshOkHttpReqeuestImpl(OkHttpClient client, Class<? extends T> resultClass, String method, String url, Map<String, String> headers, RequestBody requestBody) {
		this.client = client;
		this.resultClass = resultClass;
		this.method = method;
		this.url = url;
		this.headers = headers;
		this.requestBody = requestBody;
	}

	public static <T> MeshOkHttpReqeuestImpl<T> BinaryRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT, InputStream data, long fileSize, String contentType) {
		return new MeshOkHttpReqeuestImpl<>(client, classOfT, method, url, headers, new RequestBody() {
			@Override
			public MediaType contentType() {
				return MediaType.get(contentType);
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				sink.writeAll(Okio.source(data));
			}
		});
	}

	public static <T> MeshOkHttpReqeuestImpl<T> JsonRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT, String json) {
		return new MeshOkHttpReqeuestImpl<>(client, classOfT, method, url, headers, RequestBody.create(MediaType.get("application/json"), json));
	}

	public static <T> MeshOkHttpReqeuestImpl<T> EmptyRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT) {
		return new MeshOkHttpReqeuestImpl<>(client, classOfT, method, url, headers, RequestBody.create(null, ""));
	}

	@Override
	public void setHeader(String name, String value) {
		headers.put(name, value);
	}

	@Override
	public void setHeaders(Map<String, String> headers) {
		this.headers.putAll(headers);
	}

	private Request createRequest() {
		Request.Builder builder = new Request.Builder()
			.url(url)
			.headers(Headers.of(headers));

		if (!method.equalsIgnoreCase("get")) {
			builder = builder.method(method, requestBody);
		}

		return builder.build();
	}

	private Single<Response> getOkResponse() {
		return Single.create(sub -> {
			Call call = client.newCall(createRequest());
			call.enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					if (!sub.isDisposed()) {
						sub.onError(e);
					}
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {
					if (!sub.isDisposed()) {
						sub.onSuccess(response);
					} else {
						response.close();
					}
				}
			});
		});
	}

	@Override
	public Single<T> toSingle() {
		return getOkResponse().map(this::mapResponse);
	}

	private T mapResponse(Response response) throws IOException, MeshRestClientMessageException {
		if (!response.isSuccessful()) {
			throw new MeshRestClientMessageException(
				response.code(),
				response.message(),
				JsonUtil.readValue(response.body().string(), GenericMessageResponse.class),
				HttpMethod.valueOf(method.toUpperCase()),
				stripOrigin(url)
			);
		} else {
			String contentType = response.header("Content-Type");
			if (resultClass.isAssignableFrom(EmptyResponse.class)) {
				return (T) EmptyResponse.getInstance();
			} else if (resultClass.isAssignableFrom(MeshBinaryResponse.class)) {
				return (T) new OkHttpBinaryResponse(response);
			} else if (resultClass.isAssignableFrom(MeshWebrootResponse.class)) {
				return (T) new OkHttpWebrootResponse(response);
			} else if (contentType != null && contentType.startsWith("application/json")) {
				return JsonUtil.readValue(response.body().string(), resultClass);
			} else if (resultClass.isAssignableFrom(String.class)) {
				return (T) response.body().string();
			} else {
				throw new RuntimeException("Request can't be handled by this handler since the content type was {" + contentType + "}");
			}
		}
	}

	private String stripOrigin(String url) {
		URI uri = URI.create(url);
		String query = uri.getQuery();
		return uri.getPath() + (query == null || query.length() == 0
			? ""
			: "?" + query);
	}

	@Override
	public T blockingGet() {
		try {
			Response response = client.newCall(createRequest()).execute();
			return mapResponse(response);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void blockingAwait() {
		try {
			client.newCall(createRequest()).execute().close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Single<MeshResponse<T>> getResponse() {
		return getOkResponse().map(response -> new MeshResponse<T>() {
			@Override
			public Map<String, List<String>> getHeaders() {
				return response.headers().toMultimap();
			}

			@Override
			public List<String> getHeaders(String name) {
				return response.headers(name);
			}

			@Override
			public int getStatusCode() {
				return response.code();
			}

			@Override
			public String getBodyAsString() {
				try {
					return response.body().string();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public T getBody() {
				String body = getBodyAsString();
				return body.length() == 0
					? null
					: JsonUtil.readValue(getBodyAsString(), resultClass);
			}
		});
	}
}
