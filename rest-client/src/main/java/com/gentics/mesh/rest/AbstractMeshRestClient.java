package com.gentics.mesh.rest;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;

public abstract class AbstractMeshRestClient implements NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods,
		GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods {

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

	protected <T extends AbstractRestModel> Future<T> handleRequest(String path, Class<T> ClassOfT) {
		MeshResponseHandler<T> handler = new MeshResponseHandler<>(ClassOfT);
		HttpClientRequest request = client.get(BASEURI + "/auth/me", handler);

		System.out.println(getCookie());
		request.headers().add("Cookie", getCookie());
		request.headers().add("Authorization", "Basic " + authEnc);
		request.headers().add("Accept", "application/json");
		request.end();
		return handler.getFuture();

	}
}
