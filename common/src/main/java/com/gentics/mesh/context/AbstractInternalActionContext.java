package com.gentics.mesh.context;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * Abstract class for internal action context.
 */
public abstract class AbstractInternalActionContext extends AbstractActionContext implements InternalActionContext {

	private boolean skipWriteLock = false;

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
		return project.findBranch(branchNameOrUuid);
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

	@Override
	public boolean isSkipWriteLock() {
		return skipWriteLock;
	}

	@Override
	public InternalActionContext skipWriteLock() {
		this.skipWriteLock =true;
		return this;
	}


}
