package com.gentics.mesh.context;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.query.impl.NodeRequestParameter.EXPANDALL_QUERY_PARAM_KEY;
import static com.gentics.mesh.query.impl.NodeRequestParameter.EXPANDFIELDS_QUERY_PARAM_KEY;
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
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.VersionNumber;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
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

public abstract class AbstractInternalActionContext extends AbstractActionContext implements InternalActionContext {

	private List<String> languageTags;
	private String version;

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @return List of languages. List can be empty.
	 */
	@Override
	public List<String> getExpandedFieldnames() {
		data().computeIfAbsent(EXPANDED_FIELDNAMED_DATA_KEY, map -> {
			List<String> expandFieldnames = new ArrayList<>();
			Map<String, String> queryPairs = splitQuery();
			if (queryPairs == null) {
				return new ArrayList<>();
			}

			String value = queryPairs.get(EXPANDFIELDS_QUERY_PARAM_KEY);
			if (value != null) {
				expandFieldnames = new ArrayList<>(Arrays.asList(value.split(",")));
			}
			return expandFieldnames;
		});
		List<String> fieldList = (List<String>) data().get(EXPANDED_FIELDNAMED_DATA_KEY);
		return fieldList == null ? new ArrayList<>() : fieldList;
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
	public void respond(RestModel restModel, HttpResponseStatus status) {
		send(JsonUtil.toJson(restModel), status);
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
	public ImageManipulationParameter getImageRequestParameter() {
		// TODO return immutable object
		return ImageManipulationParameter.fromQuery(query());
	}

	@Override
	public Database getDatabase() {
		return MeshSpringConfiguration.getInstance().database();
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
	public Release getRelease(Project project) {
		if (project == null) {
			project = getProject();
		}
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
	public InternalActionContext setVersion(String version) {
		this.version = version;
		return this;
	}

}
