package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.monitor.liveness.LivenessManager;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class DatabaseAdminHandler {

	private static final Logger log = LoggerFactory.getLogger(DatabaseAdminHandler.class);

	private final LivenessManager liveness;
	private final OrientDBDatabase db;

	@Inject
	public DatabaseAdminHandler(OrientDBDatabase db, LivenessManager liveness) {
		this.db = db;
		this.liveness = liveness;
	}

	/**
	 * Stop the database of the running Mesh instance, without stopping Mesh itself
	 * 
	 * @param ac
	 */
	public void handleDatabaseStop(InternalActionContext ac) {
		try {
			if (db.isRunning()) {
				db.shutdown();
				liveness.setLive(false, "DB turned off manually");
				ac.send(OK);
			} else {
				ac.send(BAD_REQUEST);
			}
		} catch (Exception e) {
			log.error("Database stop failed", e);
			ac.fail(error(INTERNAL_SERVER_ERROR, "error_internal"));
		}
	}

	/**
	 * Start the database of the running Mesh instance
	 * 
	 * @param rc
	 */
	public void handleDatabaseStart(InternalActionContext ac) {
		try {
			if (!db.isRunning()) {
				db.setupConnectionPool();
				liveness.setLive(true, "DB turned on, after manual off");
				ac.send(OK);
			} else {
				ac.send(BAD_REQUEST);
			}
		} catch (Exception e) {
			log.error("Database start failed", e);
			ac.fail(error(INTERNAL_SERVER_ERROR, "error_internal"));
		}
	}
}
