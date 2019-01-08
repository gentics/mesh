package com.gentics.mesh.rest.client.impl;

import io.reactivex.Maybe;
import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;

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
	public Maybe<T> toMaybe() {
		return toSingle().toMaybe();
	}
}
