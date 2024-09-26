package com.gentics.mesh.core.endpoint.admin.consistency;

import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Transactional;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.parameter.ConsistencyCheckParameters;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private final BootstrapInitializer boot;

	@Inject
	public ConsistencyCheckHandler(Vertx vertx, Database db, HandlerUtilities utils, List<ConsistencyCheck> checks, BootstrapInitializer boot) {
		this.vertx = vertx;
		this.db = db;
		this.utils = utils;
		this.checks = checks;
		this.boot = boot;
	}

	/**
	 * Invoke the consistency check.
	 * 
	 * @param ac
	 *            Action context
	 */
	public void invokeCheck(InternalActionContext ac) {
		ConsistencyCheckParameters param = ac.getConsistencyCheckParameters();
		invokeAction(ac, false, param.isAsync());
	}

	/**
	 * Invoke the consistency check and repair
	 * 
	 * @param ac
	 *            Action Context
	 */
	public void invokeRepair(InternalActionContext ac) {
		ConsistencyCheckParameters param = ac.getConsistencyCheckParameters();
		invokeAction(ac, true, param.isAsync());
	}

	private void invokeAction(InternalActionContext ac, boolean attemptRepair, boolean async) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			if (async) {
				tx.jobDao().enqueueConsistencyCheck(ac.getUser(), attemptRepair);
				return new ConsistencyCheckResponse();
			} else {
				return checkConsistency(attemptRepair, false).runInExistingTx(tx);
			}
		}, model -> {
			if (async) {
				MeshEvent.triggerJobWorker(boot.mesh());
			}
			ac.send(model, OK);
		});
	}

	/**
	 * Invoke the consistency check and (optional attempt a repair operation)
	 * 
	 * @param attemptRepair
	 *            Whether to invoke a repair operation
	 * @param async whether the checks are executed in a job
	 * @return Transactional for the operation which will return the check result
	 */
	public Transactional<ConsistencyCheckResponse> checkConsistency(boolean attemptRepair, boolean async) {
		return db.transactional(tx -> {
			log.info("Consistency check has been invoked. Repair: " + attemptRepair);
			vertx.eventBus().publish(REPAIR_START.address, null);
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			// Check domain model
			for (ConsistencyCheck check : checks) {
				if (!async && check.asyncOnly()) {
					log.info("Ignoring {" + check.getName() + "} check, which is an 'async only' check.");
					continue;
				}
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
