package com.gentics.mesh.demo;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.MicroPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.MeshUserService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.SchemaService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

@Component
public class DemoDataProvider {

	private static final Logger log = LoggerFactory.getLogger(DemoDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	private static SecureRandom random = new SecureRandom();

	@Autowired
	private FramedTransactionalGraph fg;

	@Autowired
	private MeshUserService userService;

	@Autowired
	private MeshRootService rootService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private TagService tagService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private SchemaService schemaService;

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

	private Map<String, Schema> schemas = new HashMap<>();
	private Map<String, MicroPropertyType> microSchemas = new HashMap<>();
	private Map<String, MeshNode> folders = new HashMap<>();
	private Map<String, MeshNode> contents = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, MeshUser> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	private DemoDataProvider() {
	}

	public void setup(int multiplicator) throws JsonParseException, JsonMappingException, IOException {
		bootstrapInitializer.initMandatoryData();

		schemas.clear();
		microSchemas.clear();
		contents.clear();
		folders.clear();
		tags.clear();
		users.clear();
		roles.clear();
		groups.clear();

		english = languageService.findByLanguageTag("en");
		german = languageService.findByLanguageTag("de");

		addUserGroupRoleProject(multiplicator);
		addMicoSchemas();
		addSchemas();
		addTags();
		addFolderStructure();
		addContents(multiplicator);
		updatePermissions();

		log.info("Nodes:    " + getNodeCount());
		log.info("Folders:  " + folders.size());
		log.info("Contents: " + contents.size());
		log.info("Tags:     " + tags.size());
		log.info("Users:    " + users.size());
		log.info("Groups:   " + groups.size());
		log.info("Roles:    " + roles.size());
		fg.commit();
	}

	private void addContents(int multiplicator) {

		Schema contentSchema = schemas.get("content");

		for (int i = 0; i < 12 * multiplicator; i++) {
			addContent(folders.get("2014"), "News_2014_" + i, "News " + i + "!", "Neuigkeiten " + i + "!", contentSchema);
		}

		addContent(folders.get("news"), "News Overview", "News Overview", "News Übersicht", contentSchema);

		addContent(folders.get("deals"), "Super Special Deal 2015", "Buy two get nine!", "Kauf zwei und nimm neun mit!", contentSchema);
		for (int i = 0; i < 12 * multiplicator; i++) {
			addContent(folders.get("deals"), "Special Deal June 2015 - " + i, "Buy two get three! " + i, "Kauf zwei und nimm drei mit!" + i,
					contentSchema);
		}

		addContent(folders.get("2015"), "Special News_2014", "News!", "Neuigkeiten!", contentSchema);
		for (int i = 0; i < 12 * multiplicator; i++) {
			addContent(folders.get("2015"), "News_2015_" + i, "News" + i + "!", "Neuigkeiten " + i + "!", contentSchema);
		}

		MeshNode porsche911 = addContent(
				folders.get("products"),
				"Porsche 911",
				"997 is the internal designation for the Porsche 911 model manufactured and sold by German manufacturer Porsche between 2004 (as Model Year 2005) and 2012.",
				"Porsche 997 ist die interne Modellbezeichnung von Porsche für das von 2004 bis Ende 2012 produzierte 911-Modell.", contentSchema);
		porsche911.addTag(tags.get("vehicle"));
		porsche911.addTag(tags.get("car"));

		MeshNode nissanGTR = addContent(
				folders.get("products"),
				"Nissan GT-R",
				"The Nissan GT-R is a 2-door 2+2 sports coupé produced by Nissan and first released in Japan in 2007",
				"Der Nissan GT-R ist ein seit Dezember 2007 produziertes Sportcoupé des japanischen Automobilherstellers Nissan und der Nachfolger des Nissan Skyline GT-R R34.",
				contentSchema);
		nissanGTR.addTag(tags.get("vehicle"));
		nissanGTR.addTag(tags.get("car"));
		nissanGTR.addTag(tags.get("green"));

		MeshNode bmwM3 = addContent(
				folders.get("products"),
				"BMW M3",
				"The BMW M3 (first launched in 1986) is a high-performance version of the BMW 3-Series, developed by BMW's in-house motorsport division, BMW M.",
				"Der BMW M3 ist ein Sportmodell der 3er-Reihe von BMW, das seit Anfang 1986 hergestellt wird. Dabei handelt es sich um ein Fahrzeug, welches von der BMW-Tochterfirma BMW M GmbH entwickelt und anfangs (E30 und E36) auch produziert wurde.",
				contentSchema);
		bmwM3.addTag(tags.get("vehicle"));
		bmwM3.addTag(tags.get("car"));
		bmwM3.addTag(tags.get("blue"));

		MeshNode concorde = addContent(
				folders.get("products"),
				"Concorde",
				"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
				"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.",
				contentSchema);
		concorde.addTag(tags.get("plane"));
		concorde.addTag(tags.get("twinjet"));
		concorde.addTag(tags.get("red"));

		MeshNode boeing737 = addContent(
				folders.get("products"),
				"Boeing 737",
				"The Boeing 737 is a short- to medium-range twinjet narrow-body airliner. Originally developed as a shorter, lower-cost twin-engined airliner derived from Boeing's 707 and 727, the 737 has developed into a family of nine passenger models with a capacity of 85 to 215 passengers.",
				"Die Boeing 737 des US-amerikanischen Flugzeugherstellers Boeing ist die weltweit meistgebaute Familie strahlgetriebener Verkehrsflugzeuge.",
				contentSchema);
		boeing737.addTag(tags.get("plane"));
		boeing737.addTag(tags.get("twinjet"));

		MeshNode a300 = addContent(
				folders.get("products"),
				"Airbus A300",
				"The Airbus A300 is a short- to medium-range wide-body twin-engine jet airliner that was developed and manufactured by Airbus. Released in 1972 as the world's first twin-engined widebody, it was the first product of Airbus Industrie, a consortium of European aerospace manufacturers, now a subsidiary of Airbus Group.",
				"Der Airbus A300 ist das erste zweistrahlige Großraumflugzeug der Welt, produziert vom europäischen Flugzeughersteller Airbus.",
				contentSchema);
		a300.addTag(tags.get("plane"));
		a300.addTag(tags.get("twinjet"));
		a300.addTag(tags.get("red"));

		MeshNode wrangler = addContent(
				folders.get("products"),
				"Jeep Wrangler",
				"The Jeep Wrangler is a compact and mid-size (Wrangler Unlimited models) four-wheel drive off-road and sport utility vehicle (SUV), manufactured by American automaker Chrysler, under its Jeep marque – and currently in its third generation.",
				"Der Jeep Wrangler ist ein Geländewagen des US-amerikanischen Herstellers Jeep innerhalb des Chrysler-Konzerns.", contentSchema);
		wrangler.addTag(tags.get("vehicle"));
		wrangler.addTag(tags.get("jeep"));

		MeshNode volvo = addContent(folders.get("products"), "Volvo B10M",
				"The Volvo B10M was a mid-engined bus and coach chassis manufactured by Volvo between 1978 and 2003.", null, contentSchema);
		volvo.addTag(tags.get("vehicle"));
		volvo.addTag(tags.get("bus"));

		MeshNode hondact90 = addContent(folders.get("products"), "Honda CT90",
				"The Honda CT90 was a small step-through motorcycle manufactured by Honda from 1966 to 1979.", null, contentSchema);
		hondact90.addTag(tags.get("vehicle"));
		hondact90.addTag(tags.get("motorcycle"));

		MeshNode hondaNR = addContent(
				folders.get("products"),
				"Honda NR",
				"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
				"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.",
				contentSchema);
		hondaNR.addTag(tags.get("vehicle"));
		hondaNR.addTag(tags.get("motorcycle"));
		hondaNR.addTag(tags.get("green"));

	}

	private void addFolderStructure() {

		MeshNode rootNode = nodeService.create();
		//		rootNode = nodeService.save(rootNode);
		rootNode.setCreator(userInfo.getUser());
		rootNode.addProject(project);
		project.setRootNode(rootNode);

		MeshNode news = addFolder(rootNode, "News", "Neuigkeiten");
		MeshNode news2015 = addFolder(news, "2015", null);
		news2015.addTag(tags.get("car"));
		news2015.addTag(tags.get("bike"));
		news2015.addTag(tags.get("plane"));
		news2015.addTag(tags.get("jeep"));

		MeshNode news2014 = addFolder(news, "2014", null);
		addFolder(news2014, "March", null);

		addFolder(rootNode, "Products", "Produkte");
		addFolder(rootNode, "Deals", "Angebote");

	}

	private void addTags() {

		Schema colorSchema = schemas.get("color");
		Schema categoriesSchema = schemas.get("category");

		// Tags for categories
		addTag("Vehicle", "Fahrzeug", categoriesSchema);
		addTag("Car", "Auto", categoriesSchema);
		addTag("Jeep", null, categoriesSchema);
		addTag("Bike", "Fahrrad", categoriesSchema);
		addTag("Motorcycle", "Motorrad", categoriesSchema);
		addTag("Bus", "Bus", categoriesSchema);
		addTag("Plane", "Flugzeug", categoriesSchema);
		addTag("JetFigther", "Düsenjäger", categoriesSchema);
		addTag("Twinjet", "Zweistrahliges Flugzeug", categoriesSchema);

		// Tags for colors
		addTag("red", null, colorSchema);
		addTag("blue", null, colorSchema);
		addTag("green", null, colorSchema);

	}

	public UserInfo createUserInfo(String username, String firstname, String lastname) {

		String password = "test123";
		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";

		MeshUser user = userService.create(username);
		user.setUuid("UUIDOFUSER1");
		user.setPassword(password);
		log.info("Creating user with username: " + username + " and password: " + password);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmailAddress(email);
		users.put(username, user);

		String roleName = username + "_role";
		Role role = roleService.create(roleName);
		role.addPermissions(role, READ_PERM);
		roles.put(roleName, role);

		String groupName = username + "_group";
		Group group = groupService.create(groupName);
		group.addUser(user);
		group.addRole(role);
		groups.put(groupName, group);

		UserInfo userInfo = new UserInfo(user, group, role, password);
		return userInfo;

	}

	private void addUserGroupRoleProject(int multiplicator) {
		// User, Groups, Roles
		userInfo = createUserInfo("joe1", "Joe", "Doe");

		project = projectService.create(PROJECT_NAME);
		project.setCreator(userInfo.getUser());

		root = rootService.findRoot();

		// Guest Group / Role
		Role guestRole = roleService.create("guest_role");
		roles.put(guestRole.getName(), guestRole);

		Group guests = groupService.create("guests");
		guests.addRole(guestRole);
		groups.put("guests", guests);

		// Extra User
		for (int i = 0; i < 12 * multiplicator; i++) {
			MeshUser user = userService.create("guest_" + i);
			// userService.setPassword(user, "guestpw" + i);
			user.setFirstname("Guest Firstname");
			user.setLastname("Guest Lastname");
			user.setEmailAddress("guest_" + i + "@spam.gentics.com");
			guests.addUser(user);
			users.put(user.getUsername(), user);
		}
		// Extra Groups
		for (int i = 0; i < 12 * multiplicator; i++) {
			Group group = groupService.create("extra_group_" + i);
			groups.put(group.getName(), group);
		}

		// Extra Roles
		for (int i = 0; i < 12 * multiplicator; i++) {
			Role role = roleService.create("extra_role_" + i);
			roles.put(role.getName(), role);
		}
	}

	private void addMicoSchemas() {
		MicroPropertyType imageGallery = schemaService.createMicroPropertyTypeSchema("gallery");
		BasicPropertyType descriptionSchema = schemaService.createBasicPropertyTypeSchema("description", PropertyType.STRING);
		imageGallery.addProperty(descriptionSchema);

		BasicPropertyType imagesSchemas = schemaService.createListPropertyTypeSchema("images");
		//		imagesSchemas.add(PropertyType.REFERENCE);
		imageGallery.addProperty(imagesSchemas);
		microSchemas.put("gallery", imageGallery);

	}

	private void addSchemas() {
		addBootstrapSchemas();
		addBlogPostSchema();
		addColorsSchema();
		addCategorySchema();
	}

	private void addBootstrapSchemas() {
		// tag
		Schema tagSchema = schemaService.findByName("tag");
		tagSchema.addProject(project);
		schemas.put("tag", tagSchema);

		// folder
		Schema folderSchema = schemaService.findByName("folder");
		folderSchema.addProject(project);
		schemas.put("folder", folderSchema);

		// content
		Schema contentSchema = schemaService.findByName("content");
		contentSchema.addProject(project);
		schemas.put("content", contentSchema);

		// binary-content
		Schema binaryContentSchema = schemaService.findByName("binary-content");
		binaryContentSchema.addProject(project);
		schemas.put("binary-content", binaryContentSchema);

	}

	private void addColorsSchema() {

		Schema colorSchema = schemaService.create("colors");
		colorSchema.setDescription("Colors");
		colorSchema.setDescription("Colors");
		BasicPropertyType nameProp = schemaService.createBasicPropertyTypeSchema(Schema.NAME_KEYWORD, PropertyType.I18N_STRING);
		nameProp.setDisplayName("Name");
		nameProp.setDescription("The name of the category.");
		colorSchema.addPropertyTypeSchema(nameProp);
		schemas.put("color", colorSchema);
	}

	private void addBlogPostSchema() {
		Schema blogPostSchema = schemaService.create("blogpost");
		BasicPropertyType content = schemaService.createBasicPropertyTypeSchema("content", PropertyType.LIST);
		blogPostSchema.addPropertyTypeSchema(content);
		schemas.put("blogpost", blogPostSchema);

	}

	private void addCategorySchema() {
		Schema categoriesSchema = schemaService.create(TAG_CATEGORIES_SCHEMA_NAME);
		categoriesSchema.addProject(project);
		categoriesSchema.setDisplayName("Category");
		categoriesSchema.setDescription("Custom schema for tag categories");
		categoriesSchema.setCreator(userInfo.getUser());
		BasicPropertyType nameProp = schemaService.createBasicPropertyTypeSchema(Schema.NAME_KEYWORD, PropertyType.I18N_STRING);
		nameProp.setDisplayName("Name");
		nameProp.setDescription("The name of the category.");
		categoriesSchema.addPropertyTypeSchema(nameProp);

		BasicPropertyType displayNameProp = schemaService.createBasicPropertyTypeSchema(Schema.DISPLAY_NAME_KEYWORD, PropertyType.I18N_STRING);
		displayNameProp.setDisplayName("Display Name");
		displayNameProp.setDescription("The display name property of the category.");
		categoriesSchema.addPropertyTypeSchema(displayNameProp);

		BasicPropertyType contentProp = schemaService.createBasicPropertyTypeSchema(Schema.CONTENT_KEYWORD, PropertyType.I18N_STRING);
		contentProp.setDisplayName("Content");
		contentProp.setDescription("The main content html of the category.");
		categoriesSchema.addPropertyTypeSchema(contentProp);
		schemas.put("category", categoriesSchema);

	}

	private void updatePermissions() {
		// // Add Permissions
		// // Add admin permissions to all nodes
		// int i = 0;
		// for (GenericNode currentNode : genericNodeService.findAll()) {
		// currentNode = genericNodeService.reload(currentNode);
		// log.info("Adding BasicPermission to node {" + currentNode.getId() + "}");
		// if (adminRole.getId() == currentNode.getId()) {
		// log.info("Skipping role");
		// continue;
		// }
		// roleService.addPermission(adminRole, currentNode, CREATE, READ, UPDATE, DELETE);
		// adminRole = roleService.save(adminRole);
		// log.info("Added permissions to {" + i + "} objects.");
		// i++;
		// }

		// TODO determine why this is not working when using sdn
		// Add Permissions
		//		Node roleNode = neo4jTemplate.getPersistentState(userInfo.getRole());
		Role role = userInfo.getRole();

		for (Vertex vertex : fg.getVertices()) {
			WrappedVertex wrappedVertex = (WrappedVertex) vertex;

			//TODO typecheck? and verify how orient will behave
			if (role.getVertex().getId() == vertex.getId()) {
				log.info("Skipping own role");
				continue;
			}

			MeshVertex meshVertex = fg.frameElement(wrappedVertex.getBaseElement(), MeshVertex.class);
			role.addPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM);

			//			GraphPermission perm = role.addPermissions();
			//			perm.setProperty("permissions-read", true);
			//			perm.setProperty("permissions-delete", true);
			//			perm.setProperty("permissions-create", true);
			//			perm.setProperty("permissions-update", true);
			// GenericNode sdnNode = neo4jTemplate.projectTo(node, GenericNode.class);
			// roleService.addPermission(adminRole, sdnNode, CREATE, READ, UPDATE, DELETE);
			// genericNodeService.save(node);

		}
		log.info("Added BasicPermissions to nodes");

	}

	public MeshNode addFolder(MeshNode rootNode, String englishName, String germanName) {
		MeshNode folderNode = nodeService.create();
		folderNode.setParentNode(rootNode);
		folderNode.addProject(project);

		if (germanName != null) {
			folderNode.setDisplayName(german, germanName);
			folderNode.setName(german, germanName);
		}
		if (englishName != null) {
			folderNode.setDisplayName(english, englishName);
			folderNode.setName(english, englishName);
		}
		folderNode.setCreator(userInfo.getUser());
		folderNode.setSchema(schemas.get("folder"));
		if (englishName == null || StringUtils.isEmpty(englishName)) {
			throw new RuntimeException("Key for folder empty");
		}
		if (folders.containsKey(englishName.toLowerCase())) {
			throw new RuntimeException("Collision of folders detected for key " + englishName.toLowerCase());
		}

		folders.put(englishName.toLowerCase(), folderNode);
		return folderNode;
	}

	public Tag addTag(String englishName, String germanName) {
		return addTag(englishName, germanName, schemas.get("tag"));
	}

	public Tag addTag(String englishName, String germanName, Schema schema) {
		Tag tag = tagService.create();
		if (englishName != null) {
			tag.setDisplayName(english, englishName);
		}
		if (germanName != null) {
			tag.setDisplayName(german, germanName);
		}
		tag.addProject(project);
		tag.setSchema(schema);
		tag.setCreator(userInfo.getUser());
		if (englishName == null || StringUtils.isEmpty(englishName)) {
			throw new RuntimeException("Key for tag empty");
		}
		tags.put(englishName.toLowerCase(), tag);
		return tag;
	}

	private MeshNode addContent(MeshNode parentNode, String name, String englishContent, String germanContent, Schema schema) {
		MeshNode node = nodeService.create();
		node.setDisplayName(english, name + " english");
		node.setName(english, name + ".en.html");
		node.setContent(english, englishContent);

		if (germanContent != null) {
			node.setDisplayName(german, name + " german");
			node.setName(german, name + ".de.html");
			node.setContent(german, germanContent);
		}
		// TODO maybe set project should be done inside the save?
		node.addProject(project);
		node.setCreator(userInfo.getUser());
		node.setSchema(schema);
		//		node.setOrder(42);
		node.setParentNode(parentNode);
		// Add the content to the given tag
		//		parentTag.addContent(content);
		//		parentTag = tagService.save(parentTag);

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

		String name = folders.get("news").getName(language);
		String name2 = folders.get("2015").getName(language);
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

	public MeshNode getFolder(String name) {
		return folders.get(name);
	}

	public MeshNode getContent(String name) {
		return contents.get(name);
	}

	public Tag getTag(String name) {
		return tags.get(name);
	}

	public Schema getSchema(String name) {
		return schemas.get(name);
	}

	public Map<String, Tag> getTags() {
		return tags;
	}

	public Map<String, MeshNode> getContents() {
		return contents;
	}

	public Map<String, MeshNode> getFolders() {
		return folders;
	}

	public Map<String, MeshUser> getUsers() {
		return users;
	}

	public Map<String, Group> getGroups() {
		return groups;
	}

	public Map<String, Role> getRoles() {
		return roles;
	}

	public Map<String, Schema> getSchemas() {
		return schemas;
	}

	public MeshRoot getMeshRoot() {
		return root;

	}

	public int getNodeCount() {
		return folders.size() + contents.size();
	}
}
