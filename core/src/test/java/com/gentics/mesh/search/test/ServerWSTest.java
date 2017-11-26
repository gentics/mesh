package com.gentics.mesh.search.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

public class ServerWSTest extends AbstractVerticle {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new ServerWSTest());
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {

		SockJSHandlerOptions sockJSoptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		SockJSHandler handler = SockJSHandler.create(vertx, sockJSoptions);
		BridgeOptions bridgeOptions = new BridgeOptions();
		bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress("dummy"));
		bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress("dummy"));
		handler.bridge(bridgeOptions, event -> {
			System.out.println("Got event!");
			event.complete(true);
		});

		Router router = Router.router(vertx);
		Router subrouter = Router.router(vertx);
		router.mountSubRouter("/test", subrouter);
		subrouter.route("/*").handler(handler);

		HttpServerOptions options = new HttpServerOptions();
		options.setPort(4444);
		options.setCompressionSupported(true);
		options.setLogActivity(true);
		HttpServer server = vertx.createHttpServer(options);
		server.requestHandler(router::accept);

		server.listen(rh -> {
			if (rh.failed()) {
				startFuture.fail(rh.cause());
			} else {
				try {
					startFuture.complete();
				} catch (Exception e) {
					e.printStackTrace();
					startFuture.fail(e);
				}
			}
		});

		vertx.setPeriodic(1000, rh -> {
			System.out.println("Sending message");
			vertx.eventBus().publish("dummy", "hello world");
		});
	}

}
