package com.gentics.mesh.core.data.action.impl;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.action.DAOActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * @see DAOActionContext
 */
public class DAOActionContextImpl implements DAOActionContext {

	private final Tx tx;
	private final HibProject project;
	private final HibBranch branch;
	private final Object parent;

	private InternalActionContext ac;

	public DAOActionContextImpl(Tx tx, InternalActionContext ac, Object parent) {
		this.tx = tx;
		this.ac = ac;
		this.project = tx.getProject(ac);
		if (project != null) {
			this.branch = tx.getBranch(ac, project);
		} else {
			this.branch = null;
		}
		this.parent = parent;
	}

	@Override
	public Tx tx() {
		return tx;
	}

	@Override
	public InternalActionContext ac() {
		return ac;
	}

	@Override
	public HibProject project() {
		return project;
	}

	@Override
	public <T> T parent() {
		return (T) parent;
	}

	@Override
	public HibBranch branch() {
		return branch;
	}
}
