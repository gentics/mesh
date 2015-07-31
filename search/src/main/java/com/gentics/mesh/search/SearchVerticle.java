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
import io.vertx.ext.web.RoutingContext;

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
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
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
		addSearch("users", boot.userRoot(), UserListResponse.class);
		addSearch("groups", boot.groupRoot(), GroupListResponse.class);
		addSearch("role", boot.roleRoot(), RoleListResponse.class);
		addSearch("nodes", boot.nodeRoot(), NodeListResponse.class);
		addSearch("tags", boot.tagRoot(), TagListResponse.class);
		addSearch("tagFamilies", boot.tagFamilyRoot(), TagFamilyListResponse.class);
		addSearch("projects", boot.projectRoot(), ProjectListResponse.class);
		addSearch("schemas", boot.schemaContainerRoot(), SchemaListResponse.class);
		addSearch("microschemas", boot.microschemaContainerRoot(), MicroschemaListResponse.class);
	}

	private <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void addSearch(String typeName,
			RootVertex<T> root, Class<RL> classOfRL) {
		Route postRoute = route("/" + typeName).method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			try {
				handleSearch(rc, root, classOfRL);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	private <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void handleSearch(RoutingContext rc,
			RootVertex<T> rootVertex, Class<RL> classOfRL) throws InstantiationException, IllegalAccessException {

		RL listResponse = classOfRL.newInstance();
		MeshAuthUser requestUser = getUser(rc);
		Client client = elasticSearchNode.client();
		SearchRequestBuilder builder = client.prepareSearch().setQuery(rc.getBodyAsString());
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		/* TODO configure size by using global setting? */
		builder.setSize(25);
		SearchResponse response = builder.execute().actionGet();

		// TODO handle paging?
		for (SearchHit hit : response.getHits()) {
			String uuid = hit.getId();

			// Locate the node
			rootVertex.findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					T element = rh.result();
					// Check permissions
					if (requestUser.hasPermission(element, Permission.READ_PERM)) {
						// Transform node and add it to the list of nodes
					element.transformToRest(rc, th -> {
						listResponse.getData().add(th.result());
					});
				}
			} else {
				// TODO log error info?
			}
		}	);
		}
		// TODO add meta info?
		responde(rc, toJson(listResponse));

	}

	private void addEventBusHandlers() {
		EventBus bus = vertx.eventBus();

		bus.consumer("search-queue-entry", mh -> {
			GenericVertex<?> element = boot.meshRoot().getSearchQueueRoot().getNext();
			if (element != null) {
				System.out.println(element.getUuid());
				System.out.println(element.getClass());
				// TODO invoke matching store method
				boot.meshRoot().getSearchQueueRoot().removeElement(element);
			}
		});

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
				// TODO throw exception / logging /reply?
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
