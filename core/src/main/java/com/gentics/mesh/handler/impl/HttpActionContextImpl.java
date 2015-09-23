package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.handler.HttpActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class HttpActionContextImpl extends AbstractActionContext implements HttpActionContext {

	private static final Logger log = LoggerFactory.getLogger(HttpActionContextImpl.class);

	private static final String LOCALE_MAP_DATA_KEY = "locale";

	private RoutingContext rc;

	public HttpActionContextImpl(RoutingContext rc) {
		this.rc = rc;
	}

	protected RoutingContext getRoutingContext() {
		return rc;
	}

	@Override
	public String getParameter(String parameterName) {
		return rc.request().getParam(parameterName);
	}

	@Override
	public void send(String body) {
		rc.response().putHeader("content-type", APPLICATION_JSON);
		// TODO use 201 for created entities
		rc.response().setStatusCode(200).end(body);
	}

	@Override
	public MultiMap getParameters() {
		return rc.request().params();
	}

	@Override
	public Map<String, Object> data() {
		return rc.data();
	}

	@Override
	public String query() {
		return rc.request().query();
	}

	@Override
	public void fail(HttpResponseStatus status, String i18nKey, String... parameters) {
		rc.fail(new HttpStatusCodeErrorException(status, i18n(i18nKey, parameters)));
	}

	@Override
	public void fail(HttpResponseStatus status, String i18nKey, Throwable cause) {
		rc.fail(new HttpStatusCodeErrorException(status, i18n(i18nKey), cause));
	}

	@Override
	public void fail(Throwable cause) {
		rc.fail(cause);
	}

	@Override
	public String getBodyAsString() {
		return rc.getBodyAsString();
	}

	@Override
	public Locale getLocale() {
		data().computeIfAbsent(LOCALE_MAP_DATA_KEY, map -> {
			String header = rc.request().headers().get("Accept-Language");
			return getLocale(header);
		});
		return (Locale) data().get(LOCALE_MAP_DATA_KEY);
	}

	@Override
	public Set<FileUpload> getFileUploads() {
		return rc.fileUploads();
	}

	@Override
	public MultiMap requestHeaders() {
		return rc.request().headers();
	}

	@Override
	public void logout() {
		rc.session().destroy();
		rc.clearUser();
	}

}
