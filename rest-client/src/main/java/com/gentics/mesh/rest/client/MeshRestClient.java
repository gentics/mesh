package com.gentics.mesh.rest.client;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.rest.JWTAuthentication;
import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import com.gentics.mesh.rest.client.method.AdminClientMethods;
import com.gentics.mesh.rest.client.method.AdminPluginClientMethods;
import com.gentics.mesh.rest.client.method.ApiInfoClientMethods;
import com.gentics.mesh.rest.client.method.AuthClientMethods;
import com.gentics.mesh.rest.client.method.EventbusClientMethods;
import com.gentics.mesh.rest.client.method.GraphQLClientMethods;
import com.gentics.mesh.rest.client.method.GroupClientMethods;
import com.gentics.mesh.rest.client.method.JobClientMethods;
import com.gentics.mesh.rest.client.method.MicroschemaClientMethods;
import com.gentics.mesh.rest.client.method.NavRootClientMethods;
import com.gentics.mesh.rest.client.method.NavigationClientMethods;
import com.gentics.mesh.rest.client.method.NodeBinaryFieldClientMethods;
import com.gentics.mesh.rest.client.method.NodeClientMethods;
import com.gentics.mesh.rest.client.method.ProjectClientMethods;
import com.gentics.mesh.rest.client.method.BranchClientMethods;
import com.gentics.mesh.rest.client.method.RoleClientMethods;
import com.gentics.mesh.rest.client.method.SchemaClientMethods;
import com.gentics.mesh.rest.client.method.SearchClientMethods;
import com.gentics.mesh.rest.client.method.TagClientMethods;
import com.gentics.mesh.rest.client.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.client.method.UserClientMethods;
import com.gentics.mesh.rest.client.method.UtilityClientMethods;
import com.gentics.mesh.rest.client.method.WebRootClientMethods;

public interface MeshRestClient extends NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods,
	SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods,
	AdminPluginClientMethods, MicroschemaClientMethods, NodeBinaryFieldClientMethods, UtilityClientMethods, NavigationClientMethods,
	NavRootClientMethods, EventbusClientMethods, BranchClientMethods, ApiInfoClientMethods, GraphQLClientMethods, JobClientMethods {

	/**
	 * The default base URI path to the Mesh-API.
	 */
	String DEFAULT_BASEURI = "/api/v1";

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @param port
	 *            Server port
	 * @param ssl
	 *            Flag which is used to toggle ssl mode
	 * @return
	 */
	static MeshRestClient create(String host, int port, boolean ssl) {
		return new MeshRestOkHttpClientImpl(host, port, ssl);
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @return
	 */
	static MeshRestClient create(String host) {
		return new MeshRestOkHttpClientImpl(host);
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
	 * Close the client.
	 */
	void close();

	/**
	 * Set the API key. This is an alternative way for authentication. Use {@link #setLogin(String, String)} if you prefer to use regular JWT tokens.
	 * 
	 * @param apiKey
	 * @return Fluent API
	 */
	MeshRestClient setAPIKey(String apiKey);

	/**
	 * Disable the anonymous access handling. Requests will only work if you are logged in.
	 * 
	 * @return Fluent API
	 */
	MeshRestClient disableAnonymousAccess();

	/**
	 * Enable the anonymous access handling. Requests will work if anonymous access is enabled on the Gentics Mesh serve.
	 * 
	 * @return Fluent API
	 */
	MeshRestClient enableAnonymousAccess();

	/**
	 * Set the authentication provider.
	 *
	 * @param authentication
	 * @return Fluent API
	 */
	MeshRestClient setAuthenticationProvider(JWTAuthentication authentication);

	/**
	 * Get the base URI path to the Mesh-API. If the base URI is not set, the DEFAULT_BASE_URI is returned.
	 *
	 * @return the base URI
	 */
	String getBaseUri();

	/**
	 * Set the base path to the Mesh API used for all request paths.
	 * 
	 * @param uri
	 * @return Fluent API
	 */
	MeshRestClient setBaseUri(String uri);

	/**
	 * Return the mesh version.
	 * 
	 * @return
	 */
	public static String getPlainVersion() {
		return MeshVersion.getPlainVersion();
	}

	/**
	 * Return the JWT Authentication provider.
	 * 
	 * @return
	 */
	JWTAuthentication getAuthentication();
}
