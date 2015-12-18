package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.query.impl.NodeRequestParameter.EXPANDALL_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.NodeRequestParameter.LANGUAGES_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.NodeRequestParameter.RESOLVE_LINKS_QUERY_PARAM_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalHttpActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.query.impl.RolePermissionParameter;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

public class InternalHttpActionContextImpl extends HttpActionContextImpl implements InternalHttpActionContext {

	private static final Logger log = LoggerFactory.getLogger(InternalHttpActionContextImpl.class);
	private Project project;

	private MeshAuthUser user;

	public InternalHttpActionContextImpl(RoutingContext rc) {
		super(rc);
	}

	@Override
	public Project getProject() {
		if (project == null) {
			RoutingContext rc = getRoutingContext();
			project = BootstrapInitializer.getBoot().meshRoot().getProjectRoot().findByName(getProjectName(rc));
		}
		return project;
	}

	protected String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	@Override
	public Database getDatabase() {
		return MeshSpringConfiguration.getInstance().database();
	}

	@Override
	public MeshAuthUser getUser() {
		RoutingContext rc = getRoutingContext();
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
	public List<String> getSelectedLanguageTags() {
		List<String> languageTags = new ArrayList<>();
		Map<String, String> queryPairs = splitQuery();
		if (queryPairs == null) {
			return new ArrayList<>();
		}
		String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
		if (value != null) {
			languageTags = new ArrayList<>(Arrays.asList(value.split(",")));
		}
		languageTags.add(Mesh.mesh().getOptions().getDefaultLanguage());
		return languageTags;
	}

	@Override
	public boolean getExpandAllFlag() {
		Map<String, String> queryPairs = splitQuery();
		if (queryPairs == null) {
			return false;
		}
		String value = queryPairs.get(EXPANDALL_QUERY_PARAM_KEY);
		if (value != null) {
			return Boolean.valueOf(value);
		}
		return false;
	}

	@Override
	public WebRootLinkReplacer.Type getResolveLinksType() {
		Map<String, String> queryPairs = splitQuery();
		if (queryPairs == null) {
			return WebRootLinkReplacer.Type.OFF;
		}
		String value = queryPairs.get(RESOLVE_LINKS_QUERY_PARAM_KEY);
		if (value != null) {
			return WebRootLinkReplacer.Type.valueOf(value.toUpperCase());
		}
		return WebRootLinkReplacer.Type.OFF;
	}

	@Override
	public String getRolePermissionParameter() {
		Map<String, String> queryPairs = splitQuery();
		if (queryPairs == null) {
			return null;
		}
		return queryPairs.get(RolePermissionParameter.ROLE_PERMISSION_QUERY_PARAM_KEY);
	}

	@Override
	public void setUser(User user) {
		getRoutingContext().setUser(user);
	}

	@Override
	public <T> Handler<AsyncResult<T>> errorHandler() {
		Handler<AsyncResult<T>> handler = t -> {
			if (t.failed()) {
				fail(t.cause());
			}
		};
		return handler;
	}

	@Override
	public PagingParameter getPagingParameter() {
		String page = getParameter(PagingParameter.PAGE_PARAMETER_KEY);
		String perPage = getParameter(PagingParameter.PER_PAGE_PARAMETER_KEY);
		int pageInt = 1;
		int perPageInt = MeshOptions.DEFAULT_PAGE_SIZE;
		if (page != null) {
			pageInt = NumberUtils.toInt(page, 1);
		}
		if (perPage != null) {
			perPageInt = NumberUtils.toInt(perPage, MeshOptions.DEFAULT_PAGE_SIZE);
		}
		if (pageInt < 1) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
		if (perPageInt < 0) {
			error(BAD_REQUEST, "error_invalid_paging_parameters");
		}
		return new PagingParameter(pageInt, perPageInt);
	}

	@Override
	public void sendMessage(HttpResponseStatus status, String i18nMessage, String... i18nParameters) {
		send(JsonUtil.toJson(new GenericMessageResponse(i18n(i18nMessage, i18nParameters))), status);
	}

	@Override
	public void respond(RestModel restModel, HttpResponseStatus status) {
		send(JsonUtil.toJson(restModel), status);
	}

	@Override
	public ImageManipulationParameter getImageRequestParameter() {
		//TODO return immutable object
		return ImageManipulationParameter.fromQuery(query());
	}

}
