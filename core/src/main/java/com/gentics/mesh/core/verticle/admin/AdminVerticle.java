package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;
import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The admin verticle provides core administration rest endpoints.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminVerticle.class);

	@Autowired
	private AdminHandler handler;

	public AdminVerticle() {
		super("admin");
	}

	@Override
	public void registerEndPoints() throws Exception {

		addStatusHandler();
		addMigrationStatusHandler();

		// TODO secure handlers below
		addBackupHandler();
		addRestoreHandler();
		addImportHandler();
		addExportHandler();
		// addVerticleHandler();
		// addServiceHandler();

	}

	private void addMigrationStatusHandler() {
		route("/migrationStatus").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			handler.handleMigrationStatus(InternalActionContext.create(rc));
		});
	}

	private void addExportHandler() {
		route("/export").method(GET).handler(rc -> {
			handler.handleExport(rc);
		});
	}

	private void addImportHandler() {
		route("/import").method(GET).handler(rc -> {
			handler.handleImport(rc);
		});
	}

	private void addRestoreHandler() {
		route("/restore").method(GET).handler(rc -> {
			handler.handleRestore(rc);
		});
	}

	private void addBackupHandler() {
		route("/backup").method(GET).handler(rc -> {
			handler.handleBackup(rc);
		});
	}

	/**
	 * Handler that reacts onto status requests.
	 */
	private void addStatusHandler() {
		route("/status").method(GET).handler(rc -> {
			handler.handleStatus(rc);
		});

	}

	// private void addServiceHandler() {
	// route("/deployService/:mavenCoordinates").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Deploy " + rc.request().params().get("mavenCoordinates"));
	// });
	//
	// route("/undeployService/:mavenCoordinates").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Undeploy " + rc.request().params().get("mavenCoordinates"));
	// });
	//
	// }
	//
	// private void addVerticleHandler() {
	// route("/deployVerticle/:clazz").method(GET).handler(rc -> {
	// String clazz = rc.request().params().get("clazz");
	// try {
	// // TODO create merged jsonconfig (see mesh init)
	// JsonObject config = new JsonObject();
	// String id = deployAndWait(vertx, config, clazz);
	// rc.response().end("Deployed " + clazz + " id: " + id);
	// } catch (Exception e) {
	// rc.fail(e);
	// }
	// });
	//
	// route("/undeployVerticle/:clazz").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Undeploy " + rc.request().params().get("clazz"));
	// });
	// }

}
