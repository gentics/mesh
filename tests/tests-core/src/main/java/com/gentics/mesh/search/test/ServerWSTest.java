package com.gentics.mesh.search.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

public class ServerWSTest extends AbstractVerticle {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new ServerWSTest());
	}

	@Override
	public void start(Promise<Void> startFuture) throws Exception {

		// 1. Create the initial router for our API
		Router router = Router.router(vertx);
		// Disabling the body handler fixes the issue with the websocket connections
		router.route().handler(BodyHandler.create());

		// 2. Create a sub router for some nested endpoints
		Router subrouter = Router.router(vertx);
		router.mountSubRouter("/test", subrouter);

		// 3. Setup the SockJS Handler and add a route for it in our API
		SockJSHandlerOptions sockJSoptions = new SockJSHandlerOptions().setHeartbeatInterval(2000);
		SockJSHandler handler = SockJSHandler.create(vertx, sockJSoptions);
		SockJSBridgeOptions bridgeOptions = new SockJSBridgeOptions();
		bridgeOptions.addInboundPermitted(new PermittedOptions().setAddress("dummy"));
		bridgeOptions.addOutboundPermitted(new PermittedOptions().setAddress("dummy"));
		handler.bridge(bridgeOptions, event -> {
			System.out.println("Got event!");
			event.complete(true);
		});
		subrouter.route("/*").handler(handler);

		// 4. Setup the HTTP server
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(4444);
		options.setCompressionSupported(true);
		options.setLogActivity(true);
		HttpServer server = vertx.createHttpServer(options);
		server.requestHandler(router);

		server.listen(rh -> {
			if (rh.failed()) {
				startFuture.fail(rh.cause());
			} else {
				try {
					startFuture.complete();
					// Startup is done - Now setup the client
					HttpClientOptions clientOptions = new HttpClientOptions();
					clientOptions.setDefaultPort(4444).setDefaultHost("localhost");
					HttpClient client = vertx.createHttpClient(clientOptions);

					// Connect to the created websocket endpoint and log the bridged events
					client.webSocket("/test/websocket", wsHandler -> {
						WebSocket ws = wsHandler.result();
						System.out.println("WS Connected");

						// Register to migration events
						JsonObject msg = new JsonObject().put("type", "register").put("address", "dummy");
						ws.writeFinalTextFrame(msg.encode());

						ws.handler(buff -> {
							String str = buff.toString();
							System.out.println("Got event on client: " + str);
						});
					});

					// Finally send events
					vertx.setPeriodic(1000, rh2 -> {
						System.out.println("Sending message");
						vertx.eventBus().publish("dummy", "hello world");
					});
				} catch (Exception e) {
					e.printStackTrace();
					startFuture.fail(e);
				}
			}

		});

	}

}
