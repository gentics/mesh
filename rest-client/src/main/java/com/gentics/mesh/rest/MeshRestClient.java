package com.gentics.mesh.rest;

import com.gentics.mesh.etc.config.AuthenticationOptions.AuthenticationMethod;
import com.gentics.mesh.rest.impl.MeshRestClientImpl;
import com.gentics.mesh.rest.method.AdminClientMethods;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.MicroschemaClientMethods;
import com.gentics.mesh.rest.method.NavRootClientMethods;
import com.gentics.mesh.rest.method.NavigationClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.NodeFieldAPIClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.SchemaClientMethods;
import com.gentics.mesh.rest.method.SearchClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;
import com.gentics.mesh.rest.method.UtilityClientMethods;
import com.gentics.mesh.rest.method.WebRootClientMethods;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.web.RoutingContext;

public interface MeshRestClient extends NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods,
		SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods,
		MicroschemaClientMethods, NodeFieldAPIClientMethods, UtilityClientMethods, NavigationClientMethods, NavRootClientMethods {

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
		return new MeshRestClientImpl(host, port, vertx, authenticationMethod);
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
		return new MeshRestClientImpl(host, vertx);
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
	 * Set the login information according to the request headers of the provided context
	 * 
	 * Also initializes the correct authentication provider dependent on the request headers.
	 * 
	 * @param context
	 * @return Fluent API
	 */
	MeshRestClient initializeAuthenticationProvider(RoutingContext context);

	/**
	 * Return the client schema storage that is used to deserialize those responses that use a schema.
	 * 
	 * @return
	 */
	ClientSchemaStorage getClientSchemaStorage();

	/**
	 * Set the schema storage.
	 * 
	 * @param schemaStorage
	 * @return Fluent API
	 */
	MeshRestClient setClientSchemaStorage(ClientSchemaStorage schemaStorage);

	/**
	 * Close the client.
	 */
	void close();

}
