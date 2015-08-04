package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.util.VerticleHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

@Component
public class RoutingContextService {

	private static final Logger log = LoggerFactory.getLogger(RoutingContextService.class);

	@Autowired
	private FramedThreadedTransactionalGraph fg;

	@Autowired
	private MeshSpringConfiguration configuration;

	@Autowired
	private BootstrapInitializer boot;

	@Autowired
	private I18NService i18n;

	/**
	 * Check the permission and throw an invalid permission exception when no matching permission could be found.
	 */
	// TODO move this to MeshAuthUser class
	public void hasPermission(RoutingContext rc, MeshVertex node, Permission type, Handler<AsyncResult<Boolean>> resultHandler,
			Handler<AsyncResult<Boolean>> transactionCompletedHandler) throws InvalidPermissionException {
		MeshAuthUser requestUser = getUser(rc);
		requestUser.isAuthorised(node, type, handler -> {
			if (!handler.result()) {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", node.getUuid())));
				AsyncResult<Boolean> transactionCompletedFuture = Future.succeededFuture(true);
				transactionCompletedHandler.handle(transactionCompletedFuture);
			} else {
				resultHandler.handle(Future.succeededFuture(handler.result()));
				if (transactionCompletedHandler != null) {
					AsyncResult<Boolean> transactionCompletedFuture = Future.succeededFuture(true);
					transactionCompletedHandler.handle(transactionCompletedFuture);
				}
			}
		});
	}

}
