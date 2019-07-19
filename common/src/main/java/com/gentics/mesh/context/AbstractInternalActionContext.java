package com.gentics.mesh.context;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.cache.EventAwareCache;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Abstract class for internal action context.
 */
public abstract class AbstractInternalActionContext extends AbstractActionContext implements InternalActionContext {

	/**
	 * Cache for project specific branches.
	 */
	public static final EventAwareCache<String, Branch> BRANCH_CACHE = EventAwareCache.<String, Branch>builder()
		.size(500)
		.events(MeshEvent.BRANCH_UPDATED)
		.build();

	/**
	 * Field which will store the body model.
	 */
	private Object bodyModel = null;

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

	@Override
	public Branch getBranch(Project project) {
		if (project == null) {
			project = getProject();
		}
		if (project == null) {
			// TODO i18n
			throw error(INTERNAL_SERVER_ERROR, "Cannot get branch without a project");
		}
		String branchNameOrUuid = getVersioningParameters().getBranch();
		final Project selectedProject = project;
		return BRANCH_CACHE.get(project.id() + "-" + branchNameOrUuid, key -> {
			Branch branch = null;

			if (!isEmpty(branchNameOrUuid)) {
				branch = selectedProject.getBranchRoot().findByUuid(branchNameOrUuid);
				if (branch == null) {
					branch = selectedProject.getBranchRoot().findByName(branchNameOrUuid);
				}
				if (branch == null) {
					throw error(BAD_REQUEST, "branch_error_not_found", branchNameOrUuid);
				}
			} else {
				branch = selectedProject.getLatestBranch();
			}

			return branch;
		});
	}

	@Override
	public void setBody(Object model) {
		this.bodyModel = model;
	}

	@Override
	public <T> T fromJson(Class<?> classOfT) throws GenericRestException {
		if (bodyModel != null) {
			return (T) bodyModel;
		}
		return super.fromJson(classOfT);
	}

}
