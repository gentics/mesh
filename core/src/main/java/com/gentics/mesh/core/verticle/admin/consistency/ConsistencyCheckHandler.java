package com.gentics.mesh.core.verticle.admin.consistency;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.ProjectCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.UserCheck;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

@Singleton
public class ConsistencyCheckHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(ConsistencyCheckHandler.class);

	private Database db;

	private List<ConsistencyCheck> checks = new ArrayList<>();

	private BootstrapInitializer boot;

	@Inject
	public ConsistencyCheckHandler(Database db, BootstrapInitializer boot) {
		this.db = db;
		this.boot = boot;
		checks.add(new ProjectCheck());
		checks.add(new UserCheck());
	}

	public void invokeCheck(InternalActionContext ac) {
		db.asyncTx((tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			log.info("Consistency check has been invoked.");
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			// Check domain model
			for (ConsistencyCheck check : checks) {
				check.invoke(boot, response);
			}
			// Check raw graph
			for(Vertex vertex : tx.getGraph().getVertices()) {
				// TODO check for dangling vertices
			}
			return Single.just(response);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

}
