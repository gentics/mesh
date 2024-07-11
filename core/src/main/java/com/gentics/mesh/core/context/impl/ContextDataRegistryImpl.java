package com.gentics.mesh.core.context.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.context.ContextDataRegistry;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.shared.SharedKeys;

/**
 * @see ContextDataRegistry
 */
@Singleton
public class ContextDataRegistryImpl implements ContextDataRegistry {

	@Inject
	public ContextDataRegistryImpl() {

	}

	@Override
	public Project getProject(InternalActionContext ac) {
		if (ac instanceof NodeMigrationActionContext) {
			return ((NodeMigrationActionContext) ac).getProject();
		}
		return ac.get(SharedKeys.PROJECT_CONTEXT_KEY);
	}

	@Override
	public void setProject(InternalActionContext ac, Project project) {
		ac.put(SharedKeys.PROJECT_CONTEXT_KEY, project);
	}

	@Override
	public Branch getBranch(InternalActionContext ac, Project project) {
		if (ac instanceof NodeMigrationActionContext) {
			return ((NodeMigrationActionContext) ac).getBranch();
		}
		if (project == null) {
			project = getProject(ac);
		}
		if (project == null) {
			// TODO i18n
			throw error(INTERNAL_SERVER_ERROR, "Cannot get branch without a project");
		}
		String branchNameOrUuid = ac.getVersioningParameters().getBranch();
		return Tx.get().branchDao().findBranch(project, branchNameOrUuid);
	}

}
