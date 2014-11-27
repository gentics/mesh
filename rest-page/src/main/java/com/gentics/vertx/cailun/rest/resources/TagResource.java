package com.gentics.vertx.cailun.rest.resources;

import io.vertx.core.Vertx;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.vertx.cailun.repository.Page;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.Tag;
import com.gentics.vertx.cailun.rest.model.request.PageCreateRequest;
import com.gentics.vertx.cailun.rest.model.response.GenericResponse;
import com.google.common.collect.Lists;

@Path("/tag")
public class TagResource {

	@Autowired
	PageRepository pageRepository;

	@PUT
	@Path("/add/{tagPath:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Tag> addTagStructure(@Context Vertx vertx, PageCreateRequest request, final @PathParam("tagPath") String tagPath)
			throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		ExecutionEngine engine = new ExecutionEngine(graphDb);

		String query = transformPathToCypher(tagPath);
		System.out.println(query);
		// WITH tag,page MERGE (tag)-[r:TAGGED]->(page) RETURN r
		try (Transaction tx = graphDb.beginTx()) {
			ExecutionResult result = engine.execute(query);
		}
		return null;
	}

	@GET
	@Path("/get/{path:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	public GenericResponse<Page> getContentForPath(final @PathParam("path") String path) throws Exception {
		// TODO check whether pageRepository.findAllByTraversal(startNode, traversalDescription) might be an alternative
		Long pageId = getPageNodeIdForPath(path);
		Page page = pageRepository.findOne(pageId);
		return new GenericResponse<Page>(page);
	}

	private Long getPageNodeIdForPath(String path) throws Exception {
		String parts[] = path.split("/");

		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		try (Transaction tx = graphDb.beginTx()) {
			Node currentNode = graphDb.getNodeById(0L);
			for (int i = 0; i < parts.length - 1; i++) {
				String part = parts[i];
				Node nextNode = getChildNodeTagFromNodeTag(currentNode, part);
				if (nextNode != null) {
					currentNode = nextNode;
				} else {
					currentNode = null;
					break;
				}
			}
			if (currentNode != null) {
				// Finally search for the page and assume the last part of the request as filename
				Node pageNode = getChildNodePageFromNodeTag(currentNode, parts[parts.length - 1]);
				if (pageNode != null) {
					return pageNode.getId();
				} else {
					return null;
				}
			}
		}
		return null;
	}

	private Node getChildNodePageFromNodeTag(Node node, String pageFilename) {
		AtomicReference<Node> foundNode = new AtomicReference<>();
		Lists.newArrayList(node.getRelationships()).stream().filter(rel -> "TAGGED".equalsIgnoreCase(rel.getType().name())).forEach(rel -> {
			Node nextHop = rel.getStartNode();
			if (nextHop.hasLabel(DynamicLabel.label("Page"))) {
				String currentName = (String) nextHop.getProperty("filename");
				if (pageFilename.equalsIgnoreCase(currentName)) {
					foundNode.set(nextHop);
					return;
				}
			}
		});
		return foundNode.get();

	}

	private Node getChildNodeTagFromNodeTag(Node node, String tagName) {
		AtomicReference<Node> foundNode = new AtomicReference<>();
		Lists.newArrayList(node.getRelationships()).stream().filter(rel -> "TAGGED".equalsIgnoreCase(rel.getType().name())).forEach(rel -> {
			Node nextHop = rel.getEndNode();
			if (nextHop.hasLabel(DynamicLabel.label("Tag"))) {
				String currentName = (String) nextHop.getProperty("name");
				if (tagName.equalsIgnoreCase(currentName)) {
					foundNode.set(nextHop);
					return;
				}
			}
		});

		return foundNode.get();
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
			if (i < rels - 1) {
				builder.append(",");
			}
		}
		return builder.toString();
	}

}
