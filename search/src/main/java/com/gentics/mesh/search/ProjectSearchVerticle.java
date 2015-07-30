package com.gentics.mesh.search;

import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.POST;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.json.JsonUtil;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectSearchVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectSearchVerticle.class);

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	public ProjectSearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addEventBusHandlers();
		addSearchEndpoint();
	}

	private void addSearchEndpoint() {
		Route postRoute = route().method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			Client client = elasticSearchNode.client();
			SearchRequestBuilder builder = client.prepareSearch().setQuery(rc.getBodyAsString());
			builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			//TODO configure size by using global setting?
				builder.setSize(25);
				SearchResponse response = builder.execute().actionGet();

				Iterator<SearchHit> hit_it = response.getHits().iterator();
				while (hit_it.hasNext()) {
					SearchHit hit = hit_it.next();
					//					ObjectMapper mapper = new ObjectMapper();
					//					Object json = mapper.readValue(hit.getSourceAsString(), Object.class);
					//					String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
					System.out.println(hit.getSourceAsString());
				}

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
				boot.nodeRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						Node node = rh.result();
						storeNode(node);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "tag":
				boot.tagRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						Tag tag = rh.result();
						storeTag(tag);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "user":
				boot.userRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						User user = rh.result();
						storeUser(user);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "group":
				boot.groupRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						Group group = rh.result();
						storeGroup(group);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "role":
				boot.roleRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						Role role = rh.result();
						storeRole(role);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "schemaContainer":
				boot.schemaContainerRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						SchemaContainer container = rh.result();
						storeSchemaContainer(container);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "microschema":
				boot.microschemaContainerRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						MicroschemaContainer microschema = rh.result();
						storeMicroschemaContainer(microschema);
					} else {
						//TODO reply error? discard? log?
					}
				});
				break;
			case "tagFamily":
				boot.tagFamilyRoot().findByUuid(uuid, rh -> {
					if (rh.result() != null && rh.succeeded()) {
						TagFamily tagFamily = rh.result();
						storeTagFamily(tagFamily);
					} else {
						//TODO reply error? discard? log?
					}
				});
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

	private void storeMicroschemaContainer(MicroschemaContainer microschema) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, microschema);
		map.put("name", microschema.getName());
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("microschema", "microschema", microschema.getUuid()).setSource(map)
				.execute().actionGet();
	}

	private void storeTag(Tag tag) {
		Map<String, Object> map = new HashMap<>();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put("name", tag.getName());
		map.put("fields", tagFields);
		addBasicReferences(map, tag);
		addTagFamily(map, tag.getTagFamily());
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("tag", "tag", tag.getUuid()).setSource(map).execute().actionGet();
	}

	private void storeTagFamily(TagFamily tagFamily) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", tagFamily.getName());
		addBasicReferences(map, tagFamily);
		addTags(map, tagFamily.getTags());
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("tagFamily", "tagFamily", tagFamily.getUuid()).setSource(map).execute()
				.actionGet();

	}

	private void storeSchemaContainer(SchemaContainer container) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", container.getName());
		addBasicReferences(map, container);
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("schemaContainer", "schemaContainer", container.getUuid())
				.setSource(map).execute().actionGet();
	}

	private void storeRole(Role role) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", role.getName());
		addBasicReferences(map, role);
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("role", "role", role.getUuid()).setSource(map).execute().actionGet();
	}

	private void storeGroup(Group group) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", group.getName());
		addBasicReferences(map, group);
		//TODO addusers
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("group", "group", group.getUuid()).setSource(map).execute().actionGet();
	}

	private void storeUser(User user) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, user);
		map.put("username", user.getUsername());
		map.put("emailadress", user.getEmailAddress());
		map.put("firstname", user.getFirstname());
		map.put("lastname", user.getLastname());
		//TODO add node reference?
		//TODO add disabled / enabled flag
		IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("user", "user", user.getUuid()).setSource(map).execute().actionGet();
	}

	private void storeNode(Node node) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, node);
		addSchema(map, node);
		addProject(map, node.getProject());
		addTags(map, node.getTags());
		for (NodeFieldContainer container : node.getFieldContainers()) {
			removeFieldEntries(map);
			map.remove("language");
			String language = container.getLanguage().getLanguageTag();
			map.put("language", language);

			String json = JsonUtil.toJson(map);
			System.out.println(json);
			IndexResponse indexResponse = elasticSearchNode.client().prepareIndex("node", "node-" + language, node.getUuid()).setSource(map)
					.execute().actionGet();
		}

	}

	private void addTags(Map<String, Object> map, List<? extends Tag> tags) {

		List<String> tagUuids = new ArrayList<>();
		List<String> tagNames = new ArrayList<>();
		for (Tag tag : tags) {
			tagUuids.add(tag.getUuid());
			tagNames.add(tag.getName());
		}
		Map<String, List<String>> tagFields = new HashMap<>();
		tagFields.put("uuid", tagUuids);
		tagFields.put("name", tagNames);
		map.put("tags", tagFields);

	}

	private void addProject(Map<String, Object> map, Project project) {
		Map<String, String> projectFields = new HashMap<>();
		projectFields.put("name", project.getName());
		projectFields.put("uuid", project.getUuid());
		map.put("project", projectFields);
	}

	private void addSchema(Map<String, Object> map, Node node) {
		SchemaContainer schemaContainer = node.getSchemaContainer();
		String name = schemaContainer.getName();
		String uuid = schemaContainer.getUuid();
		Map<String, String> schemaFields = new HashMap<>();
		schemaFields.put("name", name);
		schemaFields.put("uuid", uuid);
		map.put("schema", schemaFields);
	}

	private void addUser(Map<String, Object> map, String prefix, User user) {
		//TODO make sure field names match response UserResponse field names.. 
		Map<String, Object> userFields = new HashMap<>();
		userFields.put("username", user.getUsername());
		userFields.put("emailadress", user.getEmailAddress());
		userFields.put("firstname", user.getFirstname());
		userFields.put("lastname", user.getLastname());
		//TODO add disabled / enabled flag
		map.put(prefix, userFields);
	}

	private void addBasicReferences(Map<String, Object> map, GenericVertex<?> vertex) {
		//TODO make sure field names match node response
		map.put("uuid", vertex.getUuid());
		addUser(map, "creator", vertex.getCreator());
		addUser(map, "editor", vertex.getEditor());
		map.put("lastEdited", vertex.getLastEditedTimestamp());
		map.put("created", vertex.getCreationTimestamp());
	}

	private void addTagFamily(Map<String, Object> map, TagFamily tagFamily) {
		Map<String, Object> tagFamilyFields = new HashMap<>();
		tagFamilyFields.put("name", tagFamily.getName());
		tagFamilyFields.put("uuid", tagFamily.getUuid());
		map.put("tagFamily", tagFamilyFields);
	}

	private void removeFieldEntries(Map<String, Object> map) {
		for (String key : map.keySet()) {
			if (key.startsWith("field.")) {
				map.remove(key);
			}
		}
	}

}
