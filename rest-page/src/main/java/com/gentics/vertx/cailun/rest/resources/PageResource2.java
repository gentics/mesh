//package com.gentics.vertx.cailun.rest.resources;
//
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Vertx;
//import io.vertx.core.eventbus.Message;
//import io.vertx.core.json.JsonObject;
//
//import javax.ws.rs.GET;
//import javax.ws.rs.PUT;
//import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
//import javax.ws.rs.Produces;
//import javax.ws.rs.container.AsyncResponse;
//import javax.ws.rs.container.Suspended;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MediaType;
//
//import com.gentics.vertx.cailun.repository.Page;
//
///**
// * Simple page resource to load and render pages
// */
//@Path("/page2")
//public class PageResource2 extends AbstractCaiLunResource {
//
//	/**
//	 * Load the page with given id and return as Json Object
//	 * 
//	 * @param response
//	 *            asynchronous response
//	 * @param vertx
//	 *            vertx
//	 * @param id
//	 *            page id
//	 */
//	@GET
//	@Path("{id}")
//	@Produces(MediaType.APPLICATION_JSON)
//	public void get(@Suspended final AsyncResponse response, @Context Vertx vertx, final @PathParam("id") Long id) {
//		JsonObject loadQuery = new JsonObject();
//		String query = "START n=node(" + id + ") RETURN n;";
//		loadQuery.put("query", query);
//		vertx.eventBus().send(DEFAULT_ADDRESS, loadQuery, optionWithTimeout, (AsyncResult<Message<JsonObject>> event) -> {
//			if (event.succeeded()) {
//				response.resume(event.result().body().encodePrettily());
//			} else {
//				response.resume(event.cause());
//			}
//		});
//	}
//
//	@GET
//	@Path("/getPage/{id}")
//	@Produces(MediaType.APPLICATION_JSON)
//	public Page getPage(@Context Vertx vertx, final @PathParam("id") Long id) {
//		Page page = new Page();
//		page.setContent("dsgasdgd");
//		page.setId(1L);
//		page.setName("Some great name");
//		return page;
//	}
//
////	@GET
////	@Path("/getPage")
//	
//	@PUT
//	@Produces(MediaType.APPLICATION_JSON)
//	public void put(@Suspended final AsyncResponse response, @Context Vertx vertx, final Page page) {
//		JsonObject obj = new JsonObject();
//		String query = "CREATE (page" + System.currentTimeMillis() + ":page {name : '" + page.getName() + "', content : '" + page.getContent()
//				+ "' })";
//		obj.put("query", query);
//		logger.info("Sending query: " + query);
//		vertx.eventBus().send(DEFAULT_ADDRESS, obj, optionWithTimeout, (AsyncResult<Message<JsonObject>> event) -> {
//			if (event.succeeded()) {
//				response.resume(event.result().body().encodePrettily());
//			} else {
//				response.resume(event.cause());
//			}
//		});
//		// System.out.println(page.getName());
//	}
//
//}
