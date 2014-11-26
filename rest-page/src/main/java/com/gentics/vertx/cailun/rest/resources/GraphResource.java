package com.gentics.vertx.cailun.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.gentics.vertx.cailun.repository.TagableContent;
import com.gentics.vertx.cailun.rest.model.response.GenericResponse;

@Path("/graph")
public class GraphResource {

	@GET
	@Path("/getContentForPath/{path}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<TagableContent> getContentForPath(final @PathParam("path") String path) {
		if (path != null) {
			String parts[] = path.split("/");
			String lastPart = parts[parts.length];

		}
		return null;
	}

	@GET
	@Path("getPathForContent/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<TagableContent> getPathForContent() {
		// From node tag ("/") -
		// String query = "START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.id='" + id + "' AND r.name='" + name + "' DELETE rel";
		// MATCH p =(begin)-[*]->(END ) WHERE begin.name='A' AND END .name='D' RETURN reduce(a="", n IN nodes(p)| a+ "/" + n.name)
		return null;
	}

}
