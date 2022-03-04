package com.gentics.mesh.distributed;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import javax.inject.Inject;

import com.gentics.mesh.core.db.cluster.ClusterManager;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * Implementation of {@link TopologyChangeReadonlyHandler} that will let mutating requests fail, if the cluster topology is changing
 */
public class TopologyChangeReadonlyHandlerImpl implements TopologyChangeReadonlyHandler {
	private final ClusterManager clusterManager;

	@Inject
	public TopologyChangeReadonlyHandlerImpl(ClusterManager clusterManager) {
		this.clusterManager = clusterManager;
	}

	@Override
	public void handle(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		String path = request.path();
		HttpMethod method = request.method();

		if (!DistributionUtils.isReadRequest(method, path) && clusterManager != null
				&& clusterManager.isClusterTopologyLocked()) {
			rc.fail(error(SERVICE_UNAVAILABLE, "error_cluster_topology_readonly").setLogStackTrace(false));
		} else {
			rc.next();
		}
	}
}
