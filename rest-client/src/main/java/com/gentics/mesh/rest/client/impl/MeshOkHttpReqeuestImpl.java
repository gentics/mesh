package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.reactivex.Maybe;
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
import java.util.Map;
import java.util.Optional;

public class MeshOkHttpReqeuestImpl<T> implements MeshRequest<T> {
	private final OkHttpClient client;
	private final Request request;
	private final Class<? extends T> resultClass;

	private MeshOkHttpReqeuestImpl(OkHttpClient client, Request request, Class<? extends T> resultClass) {
		this.client = client;
		this.request = request;
		this.resultClass = resultClass;
	}

	public static <T> MeshOkHttpReqeuestImpl<T> BinaryRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT, byte[] bodyData, String contentType) {
		Request request = createBaseBuilder(url, headers, method, RequestBody.create(MediaType.get(contentType), bodyData)).build();
		return new MeshOkHttpReqeuestImpl<>(client, request, classOfT);
	}

	public static <T> MeshOkHttpReqeuestImpl<T> JsonRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT, String json) {
		Request request = createBaseBuilder(url, headers, method, RequestBody.create(MediaType.get("application/json"), json)).build();
		return new MeshOkHttpReqeuestImpl<>(client, request, classOfT);
	}

	public static <T> MeshOkHttpReqeuestImpl<T> EmptyRequest(OkHttpClient client, String method, String url, Map<String, String> headers, Class<? extends T> classOfT) {
		Request request = createBaseBuilder(url, headers, method, RequestBody.create(null, "")).build();
		return new MeshOkHttpReqeuestImpl<>(client, request, classOfT);
	}

	private static Request.Builder createBaseBuilder(String url, Map<String, String> headers, String method, RequestBody body) {
		Request.Builder builder = new Request.Builder()
			.url(url)
			.headers(Headers.of(headers));

		if (!method.equalsIgnoreCase("get")) {
			builder = builder.method(method, body);
		}

		return builder;
	}

	@Override
	public MeshResponse<T> invoke() {
		throw new RuntimeException("Cannot invoke with OkHttp client");
	}

	@Override
	public HttpClientRequest getRequest() {
		throw new RuntimeException("Cannot modify request with OkHttp client");
	}

	@Override
	public Maybe<T> toMaybe() {
		return Maybe.create(sub -> {
			Call call = client.newCall(request);
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
					try {
						if (!sub.isDisposed()) {
							Optional<T> result = mapResponse(response);
							result.ifPresent(sub::onSuccess);
							if (!result.isPresent()) {
								sub.onComplete();
							}
						}
					} catch (Throwable e) {
						sub.onError(e);
					} finally {
						response.close();
					}
				}
			});

		});
	}

	private Optional<T> mapResponse(Response response) throws IOException, MeshRestClientMessageException {
		String body = response.body().string();
		if (!response.isSuccessful()) {
			throw new MeshRestClientMessageException(
				response.code(),
				response.message(),
				JsonUtil.readValue(body, GenericMessageResponse.class),
				HttpMethod.valueOf(request.method().toUpperCase()),
				request.url().toString()
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
		try (Response response = client.newCall(request).execute()) {
			return mapResponse(response).orElse(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void blockingAwait() {
		try {
			client.newCall(request).execute().close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
