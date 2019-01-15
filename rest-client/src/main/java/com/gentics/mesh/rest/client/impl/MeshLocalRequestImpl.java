package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.MeshResponse2;
import io.reactivex.Maybe;
import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;

import java.util.List;
import java.util.Map;

public class MeshLocalRequestImpl<T> implements MeshRequest<T> {

	private Future<T> future;

	public MeshLocalRequestImpl(Future<T> future) {
		this.future = future;
	}

	@Override
	public MeshResponse<T> invoke() {
		return new MeshResponse<>(future);
	}

	@Override
	public HttpClientRequest getRequest() {
		throw new NotImplementedException("The Http request object can't be used for local requests.");
	}

	@Override
	public Completable toCompletable() {
		return Completable.defer(() -> invoke().rxSetHandler()
				.toCompletable());
	}

	@Override
	public Single<T> toSingle() {
		return Single.defer(() -> invoke().rxSetHandler());
	}

	@Override
	public Observable<T> toObservable() {
		return Observable.defer(() -> invoke().rxSetHandler()
				.toObservable());
	}

	@Override
	public void setHeader(String name, String value) {
		throw new NotImplementedException("Can't set headers for local requests");
	}

	@Override
	public Single<MeshResponse2<T>> getResponse() {
		return toSingle().map(result -> new MeshResponse2<T>() {
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
