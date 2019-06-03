package com.gentics.mesh.rest.client;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.JWTAuthentication;
import com.gentics.mesh.rest.client.impl.HttpMethod;
import io.reactivex.Single;

import java.io.InputStream;

/**
 * Abstract class for mesh REST clients.
 */
public abstract class AbstractMeshRestHttpClient implements MeshRestClient {

	public static final int DEFAULT_PORT = 8080;

	protected JWTAuthentication authentication = new JWTAuthentication();

	protected boolean disableAnonymousAccess = false;

	@Override
	public MeshRestClient setLogin(String username, String password) {
		authentication.setLogin(username, password);
		return this;
	}

	@Override
	public MeshRestClient setLogin(String username, String password, String newPassword) {
		authentication.setLogin(username, password, newPassword);
		return this;
	}

	@Override
	public MeshRestClient setAPIKey(String apiKey) {
		// Internally the API is just a regular JWT which does not expire.
		authentication.setToken(apiKey);
		return this;
	}

	@Override
	public Single<GenericMessageResponse> login() {
		return authentication.login(this);
	}

	@Override
	public Single<GenericMessageResponse> logout() {
		return authentication.logout(this);
	}

	@Override
	public MeshRestClient setAuthenticationProvider(JWTAuthentication authentication) {
		this.authentication = authentication;
		return this;
	}

	/**
	 * Get the authentication provider.
	 *
	 * @return
	 */
	@Override
	public JWTAuthentication getAuthentication() {
		return authentication;
	}

	/**
	 * Prepare the request using the provides information and return a mesh request which is ready to be invoked.
	 *
	 * @param method
	 *            Http method
	 * @param path
	 *            Request path
	 * @param classOfT
	 *            POJO class for the response
	 * @param bodyData
	 *            Buffer which contains the body data which should be send to the server
	 * @param fileSize Total size of the data in bytes
	 * @param contentType
	 *            Content type of the posted data
	 * @return
	 */
	abstract public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, InputStream bodyData, long fileSize, String contentType);

	/**
	 * Prepare the request using the provides information and return a mesh request which is ready to be invoked.
	 *
	 * @param method
	 *            Http method
	 * @param path
	 *            Request path
	 * @param classOfT
	 *            POJO class for the response
	 * @param restModel
	 *            Rest model which should be used to construct the JSON post data
	 * @return
	 */
	abstract public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel);

	/**
	 * Prepare the request using the provides information and return a mesh request which is ready to be invoked.
	 *
	 * @param method
	 * @param path
	 * @param classOfT
	 * @param data
	 * @return
	 */
	abstract public <T> MeshRequest<T> handleTextRequest(HttpMethod method, String path, Class<? extends T> classOfT, String data);

	/**
	 * Prepare the request using the provides information and return a mesh request which is ready to be invoked.
	 *
	 * @param method
	 * @param path
	 * @param classOfT
	 * @param jsonBodyData
	 * @return
	 */
	abstract public <T> MeshRequest<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData);

	/**
	 * Prepare the request using the provides information and return a mesh request which is ready to be invoked.
	 *
	 * @param method
	 *            Http method
	 * @param path
	 *            Request path
	 * @param classOfT
	 *            POJO class for the response
	 * @return
	 */
	abstract public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT);

	/**
	 * Return the query aggregated parameter string for the given providers.
	 * 
	 * @param parameters
	 * @return
	 */
	public static String getQuery(ParameterProvider... parameters) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			ParameterProvider provider = parameters[i];
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
