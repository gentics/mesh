package com.gentics.mesh.rest;

import com.gentics.mesh.rest.client.MeshResponse;
import io.reactivex.Maybe;
import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.rest.client.MeshRequest;

import io.reactivex.Single;
import io.vertx.core.Future;

import java.util.List;
import java.util.Map;

public class MeshLocalRequestImpl<T> implements MeshRequest<T> {

	private Future<T> future;

	public MeshLocalRequestImpl(Future<T> future) {
		this.future = future;
	}

	@Override
	public Single<T> toSingle() {
		return new io.vertx.reactivex.core.Future<T>(future).rxSetHandler();
	}

	@Override
	public void setHeader(String name, String value) {
		throw new NotImplementedException("Can't set headers for local requests");
	}

	@Override
	public Single<MeshResponse<T>> getResponse() {
		return toSingle().map(result -> new MeshResponse<T>() {
			@Override
			public Map<String, List<String>> getHeaders() {
				throw new RuntimeException("There are no headers in local requests");
			}

			@Override
			public int getStatusCode() {
				throw new RuntimeException("There is no status code in local requests");
			}

			@Override
			public String getBodyAsString() {
				throw new RuntimeException("There is no string body in local requests");
			}

			@Override
			public T getBody() {
				return result;
			}

			@Override
			public List<String> getCookies() {
				throw new RuntimeException("There are no cookies in local requests");
			}
		});
	}

	@Override
	public Maybe<T> toMaybe() {
		return toSingle().toMaybe();
	}
}
