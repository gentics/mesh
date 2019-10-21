package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;

/**
 * Admin specific client methods
 */
public interface AdminClientMethods {

	/**
	 * Return the Gentics Mesh server status.
	 * 
	 * @return
	 * @deprecated Use {@link MonitoringRestClient#status()} instead.
	 */
	@Deprecated
	MeshRequest<MeshStatusResponse> meshStatus();

	/**
	 * Return the Gentics Mesh cluster status.
	 * 
	 * @return
	 * @deprecated Use {@link MonitoringRestClient#clusterStatus()} instead.
	 */
	@Deprecated
	MeshRequest<ClusterStatusResponse> clusterStatus();

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
	 * @deprecated Endpoint currently not supported
	 */
	@Deprecated
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
	 * @deprecated Endpoint currently not supported
	 */
	@Deprecated
	MeshRequest<GenericMessageResponse> invokeImport();

	/**
	 * Invoke a consistency check of the graph database.
	 * 
	 * @return
	 */
	MeshRequest<ConsistencyCheckResponse> checkConsistency();

	/**
	 * Invoke a consistency check and repair of the graph database.
	 * 
	 * @return
	 */
	MeshRequest<ConsistencyCheckResponse> repairConsistency();

	/**
	 * Gets zip file containing debug information.
	 * @return
	 */
	MeshRequest<MeshBinaryResponse> debugInfo(String... include);
}
