package com.gentics.mesh.rest.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.reactivestreams.Publisher;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
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

/**
 * OkHttp implementation of the {@link MeshRequest}.
 * 
 * @see MeshRequest
 * @param <T>
 */
public class MeshOkHttpRequestImpl<T> implements MeshRequest<T> {

	private final OkHttpClient client;
	private final MeshRestClientConfig config;
	private final Class<? extends T> resultClass;

	private final String method;
	private final String url;
	private final Map<String, String> headers;
	private final RequestBody requestBody;

	private MeshOkHttpRequestImpl(OkHttpClient client, MeshRestClientConfig config, Class<? extends T> resultClass, String method, String url, Map<String, String> headers,
		RequestBody requestBody) {
		this.client = client;
		this.config = config;
		this.resultClass = resultClass;
		this.method = method;
		this.url = url;
		this.headers = headers;
		this.requestBody = requestBody;
	}

	/**
	 * Create a new binary request.
	 * 
	 * @param <T>
	 *            Response type
	 * @param client
	 *            Client to be used
	 * @param config
	 *            Configuration
	 * @param method
	 *            Request method
	 * @param url
	 *            Request url
	 * @param headers
	 *            Additional headers
	 * @param classOfT
	 *            Response model class
	 * @param data
	 *            stream for the binary data
	 * @param fileSize
	 *            expected length of the data
	 * @param contentType
	 *            Posted content type
	 * @return
	 */
	public static <T> MeshOkHttpRequestImpl<T> BinaryRequest(OkHttpClient client, MeshRestClientConfig config, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT, InputStream data, long fileSize, String contentType) {
		return new MeshOkHttpRequestImpl<>(client, config, classOfT, method, url, headers, new RequestBody() {
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

	/**
	 * Create a {@link MeshOkHttpRequestImpl} using the request parameters that sends a JSON payload.
	 * 
	 * @param <T>
	 * @param client
	 * @param config
	 * @param method
	 * @param url
	 * @param headers
	 * @param classOfT
	 * @param json
	 * @return
	 */
	public static <T> MeshOkHttpRequestImpl<T> JsonRequest(OkHttpClient client, MeshRestClientConfig config, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT, String json) {
		return new MeshOkHttpRequestImpl<>(client, config, classOfT, method, url, headers, RequestBody.create(MediaType.get("application/json"), json));
	}

	/**
	 * Create a {@link MeshOkHttpRequestImpl} using the request parameters that sends a plain text payload.
	 * 
	 * @param <T>
	 * @param client
	 * @param config
	 * @param method
	 * @param url
	 * @param headers
	 * @param classOfT
	 * @param text
	 * @return
	 */
	public static <T> MeshOkHttpRequestImpl<T> TextRequest(OkHttpClient client, MeshRestClientConfig config, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT, String text) {
		return new MeshOkHttpRequestImpl<>(client, config, classOfT, method, url, headers, RequestBody.create(MediaType.get("text/plain"), text));
	}

	/**
	 * Create a {@link MeshOkHttpRequestImpl} request which does not send a body payload.
	 * 
	 * @param <T>
	 * @param client
	 * @param config
	 * @param method
	 * @param url
	 * @param headers
	 * @param classOfT
	 * @return
	 */
	public static <T> MeshOkHttpRequestImpl<T> EmptyRequest(OkHttpClient client, MeshRestClientConfig config, String method, String url, Map<String, String> headers,
		Class<? extends T> classOfT) {
		return new MeshOkHttpRequestImpl<>(client, config, classOfT, method, url, headers, RequestBody.create(null, ""));
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
		Single<Response> response =  Single.create(sub -> {
			Call call = client.newCall(createRequest());
			call.enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					if (!sub.isDisposed()) {
						sub.onError(new IOException(String.format("I/O Error in %s %s : %s (%s)",
								HttpMethod.valueOf(method.toUpperCase()), url, e.getClass().getSimpleName(), e.getLocalizedMessage()), e));
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

		if (config == null) {
			return response;
		}

		int retries = config.getMaxRetries();
		int delay = config.getRetryDelayMs();

		if (retries <= 0) {
			return response;
		}

		// Setting the delay < 0 means auto-delay, which is set so that all
		// retries happen within the configured overall call timeout. If no
		// delay is desired, set it to 0. Then retryOnNetworkErrors() will
		// ignore it.
		if (delay < 0) {
			delay = client.callTimeoutMillis() / retries;
		}

		return response.retryWhen(retryOnNetworkErrors(retries, delay));
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
		} else if (resultClass.isAssignableFrom(EmptyResponse.class)) {
			return (T) EmptyResponse.getInstance();
		} else if (resultClass.isAssignableFrom(MeshBinaryResponse.class)) {
			return (T) new OkHttpBinaryResponse(response);
		} else if (resultClass.isAssignableFrom(MeshWebrootResponse.class)) {
			return (T) new OkHttpWebrootResponse(response);
		} else if (resultClass.isAssignableFrom(MeshWebrootFieldResponse.class)) {
			return (T) new OkHttpWebrootFieldResponse(response);
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

	/**
	 * Notification handler for {@link Observable#retryWhen(io.reactivex.functions.Function) retryWhen()} which causes
	 * retries for network related exceptions up to {@code maxRetries}, with an optional {@code delay} in milliseconds.
	 *
	 * <p>
	 *     <strong>TODO:</strong> This should probably be moved to a generic utility class, but I did not want to
	 *     introduce a dependency to mesh-common for the rest client.
	 * </p>
	 *
	 * @see #isNotNetworkError(Throwable)
	 *
	 * @param maxRetries The maximum number of retries
	 * @param delay The delay in milliseconds for each retry, ignored when &lt; 0.
	 * @return A notification handler for {@code retryWhen()} which makes at most {@code maxRetries} retries on
	 * 		network problems
	 */
	private io.reactivex.functions.Function<Flowable<Throwable>, Publisher<Object>> retryOnNetworkErrors(int maxRetries, int delay) {
		AtomicInteger retryCount = new AtomicInteger(0);

		return errors -> errors.flatMap(err -> {
			if (isNotNetworkError(err) || retryCount.getAndIncrement() > maxRetries) {
				// Not a network problem or max retries reached, forward the error to stop retrying.
				return Flowable.error(err);
			}

			Flowable<Object> ret = Flowable.just(new Object());

			if (delay > 0) {
				ret = ret.delay(delay, TimeUnit.MILLISECONDS);
			}

			return ret;
		});
	}

	/**
	 * Check if the given throwable or its cause is a {@link SocketException},
	 * or a {@link SocketTimeoutException}.
	 *
	 * @param t The error to check
	 * @return {@code true} if the given error is a {@code SocketException} or
	 * 		a {@code SocketTimeoutException}, and {@code false} otherwise.
	 */
	private boolean isNotNetworkError(Throwable t) {
		if (t == null) {
			return true;
		}

		if (t instanceof SocketException || t instanceof SocketTimeoutException) {
			return false;
		}

		return isNotNetworkError(t.getCause());
	}
}
