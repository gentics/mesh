package com.gentics.mesh.rest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractMeshRestClient implements MeshRestClient {

	protected static final Logger log = LoggerFactory.getLogger(AbstractMeshRestClient.class);

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	protected String username;
	protected String password;
	protected String authEnc;

	protected ClientSchemaStorage clientSchemaStorage = new ClientSchemaStorage();

	protected HttpClient client;

	private String cookie;

	@Override
	public MeshRestClient setLogin(String username, String password) {
		this.username = username;
		this.password = password;
		String authStringEnc = username + ":" + password;
		authEnc = new String(Base64.encodeBase64(authStringEnc.getBytes()));
		return this;
	}

	@Override
	public HttpClient getClient() {
		return client;
	}

	@Override
	public void close() {
		client.close();
	}

	public String getCookie() {
		return cookie;
	}

	public static String getBaseuri() {
		return BASEURI;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	public MeshRestClient setCookie(String cookie) {
		this.cookie = cookie;
		return this;
	}

	@Override
	public ClientSchemaStorage getClientSchemaStorage() {
		return clientSchemaStorage;
	}

	public MeshRestClient setClientSchemaStorage(ClientSchemaStorage clientSchemaStorage) {
		this.clientSchemaStorage = clientSchemaStorage;
		return this;
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, Buffer bodyData, String contentType) {
		String uri = BASEURI + path;
		MeshResponseHandler<T> handler = new MeshResponseHandler<>(classOfT, this, method, uri);

		HttpClientRequest request = client.request(method, uri, handler);
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}

		if (getCookie() != null) {
			request.headers().add("Cookie", getCookie());
		} else {
			request.headers().add("Authorization", "Basic " + authEnc);
		}
		request.headers().add("Accept", "application/json");

		if (bodyData.length() != 0) {
			request.headers().add("content-length", String.valueOf(bodyData.length()));
			if (!StringUtils.isEmpty(contentType)) {
				request.headers().add("content-type", contentType);
			}
			request.write(bodyData);
		}
		request.end();
		return handler.getFuture();
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, RestModel restModel) {
		Buffer buffer = Buffer.buffer();
		String json = JsonUtil.toJson(restModel);
		if (log.isDebugEnabled()) {
			log.debug(json);
		}
		buffer.appendString(json);
		return handleRequest(method, path, classOfT, buffer, "application/json");
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, String jsonBodyData) {

		Buffer buffer = Buffer.buffer();
		if (!StringUtils.isEmpty(jsonBodyData)) {
			buffer.appendString(jsonBodyData);
		}

		return handleRequest(method, path, classOfT, buffer, "application/json");
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT) {
		return handleRequest(method, path, classOfT, Buffer.buffer(), null);
	}

	/**
	 * Return the query aggregated parameter string for the given providers.
	 * 
	 * @param parameters
	 * @return
	 */
	public static String getQuery(QueryParameterProvider... parameters) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			QueryParameterProvider provider = parameters[i];
			builder.append(provider.getQueryParameters());
			if (i != parameters.length - 1) {
				builder.append("&");
			}
		}
		if (builder.length() > 0) {
			return "?" + builder.toString();
		} else {
			return "";
		}
	}

}
