package com.gentics.mesh.rest.client;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.http.HttpClientRequest;

public interface MeshRequest<T> {

	/**
	 * Invoke the request and return the async response.
	 *
	 * @deprecated Dependent on Vert.x - will be removed in a later version
	 * @return
	 */
	@Deprecated
	MeshResponse<T> invoke();

	/**
	 * Return the raw request. The request can be altered before {@link #invoke()} is called or {@link #toSingle()} is subscribed.
	 *
	 * @deprecated Dependent on Vert.x - will be removed in a later version
	 * @return
	 */
	@Deprecated
	HttpClientRequest getRequest();

	/**
	 * Converts the request to a completable. When subscribed, the request is invoked. When the response is received, onComplete or onError is called.
	 * 
	 * @return An RxJava Completable
	 */
	default Completable toCompletable() {
		return toMaybe().ignoreElement();
	}

	/**
	 * Converts the request to a single. When subscribed, the request is invoked. When the response is received, onSuccess or onError is called.
	 *
	 * @return An RxJava single
	 */
	Maybe<T> toMaybe();

	/**
	 * Converts the request to a single. When subscribed, the request is invoked. When the response is received, onSuccess or onError is called.
	 *
	 * @return An RxJava single
	 */
	default Single<T> toSingle() {
		return toMaybe().toSingle();
	}

	/**
	 * Converts the request to an observable. When subscribed, the request is invoked. When the response is received, onNext or onError is called.
	 *
	 * @return An RxJava observable
	 */
	default Observable<T> toObservable() {
		return toMaybe().toObservable();
	}
}
