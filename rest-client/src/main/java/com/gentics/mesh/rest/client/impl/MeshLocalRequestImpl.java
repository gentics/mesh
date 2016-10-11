package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientRequest;
import org.apache.commons.lang.NotImplementedException;
import rx.Completable;
import rx.Observable;
import rx.Single;

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
		return toObservable().toCompletable();
	}

	@Override
	public Single<T> toSingle() {
		return toObservable().toSingle();
	}


	@Override
	public Observable<T> toObservable() {
		return Observable.defer(() -> invoke().setHandlerObservable());
	}
}
