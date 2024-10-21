package com.gentics.mesh.distributed;

import javax.inject.Inject;

import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.MasterElector;

import io.vertx.ext.web.RoutingContext;

/**
 * Cluster instance request delegator, stubbing the answers with non-cluster values.
 * 
 * @author plyhun
 *
 */
public class RequestDelegatorStub implements RequestDelegator {
	private final MasterElector coordinatorMasterElector;

	@Inject
	public RequestDelegatorStub(MasterElector coordinatorMasterElector) {
		this.coordinatorMasterElector = coordinatorMasterElector;
	}

	@Override
	public void handle(RoutingContext rc) {
		rc.next();
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public void redirectToMaster(RoutingContext routingContext) {
		throw new IllegalStateException("No redirection possible in a non-cluster environment");
	}

	@Override
	public boolean isMaster() {
		return coordinatorMasterElector.isMaster();
	}

}
