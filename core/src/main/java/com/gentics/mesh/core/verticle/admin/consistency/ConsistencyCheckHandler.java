package com.gentics.mesh.core.verticle.admin.consistency;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.GroupCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.MicroschemaContainerCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.NodeCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.ProjectCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.ReleaseCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.RoleCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.SchemaContainerCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.TagCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.TagFamilyCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.UserCheck;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler which is used to process actions for the consistency check.
 */
@Singleton
public class ConsistencyCheckHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(ConsistencyCheckHandler.class);

	private Database db;

	private static List<ConsistencyCheck> checks = Arrays.asList(new GroupCheck(), new MicroschemaContainerCheck(), new NodeCheck(),
			new ProjectCheck(), new ReleaseCheck(), new RoleCheck(), new SchemaContainerCheck(), new TagCheck(), new TagFamilyCheck(),
			new UserCheck());

	/**
	 * Get the list of checks
	 * 
	 * @return list of checks
	 */
	public static List<ConsistencyCheck> getChecks() {
		return checks;
	}

	@Inject
	public ConsistencyCheckHandler(Database db) {
		this.db = db;
	}

	/**
	 * Invoke the consistency check. 
	 * @param ac
	 */
	public void invokeCheck(InternalActionContext ac) {
		db.asyncTx((tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			log.info("Consistency check has been invoked.");
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			// Check domain model
			for (ConsistencyCheck check : checks) {
				check.invoke(db, response);
			}
			return Single.just(response);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

}
