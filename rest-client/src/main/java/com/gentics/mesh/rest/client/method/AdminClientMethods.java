package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.parameter.BackupParameters;
import com.gentics.mesh.parameter.ParameterProvider;
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
	 * Invoke a graph database backup.
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeBackup(BackupParameters parameters);

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
	 * @param parameters optional parameters
	 * @return
	 */
	MeshRequest<ConsistencyCheckResponse> checkConsistency(ParameterProvider... parameters);

	/**
	 * Invoke a consistency check and repair of the graph database.
	 * 
	 * @param parameters optional parameters
	 * @return
	 */
	MeshRequest<ConsistencyCheckResponse> repairConsistency(ParameterProvider... parameters);

	/**
	 * Gets zip file containing debug information.
	 * @return
	 */
	MeshRequest<MeshBinaryResponse> debugInfo(String... include);

	/**
	 * Returns information on the elected coordinator master.
	 * @return
	 */
	MeshRequest<CoordinatorMasterResponse> loadCoordinationMaster();

	/**
	 * Make this instance the coordination master.
	 * @return
	 */
	MeshRequest<GenericMessageResponse> setCoordinationMaster();

	/**
	 * Clear the caches (cluster wide)
	 * @return
	 */
	MeshRequest<GenericMessageResponse> clearCache();
}
