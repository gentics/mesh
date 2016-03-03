package com.gentics.mesh.context.impl;

import static com.gentics.mesh.rest.AbstractMeshRestHttpClient.getQuery;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.QueryParameterProvider;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;

public class LocalActionContextImpl<T> extends AbstractInternalActionContext implements InternalActionContext {

	private RestModel payloadObject;
	private MeshAuthUser user;
	private Map<String, Object> data = new HashMap<>();
	private Map<String, String> parameters = new HashMap<>();
	private String query;
	private Project project;
	private String responseBody;
	private HttpResponseStatus responseStatusCode;
	private Future<T> future = Future.future();
	private Class<? extends T> classOfResponse;

	public LocalActionContextImpl(MeshAuthUser user, Class<? extends T> classOfResponse, QueryParameterProvider... parameters) {
		this.query = getQuery(parameters);
		this.user = user;
		this.classOfResponse = classOfResponse;
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

	public void setPayloadObject(RestModel model) {
		this.payloadObject = model;
	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	/**
	 * Set request context specific path parameters.
	 * 
	 * @param name
	 * @param value
	 */
	public void setParameter(String name, String value) {
		this.parameters.put(name, value);
	}

	@Override
	public void send(String body, HttpResponseStatus statusCode) {
		this.responseBody = body;
		this.responseStatusCode = statusCode;
		try {
			T model = JsonUtil.readValue(responseBody, classOfResponse);
			future.complete(model);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getResponseBody() {
		return responseBody;
	}

	public HttpResponseStatus getResponseStatusCode() {
		return responseStatusCode;
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
	 * @param project
	 */
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Set<FileUpload> getFileUploads() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultiMap requestHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCookie(Cookie cookie) {
	}

	//	public void setResponseType(Class<T> classOfT) {
	//		this.classOfResponse = classOfT;
	//	}

	/**
	 * Return the future which will be completed on sending or failure.
	 * 
	 * @return
	 */
	public Future<T> getFuture() {
		return future;
	}

}
