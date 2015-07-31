package com.gentics.mesh.search;

import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.responde;
import static io.vertx.core.http.HttpMethod.POST;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.search.index.GroupIndexHandler;
import com.gentics.mesh.search.index.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.ProjectIndexHandler;
import com.gentics.mesh.search.index.RoleIndexHandler;
import com.gentics.mesh.search.index.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;
import com.gentics.mesh.search.index.UserIndexHandler;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(SearchVerticle.class);

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Autowired
	private UserIndexHandler userIndexHandler;

	@Autowired
	private GroupIndexHandler groupIndexHandler;

	@Autowired
	private RoleIndexHandler roleIndexHandler;

	@Autowired
	private ProjectIndexHandler projectIndexHandler;

	@Autowired
	private TagIndexHandler tagIndexHandler;

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@Autowired
	private TagFamilyIndexHandler tagFamilyIndexHandler;

	@Autowired
	private SchemaContainerIndexHandler schemaContainerIndexHandler;

	@Autowired
	private MicroschemaContainerIndexHandler microschemaContainerIndexHandler;

	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addEventBusHandlers();
		addSearchEndpoints();
	}

	private void addSearchEndpoints() {
		addUserSearch();
		addGroupSearch();
		addRoleSearch();
		addNodeSearch();
		addTagSearch();
		addTagFamilySearch();
		addProjectSearch();
		addSchemaContainerSearch();
		addMicroschemaContainerSearch();
	}

	private void addSchemaContainerSearch() {
		Route postRoute = route("/schemas").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addProjectSearch() {
		Route postRoute = route("/projects").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addMicroschemaContainerSearch() {
		Route postRoute = route("/microschemas").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addRoleSearch() {
		Route postRoute = route("/roles").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addTagSearch() {
		Route postRoute = route("/tags").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addTagFamilySearch() {
		Route postRoute = route("/tagFamilies").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addNodeSearch() {
		Route postRoute = route("/nodes").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			Client client = elasticSearchNode.client();
			SearchRequestBuilder builder = client.prepareSearch().setQuery(rc.getBodyAsString());
			builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			//TODO configure size by using global setting?
				builder.setSize(25);
				SearchResponse response = builder.execute().actionGet();

				NodeListResponse listResponse = new NodeListResponse();
				for (SearchHit hit : response.getHits()) {
					String uuid = hit.getId();

					// Locate the node
					boot.nodeRoot().findByUuid(uuid, rh -> {
						if (rh.result() != null && rh.succeeded()) {
							Node node = rh.result();
							// Check permissions
							if (requestUser.hasPermission(node, Permission.READ_PERM)) {
								// Transform node and add it to the list of nodes
								node.transformToRest(rc, th -> {
									listResponse.getData().add(th.result());
								});
							}
						} else {
							//TODO log error info?
						}
					});
					//					System.out.println("UUID:" + uuid);
					//					ObjectMapper mapper = new ObjectMapper();
					//					Object json = mapper.readValue(hit.getSourceAsString(), Object.class);
					//					String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
					//					System.out.println(hit.getSourceAsString());
				}
				//TODO add meta info?
				responde(rc, toJson(listResponse));
			});

	}

	private void addGroupSearch() {
		Route postRoute = route("/groups").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addUserSearch() {
		Route postRoute = route("/users").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {

		});
	}

	private void addEventBusHandlers() {
		EventBus bus = vertx.eventBus();
		bus.consumer("search-index-create", (Message<JsonObject> mh) -> {
			String uuid = mh.body().getString("uuid");
			String type = mh.body().getString("type");

			log.info("Creating index entry for " + uuid + " " + type);
			switch (type) {
			case "node":
				nodeIndexHandler.store(uuid);
				break;
			case "tag":
				tagIndexHandler.store(uuid);
				break;
			case "user":
				userIndexHandler.store(uuid);
				break;
			case "group":
				groupIndexHandler.store(uuid);
				break;
			case "role":
				roleIndexHandler.store(uuid);
				break;
			case "project":
				projectIndexHandler.store(uuid);
				break;
			case "schemaContainer":
				schemaContainerIndexHandler.store(uuid);
				break;
			case "microschema":
				microschemaContainerIndexHandler.store(uuid);
				break;
			case "tagFamily":
				tagFamilyIndexHandler.store(uuid);
				break;
			default:
				//TODO throw exception / logging /reply?
			}
		});

		bus.consumer("search-index-delete", (Message<JsonObject> mh) -> {
			String uuid = mh.body().getString("uuid");
			log.info("Delete index entry for " + uuid);
		});

		bus.consumer("search-index-update", (Message<JsonObject> mh) -> {
			String uuid = mh.body().getString("uuid");
			log.info("Update index entry for " + uuid);
		});
	}

}
