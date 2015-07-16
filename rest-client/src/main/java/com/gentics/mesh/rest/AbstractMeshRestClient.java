package com.gentics.mesh.rest;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.SchemaClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;
import com.gentics.mesh.rest.method.WebRootClientMethods;

public abstract class AbstractMeshRestClient implements NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods,
		WebRootClientMethods, SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshRestClient.class);

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

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> ClassOfT, Object requestModel) {

		Buffer buffer = Buffer.buffer();
		if (requestModel != null) {
			String json = JsonUtil.toJson(requestModel);
			buffer.appendString(json);
		}
		MeshResponseHandler<T> handler = new MeshResponseHandler<>(ClassOfT, this);

		String uri = BASEURI + path;
		System.out.println(uri);
		HttpClientRequest request = client.request(method, uri, handler);
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}

		if (getCookie() != null) {
			request.headers().add("Cookie", getCookie());
		}
		//		request.headers().add("Authorization", "Basic " + authEnc);
		request.headers().add("Accept", "application/json");
		if (buffer.length() != 0) {
			request.headers().add("content-length", String.valueOf(buffer.length()));
			request.headers().add("content-type", "application/json");
			request.write(buffer);
		}

		request.end();
		return handler.getFuture();

	}

	protected <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> ClassOfT) {
		return handleRequest(method, path, ClassOfT, null);
	}
}
