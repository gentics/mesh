package com.gentics.mesh.core.endpoint.handler;

import com.gentics.mesh.context.InternalActionContext;

public interface CrudHandler {

	/**
	 * Handle create requests.
	 * 
	 * @param ac
	 */
	void handleCreate(InternalActionContext ac);

	/**
	 * Handle delete requests.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 */
	void handleDelete(InternalActionContext ac, String uuid);

	/**
	 * Handle read requests that target a single object.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be read
	 */
	void handleRead(InternalActionContext ac, String uuid);

	/**
	 * Handle update requests.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 */
	void handleUpdate(InternalActionContext ac, String uuid);

	/**
	 * Handle read list requests.
	 * 
	 * @param ac
	 */
	void handleReadList(InternalActionContext ac);

}
