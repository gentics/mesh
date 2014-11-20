package com.gentics.vertx.cailun.starter;


//public class Server extends AbstractVerticle {
//
//	public void start() {
////		getVertx().eventBus().consumer("deploy").handler(new DeployHandler(vertx));
////		getVertx().eventBus().consumer("undeploy").handler(new UndeployHandler(vertx));
////		getVertx().eventBus().consumer("list").handler(new ListDeployments(vertx));
//
//		RouteMatcher matcher = new RouteMatcherImpl();
//		matcher	("/send/:address/:message", new Handler<HttpServerRequest>() {
//			@Override
//			public void handle(HttpServerRequest req) {
//				final HttpServerResponse response = req.response();
//				response.setChunked(true);
//				MultiMap params = req.params();
//
//				String address = params.get("address");
//				String message = params.get("message");
//
//				vertx.eventBus().send(address, message, new DeliveryOptions().setSendTimeout(10000), new Handler<AsyncResult<Message<String>>>() {
//					@Override
//					public void handle(AsyncResult<Message<String>> event) {
//						if (event.succeeded()) {
//							response.write("Succeeded.\n");
//							response.end("Received reply: " + event.result().body() + "\n");
//						} else {
//							Throwable cause = event.cause();
//							if (cause instanceof ReplyException) {
//								switch (((ReplyException) cause).failureType()) {
//								case NO_HANDLERS:
//									response.setStatusMessage("Not found").setStatusCode(404).end();
//									break;
//								case RECIPIENT_FAILURE:
//									response.setStatusMessage("Internal Server Error").setStatusCode(500).end();
//									break;
//								case TIMEOUT:
//									response.setStatusMessage("Service Unavailable").setStatusCode(503).end();
//									break;
//								}
//							} else {
//								response.setStatusMessage("Internal Server Error").setStatusCode(500).end();
//							}
//						}
//					}
//				});
//			}
//		});
//		getVertx().createHttpServer(new HttpServerOptions().setPort(8080).setHost("0.0.0.0")).requestHandler(request -> matcher.accept(request));
//	}
//}
