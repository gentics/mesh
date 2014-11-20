package com.gentics.resources;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Simple page resource to load and render pages
 */
@Path("/page")
public class PageResource extends AbstractCaiLunResource {

	/**
	 * Load the page with given id and return as Json Object
	 * 
	 * @param response
	 *            asynchronous response
	 * @param vertx
	 *            vertx
	 * @param id
	 *            page id
	 */
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public void get(@Suspended final AsyncResponse response, @Context Vertx vertx, final @PathParam("id") Long id) {
		JsonObject loadRequest = new JsonObject();
		loadRequest.put("id", id);
		vertx.eventBus().send("data-load", loadRequest, optionWithTimeout, (AsyncResult<Message<JsonObject>> event) -> {
			if (event.succeeded()) {
				response.resume(event.result().body().encodePrettily());
			} else {
				response.resume(event.cause());
			}
		});
	}

	/**
	 * Load the page with given id and render it
	 * 
	 * @param response
	 *            asynchronous response
	 * @param vertx
	 *            vertx
	 * @param id
	 *            page id
	 */
	@GET
	@Path("{id}")
	@Produces(MediaType.TEXT_HTML)
	public void render(@Suspended final AsyncResponse response, final @Context Vertx vertx, final @PathParam("id") Long id) {
		JsonObject loadRequest = new JsonObject();
		loadRequest.put("id", id);
		vertx.eventBus().send("data-load", loadRequest, optionWithTimeout, (AsyncResult<Message<JsonObject>> event) -> {
			if (event.succeeded()) {
				vertx.eventBus().send("render", event.result().body(), optionWithTimeout, (AsyncResult<Message<String>> event2) -> {
					if (event2.succeeded()) {
						response.resume(event2.result().body());
					} else {
						response.resume(event2.cause());
					}
				});
			} else {
				response.resume(event.cause());
			}
		});
	}
}
