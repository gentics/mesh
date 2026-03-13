package com.gentics.mesh.test.openapi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.openapitools.client.ApiResponse;
import org.openapitools.client.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.reactivex.Single;

class OpenAPIMeshRequestImpl<T, R> implements MeshRequest<R> {

	protected static final Logger log = LoggerFactory.getLogger(OpenAPIMeshRequestImpl.class);

	private final Callable<ApiResponse<T>> callable;
	private final Class<? extends R> targetType;

	public OpenAPIMeshRequestImpl(Callable<ApiResponse<T>> callable, Class<? extends R> targetType) {
		this.callable = callable;
		this.targetType = targetType;
	}

	protected Single<ApiResponse<T>> toApiSingle() {
		return Single.fromCallable(callable);
	}

	@Override
	public Single<R> toSingle() {
		return toApiSingle().map(apir -> ifNull(apir.getData(), r -> adaptResponse(r)));
	}

	@Override
	public void setHeader(String name, String value) {
		log.warn("Setting custom headers is not supported. Call of " + name + "=" + value + "is ignored");
	}

	@Override
	public Single<MeshResponse<R>> getResponse() {
		return toApiSingle().map(apir -> {
			return new MeshResponse<R>() {

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
				public R getBody() {
					return adaptResponse(apir.getData());
				}

				@Override
				public void close() {
				}
			};
		});
	}

	@SuppressWarnings("unchecked")
	protected R ifNull(T t, Function<T, R> ifNotNull) {
		if (t == null) {
			if (targetType.equals(EmptyResponse.class)) {
				return (R) EmptyResponse.getInstance();
			} else {
				return null;
			}
		} else {
			return ifNotNull.apply(t);
		}
	}

	protected R adaptResponse(T t) {
		return ifNull(t, r -> JsonUtil.readValue(JSON.getGson().toJson(t), targetType));
	}
}
