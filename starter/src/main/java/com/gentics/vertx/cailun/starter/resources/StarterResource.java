package com.gentics.vertx.cailun.starter.resources;

import io.vertx.core.Vertx;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.JSONP;

@Path("/cailun")
public class StarterResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public MyObject getJson() {
		MyObject o = new MyObject();
		o.setName("Andy");
		return o;
	}

	@GET
	@Path("jsonp")
	@JSONP(queryParam = "cb")
	@Produces("application/javascript")
	public MyObject getJsonPadding() {
		MyObject o = new MyObject();
		o.setName("Andy");
		return o;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public MyObject postJson(MyObject in) {
		return in;
	}

	@GET
	@Path("async")
	@Produces(MediaType.APPLICATION_JSON)
	public void getJsonAsync(@Suspended final AsyncResponse asyncResponse, @Context Vertx vertx) {
		vertx.runOnContext(aVoid -> {
			MyObject o = new MyObject();
			o.setName("Andy");
			asyncResponse.resume(o);
		});
	}

	public static class MyObject {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
