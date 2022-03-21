package com.gentics.mesh.core.db;

import java.util.Optional;

import com.gentics.mesh.cache.CacheCollection;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.security.SecurityUtils;

/**
 * A {@link Tx} is an interface for autoclosable transactions.
 */
public interface Tx extends BaseTransaction, DaoCollection, CacheCollection, SecurityUtils {

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
	 * Return the current active transaction. A transaction should be the only place where this threadlocal is updated.
	 * 
	 * @return Currently active transaction
	 */
	static Tx get() {
		return Tx.threadLocalGraph.get();
	}

	/**
	 * An optional wrapper around {@link Tx#get()}.
	 * 
	 * @return
	 */
	static Optional<Tx> maybeGet() {
		return Optional.ofNullable(get());
	}

	/**
	 * Return the latest branch of the project.
	 *
	 * @param ac
	 * @return branch
	 */
	default HibBranch getBranch(InternalActionContext ac) {
		return getBranch(ac, null);
	}

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

	/**
	 * Return the project that may be set when this action context is used for a project specific request (e.g.: /api/v2/dummy/nodes..)
	 *
	 * @param ac
	 * @return Project or null if no project has been specified in the given context.
	 */
	HibProject getProject(InternalActionContext ac);

	/**
	 * Return the transaction-carried data.
	 * 
	 * @return
	 */
	TxData data();

	/**
	 * Create a new event queue batch for CUD operations.
	 * 
	 * @return
	 */
	EventQueueBatch createBatch();

	Binaries binaries();

	S3Binaries s3binaries();

	/**
	 * An automatic cast of a higher level TXx to its implementors.
	 * 
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default <T extends Tx> T unwrap() {
		return (T) this;
	}
}
