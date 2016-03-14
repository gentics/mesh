package com.gentics.mesh.demo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.MeshRestLocalClientImpl;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class DemoDataProvider {

	private static final Logger log = LoggerFactory.getLogger(DemoDataProvider.class);

	public static final String PROJECT_NAME = "demo";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	@Autowired
	private Database db;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	private MeshRestLocalClientImpl client;

	private Language english;

	private Language german;

	//	private MeshRoot root;

	private Map<String, ProjectResponse> projects = new HashMap<>();
	private Map<String, Schema> schemas = new HashMap<>();
	private Map<String, TagFamilyResponse> tagFamilies = new HashMap<>();
	private Map<String, NodeResponse> nodes = new HashMap<>();
	private Map<String, TagResponse> tags = new HashMap<>();
	private Map<String, UserResponse> users = new HashMap<>();
	private Map<String, RoleResponse> roles = new HashMap<>();
	private Map<String, GroupResponse> groups = new HashMap<>();

	private DemoDataProvider() {
	}

	public void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException, InterruptedException {
		long start = System.currentTimeMillis();

		db.noTrx(() -> {
			//bootstrapInitializer.initMandatoryData();

			MeshAuthUser user = MeshRoot.getInstance().getUserRoot().findMeshAuthUserByUsername("admin");
			client.setUser(user);

			//			root = boot.meshRoot();
			//			english = boot.languageRoot().findByLanguageTag("en");
			//			german = boot.languageRoot().findByLanguageTag("de");

			addBootstrappedData();

			addRoles();
			addGroups();
			addUsers();

			addProjects();
			addTagFamilies();
			addTags();

			addSchemaContainers();
			addNodes();
			return null;
		});
		//		updatePermissions();
		//		invokeFullIndex();
		long duration = System.currentTimeMillis() - start;
	}

	/**
	 * Load users JSON file and create users.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void addUsers() throws IOException, InterruptedException {
		JsonObject usersJson = loadJson("users");
		JsonArray dataArray = usersJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject userJson = dataArray.getJsonObject(i);
			String email = userJson.getString("email");
			String username = userJson.getString("username");
			String firstname = userJson.getString("firstName");
			String lastname = userJson.getString("lastName");
			String password = userJson.getString("password");

			log.info("Creating user {" + username + "}");

			UserCreateRequest request = new UserCreateRequest();
			request.setUsername(username);
			request.setEmailAddress(email);
			request.setFirstname(firstname);
			request.setLastname(lastname);
			request.setPassword(password);
			Future<UserResponse> future = client.createUser(request);
			latchFor(future);

			users.put(username, future.result());

			JsonArray groupArray = userJson.getJsonArray("groups");
			for (int e = 0; e < groupArray.size(); e++) {
				//				user.addGroup(getGroup(groupArray.getString(e)));
			}
		}

	}

	/**
	 * Add groups from JSON file to graph.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws Throwable
	 */
	private void addGroups() throws InterruptedException, IOException {
		JsonObject groupsJson = loadJson("groups");
		JsonArray dataArray = groupsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject groupJson = dataArray.getJsonObject(i);
			String name = groupJson.getString("name");

			log.info("Creating group {" + name + "}");
			GroupCreateRequest groupCreateRequest = new GroupCreateRequest();
			groupCreateRequest.setName(name);
			Future<GroupResponse> groupResponseFuture = client.createGroup(groupCreateRequest);
			latchFor(groupResponseFuture);
			GroupResponse group = groupResponseFuture.result();
			groups.put(name, group);

			JsonArray rolesNode = groupJson.getJsonArray("roles");
			for (int e = 0; e < rolesNode.size(); e++) {
				Future<GroupResponse> future = client.addRoleToGroup(group.getUuid(), getRole(rolesNode.getString(e)).getUuid());
				latchFor(future);
			}
		}
	}

	private void latchFor(Future<?> future) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		future.setHandler(rh -> {
			latch.countDown();
		});
		assertTrue("The timeout of the latch was reached.", latch.await(10, TimeUnit.SECONDS));

		if (future.failed()) {
			throw new RuntimeException(future.cause());
		}
	}

	/**
	 * Load roles JSON file and add those roles to the graph.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void addRoles() throws IOException, InterruptedException {
		JsonObject rolesJson = loadJson("roles");
		JsonArray dataArray = rolesJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject roleJson = dataArray.getJsonObject(i);
			String name = roleJson.getString("name");

			log.info("Creating role {" + name + "}");
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName(name);
			Future<RoleResponse> roleFuture = client.createRole(request);
			latchFor(roleFuture);

			//role.grantPermissions(role, READ_PERM);
			roles.put(name, roleFuture.result());
		}
	}

	/**
	 * Add data to the internal maps which was created within the {@link BootstrapInitializer} (eg. admin groups, roles, users)
	 * 
	 * @throws InterruptedException
	 */
	private void addBootstrappedData() throws InterruptedException {

		Future<GroupListResponse> groupsFuture = client.findGroups();
		latchFor(groupsFuture);
		for (GroupResponse group : groupsFuture.result().getData()) {
			groups.put(group.getName(), group);
		}

		Future<UserListResponse> usersFuture = client.findUsers();
		latchFor(usersFuture);
		for (UserResponse user : usersFuture.result().getData()) {
			users.put(user.getUsername(), user);
		}

		Future<RoleListResponse> rolesFuture = client.findRoles();
		latchFor(rolesFuture);
		for (RoleResponse role : rolesFuture.result().getData()) {
			roles.put(role.getName(), role);
		}
	}

	/**
	 * Load nodes JSON file and add those nodes to the graph.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void addNodes() throws IOException, InterruptedException {
		JsonObject nodesJson = loadJson("nodes");
		JsonArray dataArray = nodesJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject nodeJson = dataArray.getJsonObject(i);
			ProjectResponse project = getProject(nodeJson.getString("project"));
			String schemaName = nodeJson.getString("schema");
			String name = nodeJson.getString("name");
			String parentNodeName = nodeJson.getString("parent");
			Schema schema = getSchemaModel(schemaName);
			NodeResponse parentNode = getNode(parentNodeName);

			log.info("Creating node {" + name + "} for schema {" + schemaName + "}");
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
			nodeCreateRequest.setSchema(new SchemaReference().setUuid(schema.getUuid()));
			nodeCreateRequest.getFields().put("name", FieldUtil.createStringField(name));

			JsonObject fieldsObject = nodeJson.getJsonObject("fields");
			if (fieldsObject != null) {
				for (String fieldName : fieldsObject.fieldNames()) {
					Object obj = fieldsObject.getValue(fieldName);
					if ("vehicleImage".equals(fieldName)) {
						nodeCreateRequest.getFields().put(fieldName, FieldUtil.createNodeField(getNode((String) obj).getUuid()));
						continue;
					}
					if ("description".equals(fieldName)) {
						nodeCreateRequest.getFields().put(fieldName, FieldUtil.createHtmlField(String.valueOf(obj)));
						continue;
					}
					if (obj instanceof Integer || obj instanceof Float || obj instanceof Double) {
						nodeCreateRequest.getFields().put(fieldName,
								FieldUtil.createNumberField(com.gentics.mesh.util.NumberUtils.createNumber(obj.toString())));
					} else if (obj instanceof String) {
						nodeCreateRequest.getFields().put(fieldName, FieldUtil.createStringField((String) obj));
					} else {
						throw new RuntimeException("Demo data type {" + obj.getClass().getName() + "} for field {" + fieldName + "} is unknown.");
					}
				}
			}
			//englishContainer.updateWebrootPathInfo("node_conflicting_segmentfield_update");

			Future<NodeResponse> nodeCreateFuture = client.createNode(project.getName(), nodeCreateRequest);
			latchFor(nodeCreateFuture);
			NodeResponse createdNode = nodeCreateFuture.result();

			// Upload binary data
			JsonObject binNode = nodeJson.getJsonObject("bin");
			if (binNode != null) {
				Buffer fileData = Buffer.buffer();
				String path = binNode.getString("path");
				//				int height = binNode.getInteger("height");
				//				int width = binNode.getInteger("width");
				String filenName = binNode.getString("filename");
				String contentType = binNode.getString("contentType");
				InputStream ins = getClass().getResourceAsStream("/data/" + path);
				if (ins == null) {
					throw new NullPointerException("Could not find binary file within path {" + path + "}");
				}

				Future<GenericMessageResponse> binaryUpdateFuture = client.updateNodeBinaryField(PROJECT_NAME, createdNode.getUuid(), "en", "image",
						fileData, filenName, contentType);
				latchFor(binaryUpdateFuture);

			}

			// Add tags to node
			JsonArray tagArray = nodeJson.getJsonArray("tags");
			for (int e = 0; e < tagArray.size(); e++) {
				String tagName = tagArray.getString(e);
				Future<NodeResponse> tagAddedFuture = client.addTagToNode(PROJECT_NAME, createdNode.getUuid(), getTag(tagName).getUuid());
				latchFor(tagAddedFuture);
			}

			nodes.put(name, createdNode);
		}

	}

	/**
	 * Load tags JSON and add those tags to the graph.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void addTags() throws IOException, InterruptedException {
		JsonObject tagsJson = loadJson("tags");
		JsonArray dataArray = tagsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagJson = dataArray.getJsonObject(i);
			String name = tagJson.getString("name");
			String tagFamilyName = tagJson.getString("tagFamily");

			log.info("Creating tag {" + name + "} to family {" + tagFamilyName + "}");
			TagFamilyResponse tagFamily = getTagFamily(tagFamilyName);
			// TODO determine project of tag family automatically or use json field to assign it
			TagCreateRequest createRequest = new TagCreateRequest();
			createRequest.getFields().setName(name);
			Future<TagResponse> tagFuture = client.createTag(PROJECT_NAME, tagFamily.getUuid(), createRequest);
			latchFor(tagFuture);
			tags.put(name, tagFuture.result());
		}
	}

	/**
	 * Load project JSON file and add those projects to the graph.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void addProjects() throws IOException, InterruptedException {
		JsonObject projectsJson = loadJson("projects");
		JsonArray dataArray = projectsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject projectJson = dataArray.getJsonObject(i);
			String name = projectJson.getString("name");

			log.info("Creating project {" + name + "}");
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(name);
			Future<ProjectResponse> projectFuture = client.createProject(request);
			latchFor(projectFuture);

			//TODO impl. once endpoint exists
			//client.assignLanguageToProject(projectFuture.result().getUuid(), getEnglish().getUuid());
			//client.assignLanguageToProject(projectFuture.result().getUuid(), getGerman().getUuid());
			ProjectResponse project = projectFuture.result();

			Future<NodeResponse> nodeFuture = client.findNodeByUuid(PROJECT_NAME, project.getRootNodeUuid());
			latchFor(nodeFuture);

			nodes.put(name + ".basenode", nodeFuture.result());
			projects.put(name, projectFuture.result());
		}
	}

	private void addTagFamilies() throws IOException, InterruptedException {
		JsonObject tagfamilyJson = loadJson("tagfamilies");
		JsonArray dataArray = tagfamilyJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagFamilyJson = dataArray.getJsonObject(i);
			String name = tagFamilyJson.getString("name");
			String projectName = tagFamilyJson.getString("project");

			log.info("Creating tagfamily {" + name + "} for project {" + projectName + "}");
			TagFamilyCreateRequest request = new TagFamilyCreateRequest();
			request.setName(name);
			//			request.setDescription("Description for basic tag family");
			Future<TagFamilyResponse> future = client.createTagFamily(PROJECT_NAME, request);
			latchFor(future);
			tagFamilies.put(name, future.result());
		}
	}

	private void addSchemaContainers() throws MeshSchemaException, IOException, InterruptedException {

		JsonObject schemasJson = loadJson("schemas");
		JsonArray dataArray = schemasJson.getJsonArray("data");
		// Create new schemas
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject schemaJson = dataArray.getJsonObject(i);
			String schemaName = schemaJson.getString("name");
			//			SchemaContainer container = boot.schemaContainerRoot().findByName(schemaName).toBlocking().single();
			//			if (container == null) {
			StringWriter writer = new StringWriter();
			InputStream ins = getClass().getResourceAsStream("/data/schemas/" + schemaName + ".json");
			IOUtils.copy(ins, writer, Charsets.UTF_8.name());
			Schema schema = JsonUtil.readSchema(writer.toString(), SchemaModel.class);
			Future<Schema> future = client.createSchema(schema);
			latchFor(future);
			//container = boot.schemaContainerRoot().create(schema, getAdmin());
			//			}
			Schema schemaResponse = future.result();
			schemas.put(schemaName, schemaResponse);

			// Assign the schema to all projects
			JsonArray projectsArray = schemaJson.getJsonArray("projects");
			for (int e = 0; e < projectsArray.size(); e++) {
				String projectName = projectsArray.getString(e);
				ProjectResponse project = getProject(projectName);
				Future<Schema> updateFuture = client.addSchemaToProject(schemaResponse.getUuid(), project.getUuid());
				latchFor(updateFuture);
				//project.getSchemaContainerRoot().addSchemaContainer(container);
			}
		}
	}

	//	private void updatePermissions() {
	//		db.noTrx(() -> {
	//			for (RoleResponse role : roles.values()) {
	//				for (Vertex vertex : Database.getThreadLocalGraph().getVertices()) {
	//					WrappedVertex wrappedVertex = (WrappedVertex) vertex;
	//					MeshVertex meshVertex = Database.getThreadLocalGraph().frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
	//					if (log.isTraceEnabled()) {
	//						log.trace("Granting CRUD permissions on {" + meshVertex.getElement().getId() + "} with role {" + role.getElement().getId()
	//								+ "}");
	//					}
	//					role.grantPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);
	//				}
	//				log.info("Added BasicPermissions to nodes for role {" + role.getName() + "}");
	//			}
	//			return null;
	//		});
	//
	//	}

	private JsonObject loadJson(String name) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(getClass().getResourceAsStream("/data/" + name + ".json"), writer, Charsets.UTF_8.name());
		return new JsonObject(writer.toString());
	}

	private Language getEnglish() {
		return english;
	}

	private Language getGerman() {
		return german;
	}

	private TagFamilyResponse getTagFamily(String name) {
		TagFamilyResponse tagfamily = tagFamilies.get(name);
		Objects.requireNonNull(tagfamily, "Tagfamily with name {" + name + "} could not be found.");
		return tagfamily;
	}

	private TagResponse getTag(String name) {
		TagResponse tag = tags.get(name);
		Objects.requireNonNull(tag, "Tag with name {" + name + "} could not be found.");
		return tag;
	}

	private Schema getSchemaModel(String name) {
		Schema model = schemas.get(name);
		Objects.requireNonNull(model, "Schema container with name {" + name + "} could not be found.");
		return model;
	}

	private ProjectResponse getProject(String name) {
		ProjectResponse project = projects.get(name);
		Objects.requireNonNull(project, "Project {" + name + "} could not be found.");
		return project;
	}

	private RoleResponse getRole(String name) {
		RoleResponse role = roles.get(name);
		Objects.requireNonNull(role, "Role with name {" + name + "} could not be found.");
		return role;
	}

	private GroupResponse getGroup(String name) {
		GroupResponse group = groups.get(name);
		Objects.requireNonNull(group, "Group for name {" + name + "} could not be found.");
		return group;
	}

	private UserResponse getUser(String name) {
		UserResponse user = users.get(name);
		Objects.requireNonNull(user, "User for name {" + name + "} could not be found.");
		return user;
	}

	private NodeResponse getNode(String name) {
		NodeResponse node = nodes.get(name);
		Objects.requireNonNull(node, "Node with name {" + name + "} could not be found.");
		return node;
	}

}
