package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphdb.OrientDBDatabase;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class DatabaseAdminHandler {

	private static final Logger log = LoggerFactory.getLogger(DatabaseAdminHandler.class);

	private final OrientDBDatabase db;

	@Inject
	public DatabaseAdminHandler(OrientDBDatabase db) {
		this.db = db;
	}

	/**
	 * Stop the database of the running Mesh instance, without stopping Mesh itself
	 * 
	 * @param rc
	 */
	public void handleDatabaseStop(RoutingContext rc) {
		try {
			if (db.isRunning()) {
				db.shutdown();
				rc.response().setStatusCode(OK.code()).end();
			} else {
				rc.response().setStatusCode(BAD_REQUEST.code()).end();
			}
		} catch (Exception e) {
			log.error("Database stop failed", e);
			rc.fail(error(INTERNAL_SERVER_ERROR, "error_internal"));
		}
	}

	/**
	 * Start the database of the running Mesh instance
	 * 
	 * @param rc
	 */
	public void handleDatabaseStart(RoutingContext rc) {
		try {
			if (!db.isRunning()) {
				db.setupConnectionPool();
				rc.response().setStatusCode(OK.code()).end();
			} else {
				rc.response().setStatusCode(BAD_REQUEST.code()).end();
			}
		} catch (Exception e) {
			log.error("Database start failed", e);
			rc.fail(error(INTERNAL_SERVER_ERROR, "error_internal"));
		}
	}
}
