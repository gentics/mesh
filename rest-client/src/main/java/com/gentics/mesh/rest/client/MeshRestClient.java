package com.gentics.mesh.rest.client;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.rest.JWTAuthentication;
import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import com.gentics.mesh.rest.client.method.AdminClientMethods;
import com.gentics.mesh.rest.client.method.AdminPluginClientMethods;
import com.gentics.mesh.rest.client.method.ApiInfoClientMethods;
import com.gentics.mesh.rest.client.method.AuthClientMethods;
import com.gentics.mesh.rest.client.method.BranchClientMethods;
import com.gentics.mesh.rest.client.method.EventbusClientMethods;
import com.gentics.mesh.rest.client.method.GenericHttpMethods;
import com.gentics.mesh.rest.client.method.GraphQLClientMethods;
import com.gentics.mesh.rest.client.method.GroupClientMethods;
import com.gentics.mesh.rest.client.method.HealthClientMethods;
import com.gentics.mesh.rest.client.method.JobClientMethods;
import com.gentics.mesh.rest.client.method.MicroschemaClientMethods;
import com.gentics.mesh.rest.client.method.NavRootClientMethods;
import com.gentics.mesh.rest.client.method.NavigationClientMethods;
import com.gentics.mesh.rest.client.method.NodeBinaryFieldClientMethods;
import com.gentics.mesh.rest.client.method.NodeClientMethods;
import com.gentics.mesh.rest.client.method.ProjectClientMethods;
import com.gentics.mesh.rest.client.method.RoleClientMethods;
import com.gentics.mesh.rest.client.method.SchemaClientMethods;
import com.gentics.mesh.rest.client.method.SearchClientMethods;
import com.gentics.mesh.rest.client.method.TagClientMethods;
import com.gentics.mesh.rest.client.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.client.method.UserClientMethods;
import com.gentics.mesh.rest.client.method.UtilityClientMethods;
import com.gentics.mesh.rest.client.method.WebRootClientMethods;

import okhttp3.OkHttpClient;

public interface MeshRestClient extends NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods,
	SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods,
	AdminPluginClientMethods, MicroschemaClientMethods, NodeBinaryFieldClientMethods, UtilityClientMethods, NavigationClientMethods,
	NavRootClientMethods, EventbusClientMethods, BranchClientMethods, ApiInfoClientMethods, GraphQLClientMethods, JobClientMethods,
	GenericHttpMethods, HealthClientMethods {

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
		return create(new MeshRestClientConfig.Builder()
			.setHost(host)
			.setPort(port)
			.setSsl(ssl)
			.build());
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @return
	 */
	static MeshRestClient create(String host) {
		return create(new MeshRestClientConfig.Builder().setHost(host).build());
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param config
	 *            Client configuration
	 * @return
	 */
	static MeshRestClient create(MeshRestClientConfig config) {
		return new MeshRestOkHttpClientImpl(config);
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param config
	 *            Client configuration
	 * @param client
	 *            Ok http client to be used
	 * @return
	 */
	static MeshRestClient create(MeshRestClientConfig config, OkHttpClient client) {
		return new MeshRestOkHttpClientImpl(config, client);
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
	 * Set the login that is used to authenticate the requests. This will also set a new password when {@link #login()} is called. Should be used when the user
	 * has to change the password.
	 *
	 * @param username
	 * @param password
	 * @return Fluent API
	 */
	MeshRestClient setLogin(String username, String password, String newPassword);

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
	 * Return the set api key from the client.
	 * 
	 * @return
	 */
	String getAPIKey();

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
