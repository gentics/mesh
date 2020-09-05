package com.gentics.mesh.core.endpoint.admin.consistency;

import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.Transactional;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler which is used to process actions for the consistency check.
 */
@Singleton
public class ConsistencyCheckHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(ConsistencyCheckHandler.class);

	private final List<ConsistencyCheck> checks;
	private final Database db;
	private final HandlerUtilities utils;
	private final Vertx vertx;

	@Inject
	public ConsistencyCheckHandler(Vertx vertx, Database db, HandlerUtilities utils, List<ConsistencyCheck> checks) {
		this.vertx = vertx;
		this.db = db;
		this.utils = utils;
		this.checks = checks;
	}

	/**
	 * Invoke the consistency check.
	 * 
	 * @param ac
	 *            Action context
	 */
	public void invokeCheck(InternalActionContext ac) {
		invokeAction(ac, false);
	}

	/**
	 * Invoke the consistency check and repair
	 * 
	 * @param ac
	 *            Action Context
	 */
	public void invokeRepair(InternalActionContext ac) {
		invokeAction(ac, true);
	}

	private void invokeAction(InternalActionContext ac, boolean attemptRepair) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			return checkConsistency(attemptRepair).runInExistingTx(tx);
		}, model -> ac.send(model, OK));
	}

	public Transactional<ConsistencyCheckResponse> checkConsistency(boolean attemptRepair) {
		return db.transactional(tx -> {
			log.info("Consistency check has been invoked. Repair: " + attemptRepair);
			vertx.eventBus().publish(REPAIR_START.address, null);
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			// Check domain model
			for (ConsistencyCheck check : checks) {
				log.info("Invoking {" + check.getName() + "} check.");
				ConsistencyCheckResult result = check.invoke(db, tx, attemptRepair);
				log.info("Check {" + check.getName() + "} completed.");
				if (attemptRepair) {
					log.info("Check {" + check.getName() + "} repaired {" + result.getRepairCount() + "} elements.");
				}
				response.getInconsistencies().addAll(result.getResults());
				response.getRepairCount().put(check.getName(), result.getRepairCount());
			}
			vertx.eventBus().publish(REPAIR_FINISHED.address, null);
			return response;
		});
	}

}
