package com.gentics.mesh.rest.client.impl;

import io.reactivex.Maybe;
import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.rest.MeshRestClientAuthenticationProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.handler.ResponseHandler;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Wrapper for a mesh HTTP request.
 *
 * @param <T>
 */
public class MeshHttpRequestImpl<T> implements MeshRequest<T> {

	private static final Logger log = LoggerFactory.getLogger(MeshHttpRequestImpl.class);

	private HttpClientRequest request;

	private ResponseHandler<T> handler;

	private Buffer bodyData;

	private String contentType;
	
	private String accepts;

	private MeshRestClientAuthenticationProvider authentication;

	public MeshHttpRequestImpl(HttpClientRequest request, ResponseHandler<T> handler, Buffer bodyData, String contentType,
			MeshRestClientAuthenticationProvider authentication, String accepts) {
		this.request = request;
		this.handler = handler;
		this.bodyData = bodyData;
		this.contentType = contentType;
		this.authentication = authentication;
		this.accepts = accepts;
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
				request.headers().set("Accept", accepts);

				if (bodyData != null && bodyData.length() != 0) {
					request.headers().set("content-length", String.valueOf(bodyData.length()));
					if (!StringUtils.isEmpty(contentType)) {
						request.headers().set("content-type", contentType);
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
			request.headers().set("Accept", "application/json");

			if (bodyData != null && bodyData.length() != 0) {
				request.headers().set("content-length", String.valueOf(bodyData.length()));
				if (!StringUtils.isEmpty(contentType)) {
					request.headers().set("content-type", contentType);
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
	public Single<T> toSingle() {
		return Single.defer(() -> invoke().rxSetHandler());
	}

	@Override
	public Maybe<T> toMaybe() {
		return toSingle().toMaybe();
	}
}
