package com.gentics.mesh.rest.client;

import com.gentics.mesh.etc.config.AuthenticationMethod;
import com.gentics.mesh.rest.client.impl.MeshRestHttpClientImpl;
import com.gentics.mesh.rest.client.method.AdminClientMethods;
import com.gentics.mesh.rest.client.method.ApiInfoClientMethods;
import com.gentics.mesh.rest.client.method.AuthClientMethods;
import com.gentics.mesh.rest.client.method.EventbusClientMethods;
import com.gentics.mesh.rest.client.method.GroupClientMethods;
import com.gentics.mesh.rest.client.method.MicroschemaClientMethods;
import com.gentics.mesh.rest.client.method.NavRootClientMethods;
import com.gentics.mesh.rest.client.method.NavigationClientMethods;
import com.gentics.mesh.rest.client.method.NodeClientMethods;
import com.gentics.mesh.rest.client.method.NodeFieldAPIClientMethods;
import com.gentics.mesh.rest.client.method.ProjectClientMethods;
import com.gentics.mesh.rest.client.method.ReleaseClientMethods;
import com.gentics.mesh.rest.client.method.RoleClientMethods;
import com.gentics.mesh.rest.client.method.SchemaClientMethods;
import com.gentics.mesh.rest.client.method.SearchClientMethods;
import com.gentics.mesh.rest.client.method.TagClientMethods;
import com.gentics.mesh.rest.client.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.client.method.UserClientMethods;
import com.gentics.mesh.rest.client.method.UtilityClientMethods;
import com.gentics.mesh.rest.client.method.WebRootClientMethods;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public interface MeshRestClient extends NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods,
		SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods,
		MicroschemaClientMethods, NodeFieldAPIClientMethods, UtilityClientMethods, NavigationClientMethods, NavRootClientMethods, EventbusClientMethods,
		ReleaseClientMethods, ApiInfoClientMethods {

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @param port
	 *            Server port
	 * @param vertx
	 *            Vertx instance to be used in combination with the vertx http client
	 * @param authenticationMethod
	 *            Authentication method to be used
	 * @return
	 */
	static MeshRestClient create(String host, int port, Vertx vertx, AuthenticationMethod authenticationMethod) {
		return new MeshRestHttpClientImpl(host, port, vertx, authenticationMethod);
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @param vertx
	 *            Vertx instance to be used in combination with the vertx http client
	 * @return
	 */
	static MeshRestClient create(String host, Vertx vertx) {
		return new MeshRestHttpClientImpl(host, vertx);
	}

	/**
	 * Return the underlying vertx http client.
	 * 
	 * @return
	 */
	HttpClient getClient();

	/**
	 * Set the login that is used to authenticate the requests.
	 * 
	 * @param username
	 * @param password
	 * @return Fluent API
	 */
	MeshRestClient setLogin(String username, String password);

	/**
	 * Close the client.
	 */
	void close();

}
