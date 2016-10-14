package com.gentics.mesh.rest.client;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.MeshRestClientAuthenticationProvider;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * Abstract class for mesh rest clients.
 */
public abstract class AbstractMeshRestHttpClient implements MeshRestClient {

	protected static final Logger log = LoggerFactory.getLogger(AbstractMeshRestHttpClient.class);

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	protected HttpClient client;

	protected MeshRestClientAuthenticationProvider authentication;

	@Override
	public MeshRestClient setLogin(String username, String password) {
		authentication.setLogin(username, password);
		return this;
	}

	@Override
	public HttpClient getClient() {
		return client;
	}

	@Override
	public void close() {
		client.close();
	}

	public static String getBaseuri() {
		return BASEURI;
	}

	public void setClient(HttpClient client) {
		this.client = client;
	}

	@Override
	public Single<GenericMessageResponse> login() {
		return authentication.login(getClient());
	}

	@Override
	public Single<GenericMessageResponse> logout() {
		return authentication.logout(getClient());
	}

	/**
	 * Set the authentication provider.
	 * 
	 * @param authentication
	 */
	public void setAuthentication(MeshRestClientAuthenticationProvider authentication) {
		this.authentication = authentication;
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
	 * @param contentType
	 *            Content type of the posted data
	 * @return
	 */
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, Buffer bodyData, String contentType) {
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, bodyData, contentType, client, authentication);
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
	 * @param restModel
	 *            Rest model which should be used to construct the JSON post data
	 * @return
	 */
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel) {
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, restModel, client, authentication);
	}

	/**
	 * Prepare the request using the provides information and return a mesh request which is ready to be invoked.
	 * 
	 * @param method
	 * @param path
	 * @param classOfT
	 * @param jsonBodyData
	 * @return
	 */
	protected <T> MeshRequest<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData) {
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, jsonBodyData, client, authentication);
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
	 * @return
	 */
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, client, authentication);
	}

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
