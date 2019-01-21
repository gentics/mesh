package com.gentics.mesh.rest.client;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.http.HttpClientRequest;

import java.util.Map;

public interface MeshRequest<T> {

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
	default Maybe<T> toMaybe() {
		return toSingle().toMaybe();
	}

	/**
	 * Converts the request to a single. When subscribed, the request is invoked. When the response is received, onSuccess or onError is called.
	 *
	 * @return An RxJava single
	 */
	Single<T> toSingle();

	/**
	 * Converts the request to an observable. When subscribed, the request is invoked. When the response is received, onNext or onError is called.
	 *
	 * @return An RxJava observable
	 */
	default Observable<T> toObservable() {
		return toMaybe().toObservable();
	}

	/**
	 * Invokes the request and blocks the current thread until the result is received.
	 * @return The result from the response or null if the response is empty
	 */
	default T blockingGet() {
		return toSingle().blockingGet();
	}

	/**
	 * Invokes the request and block the current thread until a response is received.
	 */
	default void blockingAwait() {
		blockingGet();
	}

	/**
	 * Set a header for this request. Overwrites existing header with the same name.
	 */
	void setHeader(String name, String value);

	/**
	 * Set headers for this request. Overwrites existing headers with the same name.
	 */
	default void setHeaders(Map<String, String> headers) {
		headers.forEach(this::setHeader);
	}

	/**
	 * Gets the response with additional information.
	 */
	Single<MeshResponse<T>> getResponse();
}
