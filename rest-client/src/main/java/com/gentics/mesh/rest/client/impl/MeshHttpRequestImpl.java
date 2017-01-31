package com.gentics.mesh.rest.client.impl;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.rest.MeshRestClientAuthenticationProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.handler.MeshResponseHandler;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Wrapper for a mesh http request.
 *
 * @param <T>
 */
public class MeshHttpRequestImpl<T> implements MeshRequest<T> {

	private static final Logger log = LoggerFactory.getLogger(MeshHttpRequestImpl.class);

	private HttpClientRequest request;

	private MeshResponseHandler<T> handler;

	private Buffer bodyData;

	private String contentType;

	private MeshRestClientAuthenticationProvider authentication;

	public MeshHttpRequestImpl(HttpClientRequest request, MeshResponseHandler<T> handler, Buffer bodyData, String contentType,
			MeshRestClientAuthenticationProvider authentication) {
		this.request = request;
		this.handler = handler;
		this.bodyData = bodyData;
		this.contentType = contentType;
		this.authentication = authentication;
	}

	@Override
	public HttpClientRequest getRequest() {
		return request;
	}

	@Override
	public MeshResponse<T> invoke() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking request to {" + handler.getUri() + "}");
		}

		if (authentication != null) {
			authentication.addAuthenticationInformation(request).subscribe(() -> {
				request.headers().add("Accept", "application/json");

				if (bodyData != null && bodyData.length() != 0) {
					request.headers().add("content-length", String.valueOf(bodyData.length()));
					if (!StringUtils.isEmpty(contentType)) {
						request.headers().add("content-type", contentType);
					}
					// Somehow the buffer gets mix up after some requests. It seems that the buffer object is somehow reused and does not return the correct data. toString seems to alleviate the problem.
					if (contentType != null && contentType.startsWith("application/json")) {
						request.write(bodyData.toString());
					} else {
						request.write(bodyData);
					}
				}

			});
		} else {
			request.headers().add("Accept", "application/json");

			if (bodyData != null && bodyData.length() != 0) {
				request.headers().add("content-length", String.valueOf(bodyData.length()));
				if (!StringUtils.isEmpty(contentType)) {
					request.headers().add("content-type", contentType);
				}
				// Somehow the buffer gets mix up after some requests. It seems that the buffer object is somehow reused and does not return the correct data. toString seems to alleviate the problem.
				if (contentType != null && contentType.startsWith("application/json")) {
					request.write(bodyData.toString());
				} else {
					request.write(bodyData);
				}
			}
		}

		request.end();
		return handler.getFuture();
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
