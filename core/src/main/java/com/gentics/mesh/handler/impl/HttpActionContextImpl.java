package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.HttpActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * @see HttpActionContext
 */
public class HttpActionContextImpl extends AbstractActionContext implements HttpActionContext {

	public static final String LOCALE_MAP_DATA_KEY = "locale";

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
	public void send(String body, HttpResponseStatus statusCode) {
		rc.response().putHeader("Content-Type", APPLICATION_JSON_UTF8);
		rc.response().setStatusCode(statusCode.code()).end(body);
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
		return (Locale) data().computeIfAbsent(LOCALE_MAP_DATA_KEY, map -> {
			String header = rc.request().headers().get("Accept-Language");
			return getLocale(header);
		});
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
		rc.addCookie(Cookie.cookie(MeshOptions.MESH_SESSION_KEY, "deleted").setMaxAge(0));
		rc.clearUser();
	}

}
