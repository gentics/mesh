package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;

public interface AdminClientMethods {

	/**
	 * Return the mesh status.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> meshStatus();

	/**
	 * Return the current schema/microschema migration status.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> schemaMigrationStatus();

	/**
	 * Invoke a graph database backup.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeBackup();

	/**
	 * Invoke a graph database export.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeExport();

	/**
	 * Invoke a graph database restore.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeRestore();

	/**
	 * Invoke a graph database import.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeImport();

}
