package com.gentics.mesh.handler.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.query.impl.NodeRequestParameter.EXPANDALL_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.NodeRequestParameter.LANGUAGES_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.NodeRequestParameter.RESOLVE_LINKS_QUERY_PARAM_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.VersionNumber;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalHttpActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.NavigationRequestParameter;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.query.impl.RolePermissionParameter;
import com.tinkerpop.blueprints.Vertex;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * @see InternalHttpActionContext
 */
public class InternalHttpActionContextImpl extends HttpActionContextImpl implements InternalHttpActionContext {

	private static final Logger log = LoggerFactory.getLogger(InternalHttpActionContextImpl.class);

	private Project project;

	private MeshAuthUser user;

	private List<String> languageTags;

	private String version;

	public InternalHttpActionContextImpl(RoutingContext rc) {
		super(rc);
	}

	@Override
	public Project getProject() {
		if (project == null) {
			RoutingContext rc = getRoutingContext();
			project = BootstrapInitializer.getBoot().meshRoot().getProjectRoot().findByName(getProjectName(rc)).toBlocking().single();
		}
		return project;
	}

	protected String getProjectName(RoutingContext rc) {
		return rc.get(RouterStorage.PROJECT_CONTEXT_KEY);
	}

	@Override
	public Release getRelease() {
		Project project = getProject();
		if (project == null) {
			throw error(INTERNAL_SERVER_ERROR, "Cannot get release without a project");
		}

		Release release = null;

		String releaseNameOrUuid = getParameter(NodeRequestParameter.RELEASE_QUERY_PARAM_KEY);
		if (!isEmpty(releaseNameOrUuid)) {
			release = project.getReleaseRoot().findByUuid(releaseNameOrUuid).toBlocking().single();
			if (release == null) {
				release = project.getReleaseRoot().findByName(releaseNameOrUuid).toBlocking().single();
			}
			if (release == null) {
				throw error(BAD_REQUEST, "error_release_not_found", releaseNameOrUuid);
			}
		} else {
			release = project.getLatestRelease();
		}

		return release;
	}

	@Override
	public String getVersion() {
		if (version == null) {
			String versionParameter = getParameter(NodeRequestParameter.VERSION_QUERY_PARAM_KEY);
			if (versionParameter != null) {
				if ("draft".equalsIgnoreCase(versionParameter) || "published".equalsIgnoreCase(versionParameter)) {
					version = versionParameter;
				} else {
					try {
						version = new VersionNumber(versionParameter).toString();
					} catch (IllegalArgumentException e) {
						throw error(BAD_REQUEST, "error_illegal_version", versionParameter);
					}
				}
			} else {
				version = "published";
			}
		}
		return version;
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
		if (languageTags == null) {
			languageTags = new ArrayList<>();
			Map<String, String> queryPairs = splitQuery();
			if (queryPairs == null) {
				return new ArrayList<>();
			}
			String value = queryPairs.get(LANGUAGES_QUERY_PARAM_KEY);
			if (value != null) {
				languageTags = new ArrayList<>(Arrays.asList(value.split(",")));
			}
			if (languageTags.isEmpty()) {
				languageTags.add(Mesh.mesh().getOptions().getDefaultLanguage());
			}

			// check whether given language tags exist
			Database db = MeshSpringConfiguration.getInstance().database();
			try (NoTrx noTrx = db.noTrx()) {
				for (String languageTag : languageTags) {
					if (languageTag != null) {
						Iterator<Vertex> it = db.getVertices(LanguageImpl.class, new String[] { LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY },
								new Object[] { languageTag });
						if (!it.hasNext()) {
							throw error(BAD_REQUEST, "error_language_not_found", languageTag);
						}
					}
				}
			}
		}

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
		// TODO return immutable object
		return PagingParameter.fromQuery(query());
	}

	@Override
	public NavigationRequestParameter getNavigationRequestParameter() {
		// TODO return immutable object
		return NavigationRequestParameter.fromQuery(query());
	}

	@Override
	public void respond(RestModel restModel, HttpResponseStatus status) {
		send(JsonUtil.toJson(restModel), status);
	}

	@Override
	public ImageManipulationParameter getImageRequestParameter() {
		// TODO return immutable object
		return ImageManipulationParameter.fromQuery(query());
	}

	@Override
	public void addCookie(Cookie cookie) {
		getRoutingContext().addCookie(cookie);
	}
}
