package com.gentics.mesh.demo;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.UserRoot;
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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class TestDataProvider {

	private static final Logger log = LoggerFactory.getLogger(TestDataProvider.class);

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

	private Project project;

	private UserInfo userInfo;

	private MeshRoot root;

	private Map<String, SchemaContainer> schemaContainers = new HashMap<>();
	private Map<String, TagFamily> tagFamilies = new HashMap<>();
	private Map<String, Node> folders = new HashMap<>();
	private Map<String, Node> contents = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, User> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	private TestDataProvider() {
	}

	public void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		long start = System.currentTimeMillis();

		db.noTrx(noTrx -> {
			bootstrapInitializer.initMandatoryData();
			schemaContainers.clear();
			tagFamilies.clear();
			contents.clear();
			folders.clear();
			tags.clear();
			users.clear();
			roles.clear();
			groups.clear();

			root = rootService.meshRoot();
			english = rootService.languageRoot().findByLanguageTag("en");
			german = rootService.languageRoot().findByLanguageTag("de");

			addBootstrappedData();
			addUserGroupRoleProject();
			addSchemaContainers();

			addTagFamilies();
			addTags();
			addFolderStructure();
			addContents();

			log.info("Nodes:    " + getNodeCount());
			log.info("Folders:  " + folders.size());
			log.info("Contents: " + contents.size());
			log.info("Tags:     " + tags.size());
			log.info("Schemas: " + schemaContainers.size());
			log.info("TagFamilies: " + tagFamilies.size());
			log.info("Users:    " + users.size());
			log.info("Groups:   " + groups.size());
			log.info("Roles:    " + roles.size());
		});
		updatePermissions();
		long duration = System.currentTimeMillis() - start;
		log.info("Setup took: {" + duration + "}");
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

	private void addContents() {

		SchemaContainer contentSchema = schemaContainers.get("content");

		addContent(folders.get("2014"), "News_2014", "News!", "Neuigkeiten!", contentSchema);

		addContent(folders.get("news"), "News Overview", "News Overview", "News Übersicht", contentSchema);

		addContent(folders.get("deals"), "Super Special Deal 2015", "Buy two get nine!", "Kauf zwei und nimm neun mit!", contentSchema);
		addContent(folders.get("deals"), "Special Deal June 2015", "Buy two get three!", "Kauf zwei und nimm drei mit!", contentSchema);

		addContent(folders.get("2015"), "Special News_2014", "News!", "Neuigkeiten!", contentSchema);
		addContent(folders.get("2015"), "News_2015", "News!", "Neuigkeiten!", contentSchema);

		// Node porsche911 = addContent(folders.get("products"), "Porsche 911",
		// "997 is the internal designation for the Porsche 911 model manufactured and sold by German manufacturer Porsche between 2004 (as Model Year 2005) and
		// 2012.",
		// "Porsche 997 ist die interne Modellbezeichnung von Porsche für das von 2004 bis Ende 2012 produzierte 911-Modell.", contentSchema);
		// porsche911.addTag(tags.get("vehicle"));
		// porsche911.addTag(tags.get("car"));
		//
		// Node nissanGTR = addContent(folders.get("products"), "Nissan GT-R",
		// "The Nissan GT-R is a 2-door 2+2 sports coupé produced by Nissan and first released in Japan in 2007",
		// "Der Nissan GT-R ist ein seit Dezember 2007 produziertes Sportcoupé des japanischen Automobilherstellers Nissan und der Nachfolger des Nissan Skyline
		// GT-R R34.",
		// contentSchema);
		// nissanGTR.addTag(tags.get("vehicle"));
		// nissanGTR.addTag(tags.get("car"));
		// nissanGTR.addTag(tags.get("green"));

		// Node bmwM3 = addContent(folders.get("products"), "BMW M3",
		// "The BMW M3 (first launched in 1986) is a high-performance version of the BMW 3-Series, developed by BMW's in-house motorsport division, BMW M.",
		// "Der BMW M3 ist ein Sportmodell der 3er-Reihe von BMW, das seit Anfang 1986 hergestellt wird. Dabei handelt es sich um ein Fahrzeug, welches von der
		// BMW-Tochterfirma BMW M GmbH entwickelt und anfangs (E30 und E36) auch produziert wurde.",
		// contentSchema);
		// bmwM3.addTag(tags.get("vehicle"));
		// bmwM3.addTag(tags.get("car"));
		// bmwM3.addTag(tags.get("blue"));

		Node concorde = addContent(folders.get("products"), "Concorde",
				"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
				"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.",
				contentSchema);
		concorde.addTag(tags.get("plane"));
		concorde.addTag(tags.get("twinjet"));
		concorde.addTag(tags.get("red"));

		// Node boeing737 = addContent(folders.get("products"), "Boeing 737",
		// "The Boeing 737 is a short- to medium-range twinjet narrow-body airliner. Originally developed as a shorter, lower-cost twin-engined airliner derived
		// from Boeing's 707 and 727, the 737 has developed into a family of nine passenger models with a capacity of 85 to 215 passengers.",
		// "Die Boeing 737 des US-amerikanischen Flugzeugherstellers Boeing ist die weltweit meistgebaute Familie strahlgetriebener Verkehrsflugzeuge.",
		// contentSchema);
		// boeing737.addTag(tags.get("plane"));
		// boeing737.addTag(tags.get("twinjet"));

		// Node a300 = addContent(folders.get("products"), "Airbus A300",
		// "The Airbus A300 is a short- to medium-range wide-body twin-engine jet airliner that was developed and manufactured by Airbus. Released in 1972 as
		// the world's first twin-engined widebody, it was the first product of Airbus Industrie, a consortium of European aerospace manufacturers, now a
		// subsidiary of Airbus Group.",
		// "Der Airbus A300 ist das erste zweistrahlige Großraumflugzeug der Welt, produziert vom europäischen Flugzeughersteller Airbus.",
		// contentSchema);
		// a300.addTag(tags.get("plane"));
		// a300.addTag(tags.get("twinjet"));
		// a300.addTag(tags.get("red"));

		// Node wrangler = addContent(folders.get("products"), "Jeep Wrangler",
		// "The Jeep Wrangler is a compact and mid-size (Wrangler Unlimited models) four-wheel drive off-road and sport utility vehicle (SUV), manufactured by
		// American automaker Chrysler, under its Jeep marque – and currently in its third generation.",
		// "Der Jeep Wrangler ist ein Geländewagen des US-amerikanischen Herstellers Jeep innerhalb des Chrysler-Konzerns.", contentSchema);
		// wrangler.addTag(tags.get("vehicle"));
		// wrangler.addTag(tags.get("jeep"));
		//
		// Node volvo = addContent(folders.get("products"), "Volvo B10M",
		// "The Volvo B10M was a mid-engined bus and coach chassis manufactured by Volvo between 1978 and 2003.", null, contentSchema);
		// volvo.addTag(tags.get("vehicle"));
		// volvo.addTag(tags.get("bus"));
		//
		// Node hondact90 = addContent(folders.get("products"), "Honda CT90",
		// "The Honda CT90 was a small step-through motorcycle manufactured by Honda from 1966 to 1979.", null, contentSchema);
		// hondact90.addTag(tags.get("vehicle"));
		// hondact90.addTag(tags.get("motorcycle"));

		Node hondaNR = addContent(folders.get("products"), "Honda NR",
				"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
				"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.",
				contentSchema);
		hondaNR.addTag(tags.get("vehicle"));
		hondaNR.addTag(tags.get("motorcycle"));
		hondaNR.addTag(tags.get("green"));

	}

	private void addFolderStructure() {

		Node baseNode = project.getBaseNode();
		// rootNode.addProject(project);

		Node news = addFolder(baseNode, "News", "Neuigkeiten");
		Node news2015 = addFolder(news, "2015", null);
		news2015.addTag(tags.get("car"));
		news2015.addTag(tags.get("bike"));
		news2015.addTag(tags.get("plane"));
		news2015.addTag(tags.get("jeep"));

		Node news2014 = addFolder(news, "2014", null);
		addFolder(news2014, "March", null);

		addFolder(baseNode, "Products", "Produkte");
		addFolder(baseNode, "Deals", "Angebote");

	}

	private void addTags() {

		TagFamily colorTags = tagFamilies.get("colors");
		TagFamily basicTags = tagFamilies.get("basic");

		// Tags for categories
		addTag("Vehicle", basicTags);
		addTag("Car", basicTags);
		addTag("Jeep", basicTags);
		addTag("Bike", basicTags);
		addTag("Motorcycle", basicTags);
		addTag("Bus", basicTags);
		addTag("Plane", basicTags);
		addTag("JetFigther", basicTags);
		addTag("Twinjet", basicTags);

		// Tags for colors
		addTag("red", colorTags);
		addTag("blue", colorTags);
		addTag("green", colorTags);

	}

	public UserInfo createUserInfo(String username, String firstname, String lastname) {

		String password = "test123";
		log.info("Creating user with username: " + username + " and password: " + password);

		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";
		User user = root.getUserRoot().create(username, null);
		user.setUuid("UUIDOFUSER1");
		user.setPassword(password);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmailAddress(email);

		user.setCreator(user);
		user.setCreationTimestamp(System.currentTimeMillis());
		user.setEditor(user);
		user.setLastEditedTimestamp(System.currentTimeMillis());
		users.put(username, user);

		String groupName = username + "_group";
		Group group = root.getGroupRoot().create(groupName, user);
		group.addUser(user);
		group.setCreator(user);
		group.setCreationTimestamp(System.currentTimeMillis());
		group.setEditor(user);
		group.setLastEditedTimestamp(System.currentTimeMillis());
		groups.put(groupName, group);

		String roleName = username + "_role";
		Role role = root.getRoleRoot().create(roleName, user);
		group.addRole(role);
		System.err.println("Created role: " + role.getElement().getId());
		role.grantPermissions(role, READ_PERM);
		roles.put(roleName, role);

		UserInfo userInfo = new UserInfo(user, group, role, password);
		return userInfo;

	}

	private void addUserGroupRoleProject() {
		// User, Groups, Roles
		userInfo = createUserInfo("joe1", "Joe", "Doe");
		UserRoot userRoot = getMeshRoot().getUserRoot();
		GroupRoot groupRoot = getMeshRoot().getGroupRoot();
		RoleRoot roleRoot = getMeshRoot().getRoleRoot();

		project = root.getProjectRoot().create(PROJECT_NAME, userInfo.getUser());
		project.addLanguage(getEnglish());
		project.addLanguage(getGerman());

		// Guest Group / Role

		Group guestGroup = root.getGroupRoot().create("guests", userInfo.getUser());
		groups.put("guests", guestGroup);

		Role guestRole = root.getRoleRoot().create("guest_role", userInfo.getUser());
		guestGroup.addRole(guestRole);
		roles.put(guestRole.getName(), guestRole);

		// Extra User
		// for (int i = 0; i < 6 * multiplicator; i++) {
		User user = userRoot.create("guest", userInfo.getUser());
		user.addGroup(guestGroup);
		// userService.setPassword(user, "guestpw" + i);
		user.setFirstname("Guest Firstname");
		user.setLastname("Guest Lastname");
		user.setEmailAddress("guest@spam.gentics.com");
		users.put(user.getUsername(), user);
		// }
		// Extra Groups
		// for (int i = 0; i < 6 * multiplicator; i++) {
		Group group = groupRoot.create("extra_group", userInfo.getUser());
		groups.put(group.getName(), group);
		// }

		// Extra Roles
		// for (int i = 0; i < 6 * multiplicator; i++) {
		Role role = roleRoot.create("extra_role", userInfo.getUser());
		roles.put(role.getName(), role);
		// }
	}

	// private void addMicoSchemas() {
	// SchemaRoot schemaRoot = root.getSchemaRoot();
	// MicroPropertyType imageGallery = schemaService.createMicroPropertyTypeSchema("gallery");
	// BasicPropertyType descriptionSchema = imageGallery.createBasicPropertyTypeSchema("description", PropertyType.STRING);
	// imageGallery.addProperty(descriptionSchema);
	//
	// BasicPropertyType imagesSchemas = imageGallery.createListPropertyTypeSchema("images");
	// // imagesSchemas.add(PropertyType.REFERENCE);
	// imageGallery.addProperty(imagesSchemas);
	// microSchemas.put("gallery", imageGallery);
	//
	// }

	private void addTagFamilies() {
		TagFamily basicTagFamily = getProject().getTagFamilyRoot().create("basic", userInfo.getUser());
		basicTagFamily.setDescription("Description for basic tag family");
		tagFamilies.put("basic", basicTagFamily);

		TagFamily colorTagFamily = getProject().getTagFamilyRoot().create("colors", userInfo.getUser());
		colorTagFamily.setDescription("Description for color tag family");
		tagFamilies.put("colors", colorTagFamily);

	}

	private void addSchemaContainers() throws MeshSchemaException {
		addBootstrapSchemas();
		addBlogPostSchema();
	}

	private void addBootstrapSchemas() {

		// folder
		SchemaContainer folderSchemaContainer = rootService.schemaContainerRoot().findByName("folder");
		project.getSchemaContainerRoot().addSchemaContainer(folderSchemaContainer);
		schemaContainers.put("folder", folderSchemaContainer);

		// content
		SchemaContainer contentSchemaContainer = rootService.schemaContainerRoot().findByName("content");
		project.getSchemaContainerRoot().addSchemaContainer(contentSchemaContainer);
		schemaContainers.put("content", contentSchemaContainer);

		// binary-content
		SchemaContainer binaryContentSchemaContainer = rootService.schemaContainerRoot().findByName("binary-content");
		project.getSchemaContainerRoot().addSchemaContainer(binaryContentSchemaContainer);
		schemaContainers.put("binary-content", binaryContentSchemaContainer);

	}

	private void addBlogPostSchema() throws MeshSchemaException {
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

		schemaContainers.put("blogpost", blogPostSchemaContainer);
	}

	public Node addFolder(Node rootNode, String englishName, String germanName) {
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

	//	private void setCreatorEditor(GenericVertex<?> node) {
	//		node.setCreator(userInfo.getUser());
	//		node.setCreationTimestamp(System.currentTimeMillis());
	//
	//		node.setEditor(userInfo.getUser());
	//		node.setLastEditedTimestamp(System.currentTimeMillis());
	//	}

	public Tag addTag(String name) {
		return addTag(name, getTagFamily("demo"));
	}

	public Tag addTag(String name, TagFamily tagFamily) {
		if (name == null || StringUtils.isEmpty(name)) {
			throw new RuntimeException("Name for tag empty");
		}
		Tag tag = tagFamily.create(name, project, userInfo.getUser());
		tags.put(name.toLowerCase(), tag);
		return tag;
	}

	private Node addContent(Node parentNode, String name, String englishContent, String germanContent, SchemaContainer schema) {
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

	public Project getProject() {
		return project;
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

	public int getNodeCount() {
		// +1 basenode (1 project)
		return folders.size() + contents.size() + 1;
	}

	public Map<String, TagFamily> getTagFamilies() {
		return tagFamilies;
	}

}
