package com.gentics.mesh.core.endpoint.admin;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.MeshServerInfoModel;

public interface AdminHandler {

	/**
	 * Handle the mesh status request.
	 * 
	 * @param ac
	 */
	void handleMeshStatus(InternalActionContext ac);

	/**
	 * Invoke a database backup call to the current graph database provider.
	 * 
	 * @param ac
	 */
	void handleBackup(InternalActionContext ac);

	/**
	 * Invoke the graph database backup.
	 * 
	 * @return
	 */
	String backup();

	/**
	 * Handle graph restore action.
	 * 
	 * @param ac
	 */
	void handleRestore(InternalActionContext ac);

	/**
	 * Handle graph export action.
	 * 
	 * @param ac
	 */
	void handleExport(InternalActionContext ac);

	/**
	 * Handle graph import action.
	 * 
	 * @param ac
	 */
	void handleImport(InternalActionContext ac);

	/**
	 * Load cluster status information.
	 * 
	 * @param ac
	 */
	void handleClusterStatus(InternalActionContext ac);

	/**
	 * Load the mesh server version information.
	 * 
	 * @param ac
	 */
	void handleVersions(InternalActionContext ac);

	/**
	 * Populate the mesh server version info.
	 * 
	 * @param ac
	 * @return
	 */
	MeshServerInfoModel getMeshServerInfoModel(InternalActionContext ac);

	/**
	 * Generate and return the RAML of the server.
	 * 
	 * @param ac
	 */
	void handleRAML(InternalActionContext ac);
}