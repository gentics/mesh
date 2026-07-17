package com.gentics.mesh.rest.client.impl;

import java.io.IOException;
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
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;

import io.reactivex.Completable;
import io.reactivex.Single;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Default OkHTTP based client implementation
 * 
 * @param <R>
 */
public abstract class AbstractMeshOkHttpRequest<R> implements MeshRequest<R> {

	protected final OkHttpClient client;
	protected final Class<? extends R> resultClass;

	public AbstractMeshOkHttpRequest(OkHttpClient client, Class<? extends R> resultClass) {
		this.client = client;
		this.resultClass = resultClass;
	}

	@Override
	public Single<MeshResponse<R>> getResponse() {
		return getOkResponse().map(response -> new MeshResponse<R>() {
			Supplier<R> body = Util.lazily(() -> mapResponse(response));

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
			public R getBody() {
				return body.get();
			}

			@Override
			public void close() {
				Optional.ofNullable(response).map(Response::body).ifPresent(ResponseBody::close);
			}
		});
	}

	@Override
	public Completable toCompletable() {
		return getOkResponse()
			.doOnSuccess(this::throwOnError)
			.doOnSuccess(response -> Optional.ofNullable(response)
				.ifPresent(Response::close))
			.ignoreElement();
	}

	@Override
	public Single<R> toSingle() {
		return getOkResponse().map(this::mapResponse);
	}

	public Single<Response> getOkResponse() {
		return Single.create(sub -> {
			Call call = createCall();
			call.enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					// Don't call the onError twice, but notify about multiple exceptions (should not occur often).
					if (sub.isDisposed()) {
						e.printStackTrace();
					} else {
						sub.onError(new IOException(String.format("I/O Error in %s %s : %s (%s)",
								HttpMethod.valueOf(call.request().method().toUpperCase()), call.request().url(), e.getClass().getSimpleName(), e.getLocalizedMessage()), e));
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

	protected abstract Request createRequest();

	protected Call createCall() {
		return client.newCall(createRequest());
	}

	@SuppressWarnings("unchecked")
	private R mapResponse(Response response) throws IOException, MeshRestClientMessageException {
		throwOnError(response);

		String contentType = response.header("Content-Type");
		if (!response.isSuccessful()) {
			return null;
		} else if (resultClass.isAssignableFrom(EmptyResponse.class)) {
			return (R) EmptyResponse.getInstance();
		} else if (resultClass.isAssignableFrom(MeshBinaryResponse.class)) {
			return (R) new OkHttpBinaryResponse(response);
		} else if (resultClass.isAssignableFrom(MeshWebrootResponse.class)) {
			return (R) new OkHttpWebrootResponse(response);
		} else if (resultClass.isAssignableFrom(MeshWebrootFieldResponse.class)) {
			return (R) new OkHttpWebrootFieldResponse(response, false);
		} else if (contentType != null && contentType.startsWith("application/json") && !resultClass.isAssignableFrom(String.class)) {
			return JsonUtil.readValue(response.body().string(), resultClass);
		} else if (resultClass.isAssignableFrom(String.class)) {
			return (R) response.body().string();
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
					HttpMethod.valueOf(response.request().method().toUpperCase()),
					stripOrigin(response.request().url().toString()));
			} catch (GenericRestException e) {
				err = new MeshRestClientMessageException(
					response.code(),
					response.message(),
					body,
					HttpMethod.valueOf(response.request().method().toUpperCase()),
					stripOrigin(response.request().url().toString()));
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
}
