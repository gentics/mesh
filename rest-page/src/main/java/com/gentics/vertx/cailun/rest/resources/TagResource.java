package com.gentics.vertx.cailun.rest.resources;

import io.vertx.core.Vertx;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import com.gentics.vertx.cailun.repository.Tag;
import com.gentics.vertx.cailun.rest.model.request.PageCreateRequest;
import com.gentics.vertx.cailun.rest.model.response.GenericResponse;

@Path("/tag")
public class TagResource {

	@PUT
	@Path("/add/{tagPath:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> addTagStructure(@Context Vertx vertx, PageCreateRequest request, final @PathParam("tagPath") String tagPath)
			throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		ExecutionEngine engine = new ExecutionEngine(graphDb);
		
		String query =  transformPathToCypher(tagPath);
		System.out.println(query);
		// WITH tag,page MERGE (tag)-[r:TAGGED]->(page) RETURN r
		try (Transaction tx = graphDb.beginTx()) {
			ExecutionResult result = engine.execute(query);
		}
		return null;

	}

	
	private String transformPathToCypher(String tagPath) {
		String parts[] = tagPath.split("/");
		StringBuilder builder = new StringBuilder();
		List<String> tagNames = new ArrayList<>();
		int n = 1;
		for (String part : parts) {
			String tagName = "tag" + n;
			tagNames.add(tagName);
			builder.append("MERGE (tag" + n + ":Tag { name:'" + part + "'}) ");
			n++;
		}

		int rels = 0;
		for (int i = 0; i < tagNames.size(); i++) {
			if (i == tagNames.size() - 1) {
				builder.append("(" + tagNames.get(i) + ")");
				continue;
			} else {
				builder.append("(" + tagNames.get(i) + ")-[r" + i + ":TAGGED]->");
				rels++;
			}
		}
		builder.append(" RETURN ");
		for (int i = 0; i < rels; i++) {
			builder.append("r" + i);
			if (i < rels -1) {
				builder.append(",");
			}
		}
		return builder.toString();
	}

}
