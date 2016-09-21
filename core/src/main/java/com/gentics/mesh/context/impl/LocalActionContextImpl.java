package com.gentics.mesh.context.impl;

import static com.gentics.mesh.rest.client.AbstractMeshRestHttpClient.getQuery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.util.HttpQueryUtils;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;

/**
 * Implementation of a local action context. The local action context does not rely on a routing context.
 *
 * @param <T>
 *            Type of the response object
 */
public class LocalActionContextImpl<T> extends AbstractInternalActionContext implements InternalActionContext {

	private RestModel payloadObject;
	private MeshAuthUser user;
	private Map<String, Object> data = new HashMap<>();
	private MultiMap parameters = MultiMap.caseInsensitiveMultiMap();
	private String query;
	private Project project;
	private String responseBody;
	private HttpResponseStatus responseStatus;
	private Future<T> future = Future.future();
	private Class<? extends T> classOfResponse;
	private Set<FileUpload> fileUploads = new HashSet<>();

	/**
	 * Create a new local action context.
	 * 
	 * @param user
	 *            User to be used for authentication
	 * @param classOfResponse
	 *            Response object class
	 * @param requestParameters
	 *            Query parameters which will form the complete query string
	 */
	public LocalActionContextImpl(MeshAuthUser user, Class<? extends T> classOfResponse, ParameterProvider... requestParameters) {
		this.query = getQuery(requestParameters);
		this.user = user;
		this.classOfResponse = classOfResponse;

		for (ParameterProvider requestParameter : requestParameters) {
			Map<String, String> paramMap = HttpQueryUtils.splitQuery(requestParameter.getQueryParameters());
			for (Entry<String, String> entry : paramMap.entrySet()) {
				parameters.add(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public Map<String, Object> data() {
		return data;
	}

	/**
	 * Set the query parameter for the request.
	 * 
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public String query() {
		return query;
	}

	@Override
	public String getBodyAsString() {
		return JsonUtil.toJson(payloadObject);
	}

	@Override
	public void setUser(MeshAuthUser user) {
		this.user = user;
	}

	@Override
	public MeshAuthUser getUser() {
		return user;
	}

	/**
	 * Set the payload object.
	 * 
	 * @param model
	 */
	public void setPayloadObject(RestModel model) {
		this.payloadObject = model;
	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public MultiMap getParameters() {
		return parameters;
	}

	@Override
	public void setParameter(String name, String value) {
		this.parameters.add(name, value);
	}

	@Override
	public void send(String body, HttpResponseStatus status) {
		this.responseBody = body;
		this.responseStatus = status;
		T model = JsonUtil.readValue(responseBody, classOfResponse);
		future.complete(model);
	}

	@Override
	public void send(HttpResponseStatus status) {
		future.complete();
	}

	/**
	 * Return the response body string.
	 * 
	 * @return
	 */
	public String getResponseBody() {
		return responseBody;
	}

	/**
	 * Return the response status code.
	 * 
	 * @return
	 */
	public HttpResponseStatus getResponseStatusCode() {
		return responseStatus;
	}

	@Override
	public void fail(Throwable cause) {
		future.fail(cause);
	}

	@Override
	public Locale getLocale() {
		Locale locale = new Locale("en", "EN");
		return locale;
	}

	@Override
	public void logout() {

	}

	@Override
	public Project getProject() {
		return project;
	}

	/**
	 * Set the project that will be used to invoke project scope specific actions.
	 * 
	 * @param projectName
	 */
	public void setProject(String projectName) {
		MeshInternal.get().database().noTx(() -> {
			BootstrapInitializer boot = MeshInternal.get().boot();
			boot.projectRoot().reload();
			Project project = boot.projectRoot().findByName(projectName);
			this.project = project;
			return null;
		});
	}

	@Override
	public Set<FileUpload> getFileUploads() {
		return fileUploads;
	}

	@Override
	public MultiMap requestHeaders() {
		// Not supported
		return null;
	}

	@Override
	public void addCookie(Cookie cookie) {
		// Not supported
	}

	@Override
	public void setEtag(String entityTag, boolean isWeak) {
		// Not supported
	}

	@Override
	public void setLocation(String location) {
		// Not supported
	}

	@Override
	public boolean matches(String etag, boolean isWeak) {
		return false;
	}

	/**
	 * Return the future which will be completed on sending or failure.
	 * 
	 * @return
	 */
	public Future<T> getFuture() {
		return future;
	}

}
