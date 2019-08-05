package com.gentics.mesh.core.endpoint.admin.consistency;

import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_START;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.consistency.check.BinaryCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.BranchCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.FieldCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.GraphFieldContainerCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.GroupCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.MicronodeCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.MicroschemaContainerCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.NodeCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.ProjectCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.RoleCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.SchemaContainerCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.TagCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.TagFamilyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.UserCheck;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler which is used to process actions for the consistency check.
 */
@Singleton
public class ConsistencyCheckHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(ConsistencyCheckHandler.class);

	private Database db;

	private HandlerUtilities utils;

	private Vertx vertx;

	private static List<ConsistencyCheck> checks = Arrays.asList(
		new GroupCheck(),
		new MicroschemaContainerCheck(),
		new NodeCheck(),
		new ProjectCheck(),
		new BranchCheck(),
		new RoleCheck(),
		new SchemaContainerCheck(),
		new TagCheck(),
		new TagFamilyCheck(),
		new UserCheck(),
		new GraphFieldContainerCheck(),
		new MicronodeCheck(),
		new BinaryCheck(),
		new FieldCheck());

	/**
	 * Get the list of checks
	 * 
	 * @return list of checks
	 */
	public static List<ConsistencyCheck> getChecks() {
		return checks;
	}

	@Inject
	public ConsistencyCheckHandler(Vertx vertx, Database db, HandlerUtilities utils) {
		this.vertx = vertx;
		this.db = db;
		this.utils = utils;
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
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
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
		}, model -> ac.send(model, OK));
	}

}
