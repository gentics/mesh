package com.gentics.mesh.context;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.common.RestModel;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Abstract class for internal action context.
 */
public abstract class AbstractInternalActionContext extends AbstractActionContext implements InternalActionContext {

	@Override
	public void send(RestModel restModel, HttpResponseStatus status) {
		send(restModel.toJson(), status);
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

	/**
	 * Cache for project specific releases.
	 */
	private Map<Project, Release> releaseCache = new ConcurrentHashMap<>();

	@Override
	public Release getRelease(Project project) {
		if (project == null) {
			project = getProject();
		}
		return releaseCache.computeIfAbsent(project, p -> {
			if (p == null) {
				// TODO i18n
				throw error(INTERNAL_SERVER_ERROR, "Cannot get release without a project");
			}

			Release release = null;

			String releaseNameOrUuid = getVersioningParameters().getRelease();
			if (!isEmpty(releaseNameOrUuid)) {
				release = p.getReleaseRoot().findByUuid(releaseNameOrUuid);
				if (release == null) {
					release = p.getReleaseRoot().findByName(releaseNameOrUuid);
				}
				if (release == null) {
					throw error(BAD_REQUEST, "release_error_not_found", releaseNameOrUuid);
				}
			} else {
				release = p.getLatestRelease();
			}

			return release;
		});
	}

}
