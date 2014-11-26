package com.gentics.vertx.cailun.rest.resources;

import io.vertx.core.Vertx;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.repository.Page;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.Tag;
import com.gentics.vertx.cailun.rest.POJOHelper;
import com.gentics.vertx.cailun.rest.model.request.PageCreateRequest;
import com.gentics.vertx.cailun.rest.model.response.GenericResponse;

/**
 * Simple page resource to load and render pages
 */
@Component
@Scope("singleton")
@Path("/page")
public class PageResource extends AbstractCaiLunResource {

	@Autowired
	private PageRepository pageRepository;

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Page getPage(@Context Vertx vertx, final @PathParam("id") Long id) throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		
		System.out.println("COUNT:  " + pageRepository.count());

		Page page = new Page();
		page.setContent("dsgasdgd");
		page.setId(id);
		page.setAuthor("Jotschi");
		page.setTitle("My Title");
		page.setTeaser("Guckst du hier schnell!!");
		page.setName("Some great name");
		return page;
	}

	@GET
	@Path("/pages")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<List<Page>> getPages() throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		ExecutionEngine engine = new ExecutionEngine(graphDb);
		String query = "MATCH (page:Page)<-[:`TAGGED`]-(tag:Tag) RETURN page,tag";

		try (Transaction tx = graphDb.beginTx()) {
			ExecutionResult result = engine.execute(query);
			Iterator<Node> tags = result.columnAs("tag");

		}

		GenericResponse response = new GenericResponse<>();

		return null;
	}

	@PUT
	@Path("tag/{id}/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> addTag(@Context Vertx vertx, PageCreateRequest request, final @PathParam("name") String name,
			final @PathParam("id") Long id) throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		// MATCH (page:Page { name: 'test111'}) MERGE (tag:Tag { name:'test'}) WITH tag,page MERGE (tag)-[r:TAGGED]->(page) RETURN r
		String queryRel = "MERGE (tag:Tag { name:'" + name + "'}) MATCH (page:Page { id:" + id + " }) MERGE (tag)-[r:TAGGED]->(page) RETURN r";
		try (Transaction tx = graphDb.beginTx()) {

		}
		return null;

	}

	@PUT
	@Path("untag/{id}/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> removeTag(@Context Vertx vertx, PageCreateRequest request, final @PathParam("name") String name,
			final @PathParam("id") Long id) {

		// MATCH (page:Page {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(page) DELETE rel
		String query = "START n=node(*) MATCH n-[rel:TAGGED]->r WHERE n.id='" + id + "' AND r.name='" + name + "' DELETE rel";

		return null;

	}

	/**
	 * Return the relation
	 * 
	 * @param id
	 * @param name
	 */
	@GET
	@Path("tag/{id}/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> getTag(final @PathParam("id") Long id, final @PathParam("name") String name) {
		String query = "MATCH (page:Page {name:'test111'}), (tag:Tag {name:'test'}) MATCH (tag)-[rel:`TAGGED`]->(page) return rel";
		return null;
	}

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Page> createPage(@Context Vertx vertx, PageCreateRequest request) throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();

		try (Transaction tx = graphDb.beginTx()) {
			Label name = DynamicLabel.label(Page.class.getSimpleName());
			Node node = graphDb.createNode(name);
			POJOHelper.toNode(request.getPage(), node);
			tx.success();
		}

		GenericResponse response = new GenericResponse<>();
		return response;
	}
}
