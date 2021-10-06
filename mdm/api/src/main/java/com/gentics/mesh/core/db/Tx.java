package com.gentics.mesh.core.db;

import com.gentics.mesh.cache.CacheCollection;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.security.SecurityUtils;

/**
 * A {@link Tx} is an interface for autoclosable transactions.
 */
public interface Tx extends BaseTransaction, DaoCollection, CacheCollection, SecurityUtils, TxEntityPersistenceManager {

	/**
	 * Thread local that is used to store references to the used graph.
	 */
	static ThreadLocal<Tx> threadLocalGraph = new ThreadLocal<>();

	/**
	 * Set the nested active transaction for the current thread.
	 * 
	 * @param tx
	 *            Transaction
	 */
	static void setActive(Tx tx) {
		Tx.threadLocalGraph.set(tx);
	}

	/**
	 * @deprecated Use {@link #get()} instead.
	 * @return
	 */
	@Deprecated
	static Tx getActive() {
		return Tx.threadLocalGraph.get();
	}

	/**
	 * Return the current active graph. A transaction should be the only place where this threadlocal is updated.
	 * 
	 * @return Currently active transaction
	 */
	static Tx get() {
		return getActive();
	}

	//
	// /**
	// * Mark the transaction as succeeded. The autoclosable will invoke a commit when completing.
	// */
	// void success();
	//
	// /**
	// * Mark the transaction as failed. The autoclosable will invoke a rollback when completing.
	// */
	// void failure();
	//
	//
	// /**
	// * Invoke rollback or commit when closing the autoclosable. By default a rollback will be invoked.
	// */
	// @Override
	// void close();

	TxData data();

	HibBranch getBranch(InternalActionContext ac);

	HibBranch getBranch(InternalActionContext ac, HibProject project);

	HibProject getProject(InternalActionContext ac);
}
