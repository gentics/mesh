package com.gentics.mesh.rest;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.impl.MeshRestClientImpl;
import com.gentics.mesh.rest.method.AdminClientMethods;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.MicroschemaClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.NodeFieldAPIClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.SchemaClientMethods;
import com.gentics.mesh.rest.method.SearchClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;
import com.gentics.mesh.rest.method.WebRootClientMethods;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public interface MeshRestClient extends NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods,
		SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods, MicroschemaClientMethods, NodeFieldAPIClientMethods {

	static MeshRestClient create(String host, int port, Vertx vertx) {
		return new MeshRestClientImpl(host, port, vertx);
	}

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
	 * Return the cookie that is currently used when invoking http requests.
	 * 
	 * @return
	 */
	String getCookie();

	/**
	 * Set the cookie that should be used when invoking requests.
	 * 
	 * @param cookie
	 * @return
	 */
	MeshRestClient setCookie(String cookie);

	/**
	 * Set the session id. Internally the session cookie will be set using the given id.
	 * 
	 * @param id
	 * @return
	 */
	default MeshRestClient setSessionId(String id) {
		return setCookie(MeshOptions.MESH_SESSION_KEY + "=" + id);
	}

	/**
	 * Set the login that is used to authenticate the requests.
	 * 
	 * @param username
	 * @param password
	 * @return Fluent API
	 */
	MeshRestClient setLogin(String username, String password);

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
