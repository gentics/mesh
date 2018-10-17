package com.gentics.mesh.rest.client;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.JWTAuthentication;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.reactivex.Single;

/**
 * Abstract class for mesh REST clients.
 */
public abstract class AbstractMeshRestHttpClient implements MeshRestClient {

	protected static final Logger log = LoggerFactory.getLogger(AbstractMeshRestHttpClient.class);

	public static final int DEFAULT_PORT = 8080;

	private Vertx vertx;

	private HttpClientOptions clientOptions;

	private Set<HttpClient> clientSet = new HashSet<>();

	private ThreadLocal<HttpClient> localClient = ThreadLocal.withInitial(() -> {
		HttpClient client = vertx.createHttpClient(clientOptions);
		clientSet.add(client);
		return client;
	});

	protected JWTAuthentication authentication;

	protected boolean disableAnonymousAccess = false;

	private String baseUri = DEFAULT_BASEURI;

	public AbstractMeshRestHttpClient(HttpClientOptions options, Vertx vertx) {
		this.clientOptions = options;
		this.vertx = vertx;
	}

	public AbstractMeshRestHttpClient(String host, int port, boolean ssl, Vertx vertx) {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost(host);
		options.setTryUseCompression(true);
		options.setDefaultPort(port);
		options.setSsl(ssl);
		this.clientOptions = options;
		this.vertx = vertx;
	}

	@Override
	public MeshRestClient setLogin(String username, String password) {
		authentication.setLogin(username, password);
		return this;
	}

	@Override
	public MeshRestClient setAPIKey(String apiKey) {
		// Internally the API is just a regular JWT which does not expire.
		authentication.setToken(apiKey);
		return this;
	}

	@Override
	public HttpClient getClient() {
		return localClient.get();
	}

	@Override
	public void close() {
		clientSet.forEach(client -> client.close());
		clientSet.clear();
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
	 * @param contentType
	 *            Content type of the posted data
	 * @return
	 */
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, Buffer bodyData, String contentType) {
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, bodyData, contentType, this, authentication, disableAnonymousAccess,
				"application/json");
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
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, restModel, this, authentication, disableAnonymousAccess);
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
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, jsonBodyData, this, authentication, disableAnonymousAccess);
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
		return MeshRestRequestUtil.prepareRequest(method, path, classOfT, this, authentication, disableAnonymousAccess);
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

	@Override
	public MeshRestClient setBaseUri(String baseUri) {
		this.baseUri = baseUri;
		return this;
	}

	@Override
	public String getBaseUri() {
		return baseUri;
	}

	@Override
	public Vertx vertx() {
		return vertx;
	}

}
