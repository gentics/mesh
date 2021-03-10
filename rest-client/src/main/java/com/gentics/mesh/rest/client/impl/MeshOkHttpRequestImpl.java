package com.gentics.mesh.rest.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;

import io.reactivex.Completable;
import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public class MeshOkHttpRequestImpl<T> implements MeshRequest<T> {
	private final OkHttpClient client;
	private final Class<? extends T> resultClass;

	private final String method;
	private final String url;
	private final Map<String, String> headers;
	private final RequestBody requestBody;

	private MeshOkHttpRequestImpl(OkHttpClient client, Class<? extends T> resultClass, String method, String url, Map<String, String> headers,
		RequestBody requestBody) {
		this.client = client;
		this.resultClass = resultClass;
		this.method = method;
		this.url = url;
		this.headers = headers;
		this.requestBody = requestBody;
	}

	public static <T> MeshOkHttpRequestImpl<T> BinaryRequest(OkHttpClient client, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT, InputStream data, long fileSize, String contentType) {
		return new MeshOkHttpRequestImpl<>(client, classOfT, method, url, headers, new RequestBody() {
			@Override
			public MediaType contentType() {
				return MediaType.get(contentType);
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				try {
					sink.writeAll(Okio.source(data));
				} finally {
					data.close();
				}
			}
		});
	}

	public static <T> MeshOkHttpRequestImpl<T> JsonRequest(OkHttpClient client, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT, String json) {
		return new MeshOkHttpRequestImpl<>(client, classOfT, method, url, headers, RequestBody.create(MediaType.get("application/json"), json));
	}

	public static <T> MeshOkHttpRequestImpl<T> TextRequest(OkHttpClient client, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT, String text) {
		return new MeshOkHttpRequestImpl<>(client, classOfT, method, url, headers, RequestBody.create(MediaType.get("text/plain"), text));
	}

	public static <T> MeshOkHttpRequestImpl<T> EmptyRequest(OkHttpClient client, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT) {
		return new MeshOkHttpRequestImpl<>(client, classOfT, method, url, headers, RequestBody.create(null, ""));
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

	@Override
	public Completable toCompletable() {
		return getOkResponse()
			.doOnSuccess(this::throwOnError)
			.doOnSuccess(response -> Optional.ofNullable(response)
				.ifPresent(Response::close))
			.ignoreElement();
	}

	private T mapResponse(Response response) throws IOException, MeshRestClientMessageException {
		throwOnError(response);

		String contentType = response.header("Content-Type");
		if (!response.isSuccessful()) {
			return null;
		} else if  (resultClass.isAssignableFrom(EmptyResponse.class)) {
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

	private void throwOnError(Response response) throws IOException, MeshRestClientMessageException {
		if (!response.isSuccessful() && response.code() != 304) {
			String body = response.body().string();
			MeshRestClientMessageException err;
			try {
				GenericMessageResponse msg = null;
				if (!StringUtils.isEmpty(body)) {
					msg = JsonUtil.readValue(body, GenericMessageResponse.class);
				}
				err = new MeshRestClientMessageException(
					response.code(),
					response.message(),
					msg,
					HttpMethod.valueOf(method.toUpperCase()),
					stripOrigin(url));
			} catch (GenericRestException e) {
				err = new MeshRestClientMessageException(
					response.code(),
					response.message(),
					body,
					HttpMethod.valueOf(method.toUpperCase()),
					stripOrigin(url));
			}
			response.close();
			throw err;
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
			Response response = client.newCall(createRequest()).execute();
			throwOnError(response);
			Optional.ofNullable(response.body())
				.ifPresent(ResponseBody::close);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Single<MeshResponse<T>> getResponse() {
		return getOkResponse().map(response -> new MeshResponse<T>() {
			Supplier<T> body = Util.lazily(() -> mapResponse(response));

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
				return body.get();
			}
		});
	}
}
