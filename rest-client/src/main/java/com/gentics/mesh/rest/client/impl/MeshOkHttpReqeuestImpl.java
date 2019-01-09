package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshResponse2;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MeshOkHttpReqeuestImpl<T> implements MeshRequest<T> {
	private final OkHttpClient client;
	private final Class<? extends T> resultClass;

	private final String method;
	private final String url;
	private final Map<String, String> headers;
	private final RequestBody body;

	private MeshOkHttpReqeuestImpl(OkHttpClient client, Class<? extends T> resultClass, String method, String url, Map<String, String> headers, RequestBody body) {
		this.client = client;
		this.resultClass = resultClass;
		this.method = method;
		this.url = url;
		this.headers = headers;
		this.body = body;
	}

	public static <T> MeshOkHttpReqeuestImpl<T> BinaryRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT, byte[] bodyData, String contentType) {
		return new MeshOkHttpReqeuestImpl<>(client, classOfT, method, url, headers, RequestBody.create(MediaType.get(contentType), bodyData));
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
		headers.putAll(headers);
	}

	@Override
	public MeshResponse<T> invoke() {
		throw new RuntimeException("Cannot invoke with OkHttp client");
	}

	@Override
	public HttpClientRequest getRequest() {
		throw new RuntimeException("Cannot modify request with OkHttp client");
	}

	private Request createRequest() {
		Request.Builder builder = new Request.Builder()
			.url(url)
			.headers(Headers.of(headers));

		if (!method.equalsIgnoreCase("get")) {
			builder = builder.method(method, body);
		}

		return builder.build();
	}

	private Single<Response> getOkResponse() {
		return Single.create(sub -> {
			Call call = client.newCall(createRequest());
			sub.setDisposable(new Disposable() {
				@Override
				public void dispose() {
					call.cancel();
				}

				@Override
				public boolean isDisposed() {
					return call.isCanceled();
				}
			});
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
					}
				}
			});
		});
	}

	@Override
	public Maybe<T> toMaybe() {
		return getOkResponse().flatMapMaybe(response ->
			mapResponse(response)
				.map(Maybe::just)
				.orElse(Maybe.empty())
		);
	}

	private Optional<T> mapResponse(Response response) throws IOException, MeshRestClientMessageException {
		String body = response.body().string();
		if (!response.isSuccessful()) {
			throw new MeshRestClientMessageException(
				response.code(),
				response.message(),
				JsonUtil.readValue(body, GenericMessageResponse.class),
				HttpMethod.valueOf(method.toUpperCase()),
				url
			);
		} else {
			String contentType = response.header("Content-Type");
			if (body.length() == 0) {
				return Optional.empty();
			} else if (contentType != null && contentType.startsWith("application/json")) {
				return Optional.of(JsonUtil.readValue(body, resultClass));
			} else if (resultClass.isAssignableFrom(String.class)) {
				return Optional.of((T) body);
			} else {
				throw new RuntimeException("Request can't be handled by this handler since the content type was {" + contentType + "}");
			}
		}
	}

	@Override
	public T blockingGet() {
		try (Response response = client.newCall(createRequest()).execute()) {
			return mapResponse(response).orElse(null);
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
	public Single<MeshResponse2<T>> getResponse() {
		return getOkResponse().map(response -> new MeshResponse2<T>() {
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
				return JsonUtil.readValue(getBodyAsString(), resultClass);
			}
		});
	}
}
