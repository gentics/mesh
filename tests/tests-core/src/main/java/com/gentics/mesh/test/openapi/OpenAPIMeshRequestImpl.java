package com.gentics.mesh.test.openapi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.openapitools.client.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;

import io.reactivex.Single;

class OpenAPIMeshRequestImpl<T> implements MeshRequest<T> {

	protected static final Logger log = LoggerFactory.getLogger(OpenAPIMeshRequestImpl.class);

	private final Callable<ApiResponse<T>> callable;

	public OpenAPIMeshRequestImpl(Callable<ApiResponse<T>> callable) {
		this.callable = callable;
	}

	protected Single<ApiResponse<T>> toApiSingle() {
		return Single.fromCallable(callable);
	}

	@Override
	public Single<T> toSingle() {
		return toApiSingle().map(ApiResponse::getData);
	}

	@Override
	public void setHeader(String name, String value) {
		log.warn("Setting custom headers is not supported. Call of " + name + "=" + value + "is ignored");
	}

	@Override
	public Single<MeshResponse<T>> getResponse() {
		return toApiSingle().map(apir -> {
			return new MeshResponse<T>() {

				@Override
				public Map<String, List<String>> getHeaders() {
					return apir.getHeaders();
				}

				@Override
				public int getStatusCode() {
					return apir.getStatusCode();
				}

				@Override
				public String getBodyAsString() {
					return JsonUtil.toJson(apir.getData());
				}

				@Override
				public T getBody() {
					return apir.getData();
				}

				@Override
				public void close() {
				}
			};
		});
	}
}
