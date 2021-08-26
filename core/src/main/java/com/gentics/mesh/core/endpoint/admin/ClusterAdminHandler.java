package com.gentics.mesh.core.endpoint.admin;

import com.gentics.mesh.context.InternalActionContext;

public interface ClusterAdminHandler extends AdminHandler {

	/**
	 * Load the currently active cluster configuration.
	 * 
	 * @param ac
	 */
	void handleLoadClusterConfig(InternalActionContext ac);

	/**
	 * Update the OrientDB cluster configuration.
	 * 
	 * @param ac
	 */
	void handleUpdateClusterConfig(InternalActionContext ac);

	/**
	 * Load information on the currently elected coordination master.
	 * 
	 * @param ac
	 */
	void handleLoadCoordinationMaster(InternalActionContext ac);

	/**
	 * Manually set the elected master on the instance which runs this handler.
	 * 
	 * @param ac
	 */
	void handleSetCoordinationMaster(InternalActionContext ac);

	/**
	 * Return the currently set coordinator config.
	 * 
	 * @param ac
	 */
	void handleLoadCoordinationConfig(InternalActionContext ac);

	/**
	 * Update the coordination configuration.
	 * 
	 * @param ac
	 */
	void handleUpdateCoordinationConfig(InternalActionContext ac);
}
