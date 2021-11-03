package com.gentics.mesh.core.context;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * The registry allows to load and store domain specific elements in the context. The implementation of the registry contains the storage specific details and
 * knows what elements to fetch from the actual context in order to return the requested element.
 */
public interface ContextDataRegistry {

	/**
	 * Return the project that may be set when this action context is used for a project specific request (e.g.: /api/v2/dummy/nodes..)
	 * 
	 * @param ac
	 * @return Project or null if no project has been specified in the given context.
	 */
	HibProject getProject(InternalActionContext ac);

	/**
	 * Store the project information in the given context.
	 * 
	 * @param ac
	 * @param project
	 */
	void setProject(InternalActionContext ac, HibProject project);

	/**
	 * Return the branch that may be specified in this action context as query parameter. This method will fail, if no project is set, or if the specified
	 * branch does not exist for the project When no branch was specified (but a project was set), this will return the latest branch of the project.
	 * 
	 * @param ac
	 * @param project
	 *            project for overriding the project set in the action context
	 * @return branch
	 */
	HibBranch getBranch(InternalActionContext ac, HibProject project);
}
