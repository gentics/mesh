package com.gentics.mesh.demo;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
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
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
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
	private BootstrapInitializer rootService;

	@Autowired
	protected MeshSpringConfiguration springConfig;

	@Autowired
	private BootstrapInitializer bootstrapInitializer;

	// References to dummy data

	private Language english;

	private Language german;

	private UserInfo userInfo;

	private MeshRoot root;

	private Map<String, Project> projects = new HashMap<>();
	private Map<String, SchemaContainer> schemaContainers = new HashMap<>();
	private Map<String, TagFamily> tagFamilies = new HashMap<>();
	private Map<String, Node> folders = new HashMap<>();
	private Map<String, Node> contents = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, User> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	private DemoDataProvider() {
	}

	public void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		long start = System.currentTimeMillis();

		db.noTrx(noTrx -> {
			bootstrapInitializer.initMandatoryData();

			root = rootService.meshRoot();
			english = rootService.languageRoot().findByLanguageTag("en");
			german = rootService.languageRoot().findByLanguageTag("de");

			addBootstrappedData();
			//			addUserGroupRoleProject();
			addRoles();
			addGroups();
			addUsers();
			addProjects();

			addTagFamilies();
			addTags();

			addSchemaContainers();
			addNodes();

		});
		updatePermissions();
		long duration = System.currentTimeMillis() - start;
		log.info("Setup took: {" + duration + "}");
	}

	private void addUsers() throws IOException {
		JsonObject usersJson = loadJson("users");
		JsonArray dataArray = usersJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject userJson = dataArray.getJsonObject(i);
		}

	}

	private void addGroups() throws IOException {
		JsonObject groupsJson = loadJson("groups");
		JsonArray dataArray = groupsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject groupJson = dataArray.getJsonObject(i);
		}
	}

	private void addRoles() throws IOException {
		JsonObject rolesJson = loadJson("roles");
		JsonArray dataArray = rolesJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject roleJson = dataArray.getJsonObject(i);
		}
	}

	private void updatePermissions() {
		db.noTrx(tc -> {
			Role role = userInfo.getRole();
			for (Vertex vertex : Database.getThreadLocalGraph().getVertices()) {
				WrappedVertex wrappedVertex = (WrappedVertex) vertex;
				MeshVertex meshVertex = Database.getThreadLocalGraph().frameElement(wrappedVertex.getBaseElement(), MeshVertexImpl.class);
				if (log.isTraceEnabled()) {
					log.trace("Granting CRUD permissions on {" + meshVertex.getElement().getId() + "} with role {" + role.getElement().getId() + "}");
				}
				role.grantPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);
			}
		});
		log.info("Added BasicPermissions to nodes");

	}

	/**
	 * Add data to the internal maps which was created within the {@link BootstrapInitializer} (eg. admin groups, roles, users)
	 */
	private void addBootstrappedData() {
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

	private void addNodes() throws IOException {
		JsonObject nodesJson = loadJson("nodes");
		JsonArray dataArray = nodesJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject nodeJson = dataArray.getJsonObject(i);
			Project project = getProject(nodeJson.getString("project"));
			SchemaContainer schema = schemaContainers.get(nodeJson.getString("schema"));
			if ("content".equalsIgnoreCase(schema.getName())) {
				Node content = addContent(folders.get("2015"), "News_2015", "News!", "Neuigkeiten!", schema, project);
				content.addTag(tags.get("red"));
			}
			if ("folder".equalsIgnoreCase(schema.getName())) {
				//				Node folder = addFolder(rootNode, englishName, germanName);
				//				folder.addTag(tags.get("red"));
			}
		}

	}

	private void addTags() throws IOException {
		JsonObject tagsJson = loadJson("tags");
		JsonArray dataArray = tagsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagJson = dataArray.getJsonObject(i);
			TagFamily tagFamily = tagFamilies.get(tagJson.getString("tagFamily"));
			addTag(tagJson.getString("name"), tagFamily);
		}
	}

	private JsonObject loadJson(String name) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(getClass().getResourceAsStream("/data/" + name + ".json"), writer, Charsets.UTF_8.name());
		return new JsonObject(writer.toString());
	}

	private void addProjects() throws IOException {
		JsonObject projectsJson = loadJson("projects");
		JsonArray dataArray = projectsJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject projectJson = dataArray.getJsonObject(i);
			String name = projectJson.getString("name");
			Project project = root.getProjectRoot().create(name, userInfo.getUser());
			project.addLanguage(getEnglish());
			project.addLanguage(getGerman());
			projects.put(name, project);

		}
	}

	private void addTagFamilies() throws IOException {
		JsonObject tagfamilyJson = loadJson("tagfamilies");
		JsonArray dataArray = tagfamilyJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject tagFamilyJson = dataArray.getJsonObject(i);
			String name = tagFamilyJson.getString("name");
			String projectName = tagFamilyJson.getString("projectName");

			TagFamily tagFamily = getProject(projectName).getTagFamilyRoot().create(name, userInfo.getUser());
			tagFamily.setDescription("Description for basic tag family");
		}

	}

	private void addSchemaContainers() throws MeshSchemaException, IOException {
		addBootstrapSchemas();

		JsonObject schemasJson = loadJson("schemas");
		JsonArray dataArray = schemasJson.getJsonArray("data");
		for (int i = 0; i < dataArray.size(); i++) {
			JsonObject schemaJson = dataArray.getJsonObject(i);

			Schema schema = new SchemaImpl();
			schema.setName("blogpost");
			schema.setDisplayField("title");
			schema.setMeshVersion(Mesh.getVersion());

			StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
			titleFieldSchema.setName("title");
			titleFieldSchema.setLabel("Title");
			schema.addField(titleFieldSchema);

			HtmlFieldSchema contentFieldSchema = new HtmlFieldSchemaImpl();
			titleFieldSchema.setName("content");
			titleFieldSchema.setLabel("Content");
			schema.addField(contentFieldSchema);

			SchemaContainerRoot schemaRoot = root.getSchemaContainerRoot();
			SchemaContainer blogPostSchemaContainer = schemaRoot.create(schema, getUserInfo().getUser());
			blogPostSchemaContainer.setSchema(schema);

		}

	}

	private void addBootstrapSchemas() {

		//		// folder
		//		SchemaContainer folderSchemaContainer = rootService.schemaContainerRoot().findByName("folder");
		//		project.getSchemaContainerRoot().addSchemaContainer(folderSchemaContainer);
		//
		//		// content
		//		SchemaContainer contentSchemaContainer = rootService.schemaContainerRoot().findByName("content");
		//		project.getSchemaContainerRoot().addSchemaContainer(contentSchemaContainer);
		//
		//		// binary-content
		//		SchemaContainer binaryContentSchemaContainer = rootService.schemaContainerRoot().findByName("binary-content");
		//		project.getSchemaContainerRoot().addSchemaContainer(binaryContentSchemaContainer);

	}

	public Node addFolder(Node rootNode, String englishName, String germanName, Project project) {
		Node folderNode = rootNode.create(userInfo.getUser(), schemaContainers.get("folder"), project);

		if (germanName != null) {
			NodeGraphFieldContainer germanContainer = folderNode.getOrCreateGraphFieldContainer(german);
			// germanContainer.createString("displayName").setString(germanName);
			germanContainer.createString("name").setString(germanName);
		}
		if (englishName != null) {
			NodeGraphFieldContainer englishContainer = folderNode.getOrCreateGraphFieldContainer(english);
			// englishContainer.createString("displayName").setString(englishName);
			englishContainer.createString("name").setString(englishName);
		}

		if (englishName == null || StringUtils.isEmpty(englishName)) {
			throw new RuntimeException("Key for folder empty");
		}
		if (folders.containsKey(englishName.toLowerCase())) {
			throw new RuntimeException("Collision of folders detected for key " + englishName.toLowerCase());
		}

		folders.put(englishName.toLowerCase(), folderNode);
		return folderNode;
	}

	private void setCreatorEditor(GenericVertex<?> node) {
		node.setCreator(userInfo.getUser());
		node.setCreationTimestamp(System.currentTimeMillis());

		node.setEditor(userInfo.getUser());
		node.setLastEditedTimestamp(System.currentTimeMillis());
	}

	public Tag addTag(String name, TagFamily tagFamily) {
		if (name == null || StringUtils.isEmpty(name)) {
			throw new RuntimeException("Name for tag empty");
		}
		Tag tag = tagFamily.create(name, userInfo.getUser());
		setCreatorEditor(tag);
		tags.put(name.toLowerCase(), tag);
		return tag;
	}

	private Node addContent(Node parentNode, String name, String englishContent, String germanContent, SchemaContainer schema, Project project) {
		Node node = parentNode.create(userInfo.getUser(), schemaContainers.get("content"), project);
		if (englishContent != null) {
			NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
			englishContainer.createString("name").setString(name + " english name");
			englishContainer.createString("title").setString(name + " english title");
			englishContainer.createString("displayName").setString(name + " english displayName");
			englishContainer.createString("filename").setString(name + ".en.html");
			englishContainer.createHTML("content").setHtml(englishContent);
		}

		if (germanContent != null) {
			NodeGraphFieldContainer germanContainer = node.getOrCreateGraphFieldContainer(german);
			germanContainer.createString("name").setString(name + " german");
			germanContainer.createString("title").setString(name + " english title");
			germanContainer.createString("displayName").setString(name + " german");
			germanContainer.createString("filename").setString(name + ".de.html");
			germanContainer.createHTML("content").setHtml(germanContent);
		}

		if (contents.containsKey(name.toLowerCase())) {
			throw new RuntimeException("Collsion of contents detected for key " + name.toLowerCase());
		}
		contents.put(name.toLowerCase(), node);
		return node;
	}

	/**
	 * Returns the path to the tag for the given language.
	 */
	public String getPathForNews2015Tag(Language language) {

		String name = folders.get("news").getGraphFieldContainer(language).getString("name").getString();
		String name2 = folders.get("2015").getGraphFieldContainer(language).getString("name").getString();
		return name + "/" + name2;
	}

	public Language getEnglish() {
		return english;
	}

	public Language getGerman() {
		return german;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	public Node getFolder(String name) {
		return folders.get(name);
	}

	public TagFamily getTagFamily(String key) {
		return tagFamilies.get(key);
	}

	public Node getContent(String name) {
		return contents.get(name);
	}

	public Tag getTag(String name) {
		return tags.get(name);
	}

	public SchemaContainer getSchemaContainer(String name) {
		return schemaContainers.get(name);
	}

	public Map<String, Tag> getTags() {
		return tags;
	}

	public Map<String, Node> getContents() {
		return contents;
	}

	public Map<String, Node> getFolders() {
		return folders;
	}

	public Map<String, User> getUsers() {
		return users;
	}

	public Map<String, Group> getGroups() {
		return groups;
	}

	public Map<String, Role> getRoles() {
		return roles;
	}

	public Map<String, SchemaContainer> getSchemaContainers() {
		return schemaContainers;
	}

	public MeshRoot getMeshRoot() {
		return root;
	}

	public Map<String, Project> getProjects() {
		return projects;
	}

	private Project getProject(String projectName) {
		return getProjects().get(projectName);
	}

}
