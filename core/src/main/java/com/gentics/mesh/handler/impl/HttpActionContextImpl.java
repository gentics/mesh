package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.HttpActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class HttpActionContextImpl extends AbstractActionContext implements HttpActionContext {

	private static final Logger log = LoggerFactory.getLogger(HttpActionContextImpl.class);

	private static final String LOCALE_MAP_DATA_KEY = "locale";
	private Project project;

	private MeshAuthUser user;

	private RoutingContext rc;

	public HttpActionContextImpl(RoutingContext rc) {
		this.rc = rc;

	}

	private String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	@Override
	public String getParameter(String parameterName) {
		return rc.request().getParam(parameterName);
	}

	@Override
	public Project getProject() {
		if (project == null) {
			project = BootstrapInitializer.getBoot().meshRoot().getProjectRoot().findByName(getProjectName(rc));
		}
		return project;
	}

	@Override
	public MeshAuthUser getUser() {
		if (user == null && rc.user() != null) {
			if (rc.user() instanceof MeshAuthUser) {
				user = (MeshAuthUser) rc.user();
			} else {
				log.error("Could not load user from routing context.");
				// TODO i18n
				throw new HttpStatusCodeErrorException(INTERNAL_SERVER_ERROR, "Could not load request user");
			}
		}
		return user;
	}

	@Override
	public void setUser(User user) {
		rc.setUser(user);
	}

	@Override
	public String i18n(String i18nKey, String... parameters) {
		return I18NService.getI18n().get(this, i18nKey, parameters);
	}

	@Override
	public <T> AsyncResult<T> failedFuture(HttpResponseStatus status, String i18nMessage, Throwable cause) {
		return Future.failedFuture(new HttpStatusCodeErrorException(status, i18n(i18nMessage), cause));
	}

	@Override
	public <T> AsyncResult<T> failedFuture(HttpResponseStatus status, String i18nKey, String... parameters) {
		return Future.failedFuture(new HttpStatusCodeErrorException(status, i18n(i18nKey, parameters)));
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

	@Override
	public PagingInfo getPagingInfo() {
		MultiMap params = getParameters();
		int page = 1;
		int perPage = MeshOptions.DEFAULT_PAGE_SIZE;
		if (params != null) {
			page = NumberUtils.toInt(params.get("page"), 1);
			perPage = NumberUtils.toInt(params.get("perPage"), MeshOptions.DEFAULT_PAGE_SIZE);
		}
		if (page < 1) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, i18n("error_invalid_paging_parameters"));
		}
		if (perPage <= 0) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, i18n("error_invalid_paging_parameters"));
		}
		return new PagingInfo(page, perPage);
	}

}
