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
	private static EventAwareCache<Object, Branch> branchCache = EventAwareCache.builder().size(500).events(MeshEvent.BRANCH_UPDATED).build();

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
		final Project selectedProject = project;
		return branchCache.get(project.id(), p -> {
			if (p == null) {
				// TODO i18n
				throw error(INTERNAL_SERVER_ERROR, "Cannot get branch without a project");
			}

			Branch branch = null;

			String branchNameOrUuid = getVersioningParameters().getBranch();
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
