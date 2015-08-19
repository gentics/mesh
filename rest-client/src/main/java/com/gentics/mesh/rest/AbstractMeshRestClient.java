package com.gentics.mesh.rest;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.method.AdminClientMethods;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.SchemaClientMethods;
import com.gentics.mesh.rest.method.SearchClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;
import com.gentics.mesh.rest.method.WebRootClientMethods;

public abstract class AbstractMeshRestClient
		implements NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods, SchemaClientMethods,
		GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods {

	protected static final Logger log = LoggerFactory.getLogger(AbstractMeshRestClient.class);

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	protected String username;
	protected String password;
	protected String authEnc;

	protected ClientSchemaStorage clientSchemaStorage = new ClientSchemaStorage();

	protected HttpClient client;

	private String cookie;

	public void setLogin(String username, String password) {
		this.username = username;
		this.password = password;
		String authStringEnc = username + ":" + password;
		authEnc = new String(Base64.encodeBase64(authStringEnc.getBytes()));
	}

	public HttpClient getClient() {
		return client;
	}

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

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public ClientSchemaStorage getClientSchemaStorage() {
		return clientSchemaStorage;
	}

	public void setClientSchemaStorage(ClientSchemaStorage clientSchemaStorage) {
		this.clientSchemaStorage = clientSchemaStorage;
	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, Buffer bodyData, String contentType) {
		MeshResponseHandler<T> handler = new MeshResponseHandler<>(classOfT, this);

		String uri = BASEURI + path;
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

	protected String getQuery(QueryParameterProvider... parameters) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			QueryParameterProvider provider = parameters[i];
			builder.append(provider.getQueryParameters());
			if(i!=parameters.length-1) {
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
