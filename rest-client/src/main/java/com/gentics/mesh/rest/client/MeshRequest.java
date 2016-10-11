package com.gentics.mesh.rest.client;

import io.vertx.core.http.HttpClientRequest;
import rx.Completable;
import rx.Observable;
import rx.Single;

public interface MeshRequest<T> {

	/**
	 * Invoke the request and return the async response.
	 * 
	 * @return
	 */
	MeshResponse<T> invoke();

	/**
	 * Return the raw request. The request can be altered before {@link #invoke()} is called
	 * or {@link #toSingle()} is subscribed.
	 * 
	 * @return
	 */
	HttpClientRequest getRequest();

	/**
	 * Converts the request to a completable.
	 * When subscribed, the request is invoked.
	 * When the response is received, onComplete or onError is called.
	 * @return An RxJava Completable
	 */
	Completable toCompletable();

	/**
	 * Converts the request to a single.
	 * When subscribed, the request is invoked.
	 * When the response is received, onSuccess or onError is called.
	 *
	 * @return An RxJava single
	 */
	Single<T> toSingle();

	/**
	 * Converts the request to an observable.
	 * When subscribed, the request is invoked.
	 * When the response is received, onNext or onError is called.
	 *
	 * @return An RxJava observable
	 */
	Observable<T> toObservable();
}
