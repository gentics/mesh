package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.handler.InternalActionContext;

public abstract class AbstractCrudHandler extends AbstractHandler {

	/**
	 * Handle create requests.
	 * 
	 * @param ac
	 */
	abstract public void handleCreate(InternalActionContext ac);

	/**
	 * Handle delete requests.
	 * 
	 * @param ac
	 */
	abstract public void handleDelete(InternalActionContext ac);

	/**
	 * Handle update requests.
	 * 
	 * @param ac
	 */
	abstract public void handleUpdate(InternalActionContext ac);

	/**
	 * Handle read requests that target a single object.
	 * 
	 * @param ac
	 */
	abstract public void handleRead(InternalActionContext ac);

	/**
	 * Handle read list requests.
	 * 
	 * @param ac
	 */
	abstract public void handleReadList(InternalActionContext ac);

}
