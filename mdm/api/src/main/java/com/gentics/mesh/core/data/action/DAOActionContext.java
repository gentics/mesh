package com.gentics.mesh.core.data.action;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.action.impl.DAOActionContextImpl;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * Context for a DAO action operation. The context contains information about the scope of the operation.
 */
public interface DAOActionContext {

	static <T> DAOActionContext context(Tx tx, InternalActionContext ac, T parent) {
		return new DAOActionContextImpl(tx, ac, parent);
	}

	static <T> DAOActionContext context(Tx tx, InternalActionContext ac) {
		return new DAOActionContextImpl(tx, ac, null);
	}

	/**
	 * Transaction to be used.
	 * 
	 * @return
	 */
	Tx tx();

	/**
	 * 
	 * @return
	 */
	HibProject project();

	/**
	 * Parent element for the operation (e.g. Tag Family or Branch for tag load requests).
	 * 
	 * @param <T>
	 * @return
	 */
	<T> T parent();

	/**
	 * Return the branch for the action.
	 * 
	 * @return
	 */
	HibBranch branch();

	/**
	 * Return the internal action context.
	 * 
	 * @return
	 */
	InternalActionContext ac();

}
