package com.gentics.vertx.cailun.base;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.vertx.cailun.base.rest.request.PageCreateRequest;
import com.gentics.vertx.cailun.core.CaiLunLinkResolver;
import com.gentics.vertx.cailun.core.CaiLunLinkResolverFactoryImpl;
import com.gentics.vertx.cailun.core.LinkReplacer;
import com.gentics.vertx.cailun.page.PageRepository;
import com.gentics.vertx.cailun.page.model.Page;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;
import com.gentics.vertx.cailun.rest.response.GenericResponse;
import com.gentics.vertx.cailun.tag.TagRepository;
import com.gentics.vertx.cailun.tag.model.Tag;
import com.google.common.collect.Lists;


@Component
@Scope("singleton")
@SpringVerticle
public class TagVerticle extends AbstractCailunRestVerticle {
	
	private static Logger log = LoggerFactory.getLogger(TagVerticle.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private CaiLunLinkResolverFactoryImpl<CaiLunLinkResolver> resolver;

	@Autowired
	GraphDatabaseService graphDb;

	public TagVerticle() {
		super("tag");
	}

	@Override
	public void start() throws Exception {
		super.start();

		addAddTagStructureHandler();
		addGetPathHandler();
	}

	private void addGetPathHandler() {
		// @Path("/get/{path:.*}")
		
		getRouter().routeWithRegex("\\/get\\/(.*)").method(GET).handler(rc -> {
			try {
				String path = rc.request().params().get("param0");
				// TODO check whether pageRepository.findAllByTraversal(startNode, traversalDescription) might be an alternative
				Long pageId = getPageNodeIdForPath(path);
				if (pageId != null) {
					Page page = pageRepository.findOne(pageId);
					resolveLinks(page);
					ObjectMapper mapper = new ObjectMapper();
					String json = mapper.writeValueAsString(new GenericResponse<Page>(page));
					rc.response().end(json);
				} else {
					rc.fail(new Exception("Page for path {" + path + "} could not be found."));
					// TODO add json response - Make error responses generic
					rc.response().end("Element not found");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		});

	}

	private void addAddTagStructureHandler() {
		// @Path("/add/{tagPath:.*}")
		route("/add/:tagPath").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {

			PageCreateRequest request = fromJson(rc.request(), PageCreateRequest.class);
			String tagPath = rc.request().params().get("tagPath");
			// final @PathParam("tagPath") String tagPath
				ExecutionEngine engine = new ExecutionEngine(graphDb);

				String query = transformPathToCypher(tagPath);
				System.out.println(query);
				// WITH tag,page MERGE (tag)-[r:TAGGED]->(page) RETURN r
				try (Transaction tx = graphDb.beginTx()) {
					ExecutionResult result = engine.execute(query);
				}
			});
	}

	private void resolveLinks(Page page) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a spring service
		LinkReplacer replacer = new LinkReplacer(resolver);
		page.setContent(replacer.replace(page.getContent()));
	}

	private Long getPageNodeIdForPath(String path) throws Exception {
		String parts[] = path.split("/");
		Tag rootTag = tagRepository.findRootTag();
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		try (Transaction tx = graphDb.beginTx()) {
			Node currentNode = graphDb.getNodeById(rootTag.getId());
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
