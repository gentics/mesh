package com.gentics.mesh.demo;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class DemoDataProvider {

	private static final Logger log = LoggerFactory.getLogger(DemoDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	@Autowired
	private Database db;

	@Autowired
	private BootstrapInitializer boot;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	private BootstrapInitializer bootstrapInitializer;

	private Language english;

	private Language german;

	private MeshRoot root;

	private Map<String, Project> projects = new HashMap<>();
	private Map<String, SchemaContainer> schemaContainers = new HashMap<>();
	private Map<String, TagFamily> tagFamilies = new HashMap<>();
	private Map<String, Node> nodes = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, User> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	private DemoDataProvider() {
	}

	public void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException, InterruptedException {
		long start = System.currentTimeMillis();

		db.noTrx(() -> {
			bootstrapInitializer.initMandatoryData();

			root = boot.meshRoot();
			english = boot.languageRoot().findByLanguageTag("en");
			german = boot.languageRoot().findByLanguageTag("de");

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
		updatePermissions();
		invokeFullIndex();
		long duration = System.currentTimeMillis() - start;
	}

	private void invokeFullIndex() throws InterruptedException {
		db.noTrx(() -> {
			boot.meshRoot().getSearchQueue().addFullIndex();
			boot.meshRoot().getSearchQueue().processAll();
			return null;
		});
	}

	/**
	 * Load users JSON file and create users.
	 * 
	 * @throws IOException
	 */
	private void addUsers() throws IOException {
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
			User user = root.getUserRoot().create(username, null);
			user.setPassword(password);
			user.setFirstname(firstname);
			user.setLastname(lastname);
			user.setEmailAddress(email);
			users.put(username, user);

			JsonArray groupArray = userJson.getJsonArray("groups");
			for (int e = 0; e < groupArray.size(); e++) {
				user.addGroup(getGroup(groupArray.getString(e)));
			}
		}

	}

	/**
	 * Add groups from JSON file to graph.
	 * 
	 * @throws IOException
	 */
	private void addGroups() throws IOException {
		JsonObject groupsJson = loadJson("groups");
		JsonArray dataArray = groupsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject groupJson = dataArray.getJsonObject(i);
			String name = groupJson.getString("name");

			log.info("Creating group {" + name + "}");
			Group group = root.getGroupRoot().create(name, getAdmin());

			JsonArray rolesNode = groupJson.getJsonArray("roles");
			for (int e = 0; e < rolesNode.size(); e++) {
				group.addRole(getRole(rolesNode.getString(e)));
			}
			groups.put(name, group);
		}
	}

	/**
	 * Load roles JSON file and add those roles to the graph.
	 * 
	 * @throws IOException
	 */
	private void addRoles() throws IOException {
		JsonObject rolesJson = loadJson("roles");
		JsonArray dataArray = rolesJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject roleJson = dataArray.getJsonObject(i);
			String name = roleJson.getString("name");

			log.info("Creating role {" + name + "}");
			Role role = root.getRoleRoot().create(name, getAdmin());
			role.grantPermissions(role, READ_PERM);
			roles.put(name, role);
		}
	}

	/**
	 * Add data to the internal maps which was created within the {@link BootstrapInitializer} (eg. admin groups, roles, users)
	 */
	private void addBootstrappedData() {
		english = root.getLanguageRoot().findByLanguageTag("en");
		german = root.getLanguageRoot().findByLanguageTag("de");
		for (Group group : root.getGroupRoot().findAll()) {
			groups.put(group.getName(), group);
		}
		for (User user : root.getUserRoot().findAll()) {
			users.put(user.getUsername(), user);
		}
		for (Role role : root.getRoleRoot().findAll()) {
			roles.put(role.getName(), role);
		}
	}

	/**
	 * Load nodes JSON file and add those nodes to the graph.
	 * 
	 * @throws IOException
	 */
	private void addNodes() throws IOException {
		JsonObject nodesJson = loadJson("nodes");
		JsonArray dataArray = nodesJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject nodeJson = dataArray.getJsonObject(i);
			Project project = getProject(nodeJson.getString("project"));
			String schemaName = nodeJson.getString("schema");
			String name = nodeJson.getString("name");
			String parentNodeName = nodeJson.getString("parent");
			SchemaContainer schema = getSchemaContainer(schemaName);
			Node parentNode = getNode(parentNodeName);

			log.info("Creating node {" + name + "} for schema {" + schemaName + "}");
			Node node = parentNode.create(getAdmin(), schema, project);

			JsonArray tagArray = nodeJson.getJsonArray("tags");
			for (int e = 0; e < tagArray.size(); e++) {
				String tagName = tagArray.getString(e);
				node.addTag(getTag(tagName));
			}
			NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
			englishContainer.createString("name").setString(name);

			JsonObject binNode = nodeJson.getJsonObject("bin");
			if (binNode != null) {
				String path = binNode.getString("path");
				int height = binNode.getInteger("height");
				int width = binNode.getInteger("width");
				String sha512sum = binNode.getString("sha512sum");
				String filenName = binNode.getString("filename");
				String contentType = binNode.getString("contentType");
				InputStream ins = getClass().getResourceAsStream("/data/" + path);
				if (ins == null) {
					throw new NullPointerException("Could not find binary file within path {" + path + "}");
				}

				BinaryGraphField binaryField = englishContainer.createBinary("binary");

				File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), binaryField.getSegmentedPath());
				folder.mkdirs();
				File outputFile = new File(folder, node.getUuid() + ".bin");
				if (!outputFile.exists()) {
					IOUtils.copy(ins, new FileOutputStream(outputFile));
				}
				binaryField.setImageWidth(width);
				binaryField.setImageHeight(height);
				binaryField.setMimeType(contentType);
				binaryField.setImageDPI(300);
				binaryField.setFileName(filenName);
				binaryField.setSHA512Sum(sha512sum);
				binaryField.setFileSize(outputFile.length());
			}

			JsonObject fieldsObject = nodeJson.getJsonObject("fields");
			if (fieldsObject != null) {
				for (String fieldName : fieldsObject.fieldNames()) {
					Object obj = fieldsObject.getValue(fieldName);
					if ("vehicleImage".equals(fieldName)) {
						englishContainer.createNode(fieldName, getNode((String) obj));
						continue;
					}
					if ("description".equals(fieldName)) {
						englishContainer.createHTML(fieldName).setHtml(String.valueOf(obj));
						continue;
					}
					if (obj instanceof Integer || obj instanceof Float || obj instanceof Double) {
						englishContainer.createNumber(fieldName).setNumber(com.gentics.mesh.util.NumberUtils.createNumber(obj.toString()));
					} else if (obj instanceof String) {
						englishContainer.createString(fieldName).setString((String) obj);
					} else {
						throw new RuntimeException("Demo data type {" + obj.getClass().getName() + "} for field {" + fieldName + "} is unknown.");
					}
				}
			}
			nodes.put(name, node);
		}

	}

	/**
	 * Load tags JSON and add those tags to the graph.
	 * 
	 * @throws IOException
	 */
	private void addTags() throws IOException {
		JsonObject tagsJson = loadJson("tags");
		JsonArray dataArray = tagsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagJson = dataArray.getJsonObject(i);
			String name = tagJson.getString("name");
			String tagFamilyName = tagJson.getString("tagFamily");

			log.info("Creating tag {" + name + "} to family {" + tagFamilyName + "}");
			TagFamily tagFamily = getTagFamily(tagFamilyName);
			// TODO determine project of tag family automatically or use json field to assign it
			Tag tag = tagFamily.create(name, getProject("demo"), getAdmin());
			tags.put(name, tag);
		}
	}

	/**
	 * Load project JSON file and add those projects to the graph.
	 * 
	 * @throws IOException
	 */
	private void addProjects() throws IOException {
		JsonObject projectsJson = loadJson("projects");
		JsonArray dataArray = projectsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject projectJson = dataArray.getJsonObject(i);
			String name = projectJson.getString("name");

			log.info("Creating project {" + name + "}");
			Project project = root.getProjectRoot().create(name, getAdmin());
			project.addLanguage(getEnglish());
			project.addLanguage(getGerman());
			Node baseNode = project.getBaseNode();
			nodes.put(name + ".basenode", baseNode);
			projects.put(name, project);
		}
	}

	private void addTagFamilies() throws IOException {
		JsonObject tagfamilyJson = loadJson("tagfamilies");
		JsonArray dataArray = tagfamilyJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagFamilyJson = dataArray.getJsonObject(i);
			String name = tagFamilyJson.getString("name");
			String projectName = tagFamilyJson.getString("project");

			log.info("Creating tagfamily {" + name + "} for project {" + projectName + "}");
			TagFamily tagFamily = getProject(projectName).getTagFamilyRoot().create(name, getAdmin());
			tagFamily.setDescription("Description for basic tag family");
			tagFamilies.put(name, tagFamily);
		}
	}

	private void addSchemaContainers() throws MeshSchemaException, IOException {

		JsonObject schemasJson = loadJson("schemas");
		JsonArray dataArray = schemasJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject schemaJson = dataArray.getJsonObject(i);
			String schemaName = schemaJson.getString("name");
			SchemaContainer container = boot.schemaContainerRoot().findByName(schemaName).toBlocking().single();
			if (container == null) {
				StringWriter writer = new StringWriter();
				InputStream ins = getClass().getResourceAsStream("/data/schemas/" + schemaName + ".json");
				IOUtils.copy(ins, writer, Charsets.UTF_8.name());
				Schema schema = JsonUtil.readSchema(writer.toString(), SchemaImpl.class);
				container = boot.schemaContainerRoot().create(schema, getAdmin());
			}
			schemaContainers.put(schemaName, container);

			JsonArray projectsArray = schemaJson.getJsonArray("projects");
			for (int e = 0; e < projectsArray.size(); e++) {
				String projectName = projectsArray.getString(e);
				Project project = getProject((String) projectName);
				project.getSchemaContainerRoot().addSchemaContainer(container);
			}
		}
	}

	private void updatePermissions() {
		db.noTrx(() -> {
			for (Role role : roles.values()) {
				for (Vertex vertex : Database.getThreadLocalGraph().getVertices()) {
					WrappedVertex wrappedVertex = (WrappedVertex) vertex;
					MeshVertex meshVertex = Database.getThreadLocalGraph().frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
					if (log.isTraceEnabled()) {
						log.trace("Granting CRUD permissions on {" + meshVertex.getElement().getId() + "} with role {" + role.getElement().getId()
								+ "}");
					}
					role.grantPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);
				}
				log.info("Added BasicPermissions to nodes for role {" + role.getName() + "}");
			}
			return null;
		});

	}

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

	private TagFamily getTagFamily(String name) {
		TagFamily tagfamily = tagFamilies.get(name);
		Objects.requireNonNull(tagfamily, "Tagfamily with name {" + name + "} could not be found.");
		return tagfamily;
	}

	private Tag getTag(String name) {
		Tag tag = tags.get(name);
		Objects.requireNonNull(tag, "Tag with name {" + name + "} could not be found.");
		return tag;
	}

	private SchemaContainer getSchemaContainer(String name) {
		SchemaContainer container = schemaContainers.get(name);
		Objects.requireNonNull(container, "Schema container with name {" + name + "} could not be found.");
		return container;
	}

	private Project getProject(String name) {
		Project project = projects.get(name);
		Objects.requireNonNull(project, "Project {" + name + "} could not be found.");
		return project;
	}

	private User getAdmin() {
		User admin = getUser("admin");
		Objects.requireNonNull(admin, "Admin user could not be found.");
		return admin;
	}

	private Role getRole(String name) {
		Role role = roles.get(name);
		Objects.requireNonNull(role, "Role with name {" + name + "} could not be found.");
		return role;
	}

	private Group getGroup(String name) {
		Group group = groups.get(name);
		Objects.requireNonNull(group, "Group for name {" + name + "} could not be found.");
		return group;
	}

	private User getUser(String name) {
		User user = users.get(name);
		Objects.requireNonNull(user, "User for name {" + name + "} could not be found.");
		return user;
	}

	private Node getNode(String name) {
		Node node = nodes.get(name);
		Objects.requireNonNull(node, "Node with name {" + name + "} could not be found.");
		return node;
	}

}
