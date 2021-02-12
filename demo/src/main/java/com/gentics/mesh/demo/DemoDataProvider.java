package com.gentics.mesh.demo;

import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.ODBBootstrapInitializer;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.rest.MeshLocalClient;
import com.gentics.mesh.rest.client.MeshRequest;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central data provider for the Gentics Mesh Demo content.
 * 
 * The content is created using the {@link MeshLocalClient}.
 */
@Singleton
public class DemoDataProvider {

	private static final Logger log = LoggerFactory.getLogger(DemoDataProvider.class);

	public static final String PROJECT_NAME = "demo";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	private Database db;

	private MeshLocalClient client;

	private Map<String, ProjectResponse> projects = new HashMap<>();
	private Map<String, SchemaResponse> schemas = new HashMap<>();
	private Map<String, TagFamilyResponse> tagFamilies = new HashMap<>();
	private Map<String, NodeResponse> nodes = new HashMap<>();
	private Map<String, TagResponse> tags = new HashMap<>();
	private Map<String, UserResponse> users = new HashMap<>();
	private Map<String, RoleResponse> roles = new HashMap<>();
	private Map<String, GroupResponse> groups = new HashMap<>();

	private ODBBootstrapInitializer boot;

	@Inject
	public DemoDataProvider(Database database, MeshLocalClient client, ODBBootstrapInitializer boot) {
		this.db = database;
		this.client = client;
		this.boot = boot;
	}

	/**
	 * Setup the demo content
	 * 
	 * @param syncIndex
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws MeshSchemaException
	 * @throws InterruptedException
	 */
	public void setup(boolean syncIndex) throws JsonParseException, JsonMappingException, IOException, MeshSchemaException, InterruptedException {
		MeshAuthUser user = db.tx(() -> {
			return boot.meshRoot().getUserRoot().findMeshAuthUserByUsername("admin");
		});
		client.setUser(user);

		addBootstrappedData();

		addRoles();
		addGroups();
		addUsers();

		addProjects();
		addTagFamilies();
		addTags();

		addSchemaContainers();
		addNodes();
		// updatePermissions();
		publishAllNodes();
		addWebclientPermissions();
		addAnonymousPermissions();

		if (syncIndex) {
			invokeFullIndex();
		}
		log.info("Demo data setup completed");
	}

	/**
	 * Invoke the reindex action to update the search index.
	 */
	public void invokeFullIndex() {
		boot.syncIndex();
	}

	/**
	 * Publish the basenodes for each project we created.
	 * 
	 * @throws InterruptedException
	 */
	private void publishAllNodes() throws InterruptedException {
		for (ProjectResponse project : projects.values()) {
			call(() -> client.publishNode(PROJECT_NAME, project.getRootNode().getUuid(), new PublishParametersImpl().setRecursive(true)));
		}
	}

	/**
	 * Add the anonymous read permissions
	 */
	private void addAnonymousPermissions() {
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().add(READ);
		request.getPermissions().add(READ_PUBLISHED);
		call(() -> client.updateRolePermissions(getRole("anonymous").getUuid(), "projects/" + getProject("demo").getUuid(), request));
		call(() -> client.updateRolePermissions(getRole("anonymous").getUuid(), "users/" + users.get("anonymous").getUuid(), request));
	}

	/**
	 * Add the webclient role permissions.
	 * 
	 * @throws InterruptedException
	 */
	private void addWebclientPermissions() throws InterruptedException {
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().add(READ);
		request.getPermissions().add(UPDATE);
		call(() -> client.updateRolePermissions(getRole("Client Role").getUuid(), "projects/" + getProject("demo").getUuid(), request));
		call(() -> client.updateRolePermissions(getRole("Client Role").getUuid(), "users/" + users.get("webclient").getUuid(), request));
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
			String uuid = userJson.getString("uuid");
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
			UserResponse response = call(() -> client.createUser(uuid, request));
			users.put(username, response);

			JsonArray groupArray = userJson.getJsonArray("groups");
			for (int e = 0; e < groupArray.size(); e++) {
				String groupUuid = groups.get(groupArray.getString(e)).getUuid();
				call(() -> client.addUserToGroup(groupUuid, response.getUuid()));
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
			String uuid = groupJson.getString("uuid");

			log.info("Creating group {" + name + "}");
			GroupCreateRequest groupCreateRequest = new GroupCreateRequest();
			groupCreateRequest.setName(name);
			GroupResponse group = call(() -> client.createGroup(uuid, groupCreateRequest));
			groups.put(name, group);

			JsonArray rolesNode = groupJson.getJsonArray("roles");
			for (int e = 0; e < rolesNode.size(); e++) {
				final int r = e;
				call(() -> client.addRoleToGroup(group.getUuid(), getRole(rolesNode.getString(r)).getUuid()));
			}
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
			String uuid = roleJson.getString("uuid");

			log.info("Creating role {" + name + "}");
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName(name);
			RoleResponse role = call(() -> client.createRole(uuid, request));
			roles.put(name, role);
		}
	}

	/**
	 * Add data to the internal maps which was created within the {@link BootstrapInitializer} (e.g.: admin groups, roles, users)
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void addBootstrappedData() throws InterruptedException, IOException {
		GroupListResponse groupsResponse = client.findGroups().blockingGet();
		for (GroupResponse group : groupsResponse.getData()) {
			groups.put(group.getName(), group);
		}

		UserListResponse usersResponse = client.findUsers().blockingGet();
		for (UserResponse user : usersResponse.getData()) {
			users.put(user.getUsername(), user);
		}

		RoleListResponse rolesResponse = client.findRoles().blockingGet();
		for (RoleResponse role : rolesResponse.getData()) {
			roles.put(role.getName(), role);
		}

		SchemaListResponse schemasResponse = client.findSchemas().blockingGet();
		for (SchemaResponse schema : schemasResponse.getData()) {
			schemas.put(schema.getName(), schema);
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
			String uuid = nodeJson.getString("uuid");
			ProjectResponse project = getProject(nodeJson.getString("project"));
			String schemaName = nodeJson.getString("schema");
			String parentNodeName = nodeJson.getString("parent");
			String segmentFieldValue = nodeJson.getString("segmentFieldValue");
			String name = nodeJson.getString("name");
			SchemaResponse schema = getSchemaModel(schemaName);
			NodeResponse parentNode = (nodeJson.getString("project") + ".basenode").equals(parentNodeName) ? null : getNode(parentNodeName);

			log.info("Creating node {" + name + "} for schema {" + schemaName + "}");
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setLanguage("en");
			if (parentNode != null) {
				nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNode.getUuid()));
			} else {
				nodeCreateRequest.setParentNode(project.getRootNode());
			}
			nodeCreateRequest.setSchema(new SchemaReferenceImpl().setUuid(schema.getUuid()));
			nodeCreateRequest.getFields().put("name", FieldUtil.createStringField(name));

			// Add the segment field value
			switch (schemaName) {
			case "category":
			case "folder":
			case "vehicle":
				nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField(segmentFieldValue));
				break;
			}

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
			NodeResponse createdNode = call(() -> client.createNode(uuid, project.getName(), nodeCreateRequest));
			System.out.println("UUID: " + uuid + " - " + createdNode.getUuid());

			// Upload binary data
			JsonObject binNode = nodeJson.getJsonObject("bin");
			if (binNode != null) {
				String path = binNode.getString("path");
				String filenName = binNode.getString("filename");
				String contentType = binNode.getString("contentType");
				InputStream ins = getClass().getResourceAsStream("/data/" + path);
				if (ins == null) {
					throw new NullPointerException("Could not find binary file within path {" + path + "}");
				}
				byte[] bytes = IOUtils.toByteArray(ins);
				Buffer fileData = Buffer.buffer(bytes);

				NodeResponse resp = call(
					() -> client.updateNodeBinaryField(PROJECT_NAME, createdNode.getUuid(), "en", createdNode.getVersion().toString(), "image",
						fileData.getBytes(), filenName, contentType));

				Float fpx = binNode.getFloat("fpx");
				Float fpy = binNode.getFloat("fpy");
				if (fpx != null && fpy != null) {
					NodeUpdateRequest update = resp.toRequest();
					BinaryField image = update.getFields().getBinaryField("image");
					image.setFocalPoint(fpx, fpy);
					update.getFields().put("image", image);
					call(() -> client.updateNode(PROJECT_NAME, createdNode.getUuid(), update));
				}
			}

			// Add tags to node
			JsonArray tagArray = nodeJson.getJsonArray("tags");
			for (int e = 0; e < tagArray.size(); e++) {
				String tagName = tagArray.getString(e);
				call(() -> client.addTagToNode(PROJECT_NAME, createdNode.getUuid(), getTag(tagName).getUuid()));
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
			String uuid = tagJson.getString("uuid");
			String tagFamilyName = tagJson.getString("tagFamily");

			log.info("Creating tag {" + name + "} to family {" + tagFamilyName + "}");
			TagFamilyResponse tagFamily = getTagFamily(tagFamilyName);
			// TODO determine project of tag family automatically or use json field to assign it
			TagCreateRequest createRequest = new TagCreateRequest();
			createRequest.setName(name);
			TagResponse result = call(() -> client.createTag(PROJECT_NAME, tagFamily.getUuid(), uuid, createRequest));
			tags.put(name, result);
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
			String uuid = projectJson.getString("uuid");

			log.info("Creating project {" + name + "}");
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			request.setName(name);
			ProjectResponse project = call(() -> client.createProject(uuid, request));
			projects.put(name, project);
		}
	}

	private void addTagFamilies() throws IOException, InterruptedException {
		JsonObject tagfamilyJson = loadJson("tagfamilies");
		JsonArray dataArray = tagfamilyJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagFamilyJson = dataArray.getJsonObject(i);
			String uuid = tagFamilyJson.getString("uuid");
			String name = tagFamilyJson.getString("name");
			String projectName = tagFamilyJson.getString("project");

			log.info("Creating tagfamily {" + name + "} for project {" + projectName + "}");
			TagFamilyCreateRequest request = new TagFamilyCreateRequest();
			request.setName(name);
			TagFamilyResponse result = call(() -> client.createTagFamily(PROJECT_NAME, uuid, request));
			tagFamilies.put(name, result);
		}
	}

	private void addSchemaContainers() throws MeshSchemaException, IOException, InterruptedException {

		JsonObject schemasJson = loadJson("schemas");
		JsonArray dataArray = schemasJson.getJsonArray("data");
		// Create new schemas
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject schemaJson = dataArray.getJsonObject(i);
			String schemaName = schemaJson.getString("name");
			String uuid = schemaJson.getString("uuid");
			StringWriter writer = new StringWriter();
			InputStream ins = getClass().getResourceAsStream("/data/schemas/" + schemaName + ".json");
			if (ins != null) {
				IOUtils.copy(ins, writer, Charsets.UTF_8.name());
				SchemaCreateRequest schema = JsonUtil.readValue(writer.toString(), SchemaCreateRequest.class);
				SchemaResponse schemaResponse = call(() -> client.createSchema(uuid, schema));
				schemas.put(schemaName, schemaResponse);
			}

			// Assign all schemas to all projects
			JsonArray projectsArray = schemaJson.getJsonArray("projects");
			for (int e = 0; e < projectsArray.size(); e++) {
				String projectName = projectsArray.getString(e);
				ProjectResponse project = getProject(projectName);
				for (SchemaResponse schema : schemas.values()) {
					call(() -> client.assignSchemaToProject(project.getName(), schema.getUuid()));
				}
			}

		}
	}

	protected <T> T call(ClientHandler<T> handler) {
		try {
			return handler.handle().blockingGet();
		} catch (Exception e) {
			throw new RuntimeException("Error while handling request.", e);
		}
	}

	private JsonObject loadJson(String name) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(getClass().getResourceAsStream("/data/" + name + ".json"), writer, Charsets.UTF_8.name());
		return new JsonObject(writer.toString());
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

	private SchemaResponse getSchemaModel(String name) {
		SchemaResponse model = schemas.get(name);
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

	private NodeResponse getNode(String name) {
		NodeResponse node = nodes.get(name);
		Objects.requireNonNull(node, "Node with name {" + name + "} could not be found.");
		return node;
	}

	@FunctionalInterface
	protected static interface ClientHandler<T> {
		MeshRequest<T> handle() throws Exception;
	}

}
