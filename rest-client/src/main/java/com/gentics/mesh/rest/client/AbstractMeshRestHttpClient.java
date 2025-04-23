package com.gentics.mesh.rest.client;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.JWTAuthentication;
import com.gentics.mesh.rest.client.impl.HttpMethod;

import io.reactivex.Single;

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
	public String getAPIKey() {
		return authentication.getToken();
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
	 * Prepare a request for uploading a file as multipart/form-data
	 * @param <T> type of the response
	 * @param method Http method
	 * @param path Request path
	 * @param classOfT POJO class for the response
	 * @param fileName file name
	 * @param contentType content type
	 * @param fileData file data as input stream
	 * @param fileSize file size
	 * @param fields map containing additional fields which should be contained in the form
	 * @return request
	 */
	abstract public <T> MeshRequest<T> prepareFileuploadRequest(HttpMethod method, String path,
			Class<? extends T> classOfT, String fileName, String contentType, InputStream fileData, long fileSize,
			Map<String, String> fields);

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
	 * Should the JSON body be minified?
	 * 
	 * @return
	 */
	abstract protected boolean isMinifyJson();

	/**
	 * Return the query aggregated parameter string for the given providers.
	 * @param config configuration object (may be null)
	 * @param parameters
	 * 
	 * @return
	 */
	public static String getQuery(MeshRestClientConfig config, ParameterProvider... parameters) {
		return getQuery(config, Stream.of(parameters));
	}

	/**
	 * Return the query aggregated parameter string for the given providers stream.
	 * @param config configuration object (may be null)
	 * @param parameters
	 * 
	 * @return
	 */
	public static String getQuery(MeshRestClientConfig config, Stream<ParameterProvider> parameters) {
		Map<String, String> params = new LinkedHashMap<>();
		if (config != null) {
			// get the default parameters from the configuration (if any)
			ParameterProvider[] defaultParameters = config.getDefaultParameters();
			// put all non-blank parameters to the map
			Stream.of(defaultParameters).flatMap(provider -> provider.getParameters().entrySet().stream())
					.filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
					.forEach(entry -> params.put(entry.getKey(), entry.getValue()));
			}

		// put all non-blank parameters from the given providers to the map
		parameters.flatMap(provider -> provider.getParameters().entrySet().stream())
				.filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
				.forEach(entry -> params.put(entry.getKey(), entry.getValue()));

		// combine all parameters to a query string
		String query = params.entrySet().stream().map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(Collectors.joining("&"));
		if (query.length() > 0) {
			return "?" + query;
		} else {
			return "";
		}
	}
}
