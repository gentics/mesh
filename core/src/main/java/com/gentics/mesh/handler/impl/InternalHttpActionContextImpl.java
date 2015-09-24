package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.rest.node.NodeRequestParameters.LANGUAGES_QUERY_PARAM_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.InternalHttpActionContext;

import io.vertx.core.MultiMap;
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
	public void setUser(User user) {
		getRoutingContext().setUser(user);
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
