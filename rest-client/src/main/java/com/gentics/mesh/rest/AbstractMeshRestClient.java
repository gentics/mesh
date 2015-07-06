package com.gentics.mesh.rest;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;

public abstract class AbstractMeshRestClient implements NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods,
		GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshRestClient.class);

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	protected String username;
	protected String password;
	protected String authEnc;

	protected ClientSchemaStorage clientSchemaStorage = new ClientSchemaStorage();

	protected HttpClient client;

	private static String cookie;

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

	public static String getCookie() {
		return cookie;
	}

	public static String getBaseuri() {
		return BASEURI;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	public static void setCookie(String cookie) {
		AbstractMeshRestClient.cookie = cookie;
	}

	public ClientSchemaStorage getClientSchemaStorage() {
		return clientSchemaStorage;
	}

	public void setClientSchemaStorage(ClientSchemaStorage clientSchemaStorage) {
		this.clientSchemaStorage = clientSchemaStorage;
	}

	protected Future<TagResponse> handleRequest(String string, Class<TagResponse> class1, TagUpdateRequest tagUpdateRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected <T extends AbstractRestModel> Future<T> handleRequest(String path, Class<T> ClassOfT) {
		MeshResponseHandler<T> handler = new MeshResponseHandler<>(ClassOfT);

		String uri = BASEURI + path;
		HttpClientRequest request = client.get(uri, handler);
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}
		System.out.println(path);
		System.out.println(getCookie());
		System.out.println(authEnc);
		if (getCookie() != null) {
			request.headers().add("Cookie", getCookie());
		}
		request.headers().add("Authorization", "Basic " + authEnc);
		request.headers().add("Accept", "application/json");
		request.end();
		return handler.getFuture();

	}
}
