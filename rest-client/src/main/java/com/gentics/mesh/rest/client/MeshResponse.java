package com.gentics.mesh.rest.client;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.reactivex.core.Future;

/**
 * @deprecated Used in Vert.x client. Use OkHttp client instead.
 */
@Deprecated
public class MeshResponse<T> extends Future<T> {

	private HttpClientResponse rawResponse;

	private String bodyJson;

	public MeshResponse(io.vertx.core.Future<T> delegate) {
		super(delegate);
	}

	public static <T> MeshResponse<T> create() {
		return new MeshResponse<T>(io.vertx.core.Future.future());
	}

	/**
	 * Get the raw response.
	 * 
	 * @return
	 */
	public HttpClientResponse getRawResponse() {
		return rawResponse;
	}

	/**
	 * Set the raw response.
	 * 
	 * @param response
	 */
	public void setRawResponse(HttpClientResponse response) {
		this.rawResponse = response;
	}

	/**
	 * Set the body JSON string.
	 * 
	 * @param json
	 *            JSON String
	 * @return
	 */
	public MeshResponse<T> setBodyJson(String json) {
		this.bodyJson = json;
		return this;
	}

	/**
	 * Return the body JSON string.
	 * 
	 * @return JSON String or null if no body has been set
	 */
	public String getBodyJson() {
		return bodyJson;
	}

}
