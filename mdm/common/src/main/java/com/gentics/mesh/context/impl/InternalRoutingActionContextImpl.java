package com.gentics.mesh.context.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpHeaders.CACHE_CONTROL;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.shared.SharedKeys;
import com.gentics.mesh.util.ETag;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

/**
 * Vert.x specific routing context based action context implementation.
 */
public class InternalRoutingActionContextImpl extends AbstractInternalActionContext {

	private static final Logger log = LoggerFactory.getLogger(InternalRoutingActionContextImpl.class);

	private RoutingContext rc;

	private MeshAuthUser user;

	private Map<String, Object> data;

	private HttpServerConfig httpServerConfig;

	public static final String LOCALE_MAP_DATA_KEY = "locale";

	/**
	 * Create a new routing context based vertx action context.
	 * 
	 * @param rc
	 */
	public InternalRoutingActionContextImpl(RoutingContext rc) {
		this(rc, Tx.maybeGet().map(tx -> tx.data().options().getHttpServerOptions()).orElse(null));
	}

	/**
	 * Create a new routing context based vertx action context.
	 * 
	 * @param rc
	 */
	public InternalRoutingActionContextImpl(RoutingContext rc, HttpServerConfig httpServerConfig) {
		this.rc = rc;
		if (rc.data() != null) {
			this.data = Collections.synchronizedMap(rc.data());
		} else {
			this.data = new ConcurrentHashMap<>();
		}
	}

	@Override
	protected HttpServerConfig getHttpServerConfig() {
		return httpServerConfig;
	}

	@Override
	public String getParameter(String parameterName) {
		return rc.request().getParam(parameterName);
	}

	@Override
	public MultiMap getParameters() {
		return rc.request().params();
	}

	@Override
	public void setParameter(String name, String value) {
		rc.request().params().set(name, value);
	}

	@Override
	public void send(String body, HttpResponseStatus status, String contentType) {
		HttpServerResponse response = rc.response();
		response.putHeader(CONTENT_TYPE, contentType);

		// Check if a custom header has been set
		if (!response.headers().contains(CACHE_CONTROL)) {
			response.putHeader(CACHE_CONTROL, "no-cache");
		}
		response.setStatusCode(status.code()).end(body);
	}

	@Override
	public void send(HttpResponseStatus status) {
		rc.response().setStatusCode(status.code()).end();
	}

	@Override
	public void setEtag(String entityTag, boolean isWeak) {
		rc.response().putHeader(HttpHeaders.ETAG, ETag.prepareHeader(entityTag, isWeak));
	}

	@Override
	public void setWebrootResponseType(String type) {
		rc.response().putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, type);
	}

	@Override
	public void setLocation(String basePath) {
		String protocol = "http://";
		if (rc.request().isSSL()) {
			protocol = "https://";
		}
		String hostAndPort = rc.request().host();
		rc.response().putHeader(HttpHeaders.LOCATION, protocol + hostAndPort + basePath);
	}

	@Override
	public boolean matches(String etag, boolean isWeak) {
		String headerValue = rc.request().getHeader(HttpHeaders.IF_NONE_MATCH);
		if (headerValue == null) {
			return false;
		}
		return headerValue.equals(ETag.prepareHeader(etag, isWeak));
	}

	@Override
	public Map<String, Object> data() {
		return data;
	}

	@Override
	public String query() {
		return rc.request().query();
	}

	@Override
	public void fail(Throwable cause) {
		rc.fail(cause);
	}

	@Override
	public String getBodyAsString() {
		return rc.body().asString();
	}

	@Override
	public Locale getLocale() {
		return (Locale) data().computeIfAbsent(LOCALE_MAP_DATA_KEY, map -> {
			String header = rc.request().headers().get(HttpHeaders.ACCEPT_LANGUAGE);
			return getLocale(header);
		});
	}

	@Override
	public List<FileUpload> getFileUploads() {
		return rc.fileUploads();
	}

	@Override
	public MultiMap requestHeaders() {
		return rc.request().headers();
	}

	@Override
	public void logout() {
		Session session = rc.session();
		if (session != null) {
			session.destroy();
		}
		rc.response().addCookie(Cookie.cookie(SharedKeys.TOKEN_COOKIE_KEY, "deleted").setMaxAge(0).setPath("/"));
		rc.clearUser();
	}

	@Override
	public void addCookie(Cookie cookie) {
		rc.response().addCookie(cookie);
	}

	@Override
	public HibUser getUser() {
		MeshAuthUser authUser = getMeshAuthUser();
		if (authUser == null) {
			return null;
		} else {
			return authUser.getDelegate();
		}
	}

	@Override
	public MeshAuthUser getMeshAuthUser() {
		if (user == null && rc.user() != null) {
			if (rc.user() instanceof MeshAuthUser) {
				user = (MeshAuthUser) rc.user();
			} else {
				log.error("Could not load user from routing context.");
				// TODO i18n
				throw new GenericRestException(INTERNAL_SERVER_ERROR, "Could not load request user");
			}
		}
		return user;
	}

	@Override
	public void setUser(MeshAuthUser user) {
		rc.setUser(user);
	}

	@Override
	public boolean isMigrationContext() {
		return false;
	}

	@Override
	public boolean isPurgeAllowed() {
		return true;
	}

	@Override
	public void setHttpServerConfig(HttpServerConfig config) {
		this.httpServerConfig = config;
	}
}
