package com.gentics.mesh.core.verticle.admin;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 * The admin verticle provides core administration rest endpoints.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class AdminVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminVerticle.class);

	public static final String GIT_PULL_CHECKER_INTERVAL_KEY = "gitPullCheckerInterval";
	public static final String GIT_PULL_CHECKER_KEY = "gitPullChecker";

	public static final boolean DEFAULT_GIT_CHECKER = false;
	public static final long DEFAULT_GIT_CHECKER_INTERVAL = 60 * 5 * 100; // 5 Min

	@Autowired
	private AdminHandler handler;

	//	private GitPullChecker gitChecker;

	public AdminVerticle() {
		super("admin");
	}

	@Override
	public void registerEndPoints() throws Exception {
		//		if (config().getBoolean(GIT_PULL_CHECKER_KEY, DEFAULT_GIT_CHECKER)) {
		//			gitChecker = new GitPullChecker(config().getLong(GIT_PULL_CHECKER_INTERVAL_KEY, DEFAULT_GIT_CHECKER_INTERVAL));
		//		}

		addStatusHandler();
		addEventBusHandler();

		// TODO secure handlers below
		addBackupHandler();
		addRestoreHandler();
		addImportHandler();
		addExportHandler();
		// addVerticleHandler();
		// addServiceHandler();

		route("/test").handler(rh -> {
			JsonObject msg = new JsonObject();
			msg.put("text", "Hallo Welt" + System.currentTimeMillis());
			vertx.eventBus().send("some-address", msg);
			rh.response().end("msg sent");
		});

	}

	private void addEventBusHandler() {
		SockJSHandler handler = SockJSHandler.create(vertx);
		BridgeOptions options = new BridgeOptions();
		options.addInboundPermitted(new PermittedOptions().setAddress("some-address"));
		options.addOutboundPermitted(new PermittedOptions().setAddress("some-address"));
		handler.bridge(options, event -> {
			if (event.type() == BridgeEventType.SOCKET_CREATED) {
				log.info("A socket was created");
			}
			event.complete(true);
		});
		route("/eventbus/*").handler(handler);
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

	@Override
	public void stop() throws Exception {
		super.stop();
		//		if (gitChecker != null) {
		//			gitChecker.close();
		//		}
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
