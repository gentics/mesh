package com.gentics.mesh.test;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestSize.EMPTY;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.MeshJsonException;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TestDataProvider {

	private static final Logger log = LoggerFactory.getLogger(TestDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String INITIAL_BRANCH_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	private static TestDataProvider instance;

	public static TestDataProvider getInstance() {
		return instance;
	}

	private Database db;

	private BootstrapInitializer boot;

	// References to dummy data

	private String english = "en";

	private String german = "de";

	private Project project;
	private String projectUuid;
	private String branchUuid;

	private UserInfo userInfo;

	private MeshRoot root;

	private TestSize size;

	private Map<String, SchemaContainer> schemaContainers = new HashMap<>();
	private Map<String, MicroschemaContainer> microschemaContainers = new HashMap<>();
	private Map<String, TagFamily> tagFamilies = new HashMap<>();
	private long contentCount = 0;
	private Map<String, Node> folders = new HashMap<>();
	private Map<String, Node> contents = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, User> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	private String contentUuid;

	public TestDataProvider(TestSize size, BootstrapInitializer boot, Database database) {
		this.size = size;
		this.boot = boot;
		this.db = database;
		instance = this;
	}

	public void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		long start = System.currentTimeMillis();
		if (getSize() == EMPTY) {
			return;
		}

		try (Tx tx = db.tx()) {
			boot.initMandatoryData();
			boot.initOptionalData(true);
			tx.getGraph().commit();
			schemaContainers.clear();
			microschemaContainers.clear();
			tagFamilies.clear();
			contents.clear();
			folders.clear();
			tags.clear();
			users.clear();
			roles.clear();
			groups.clear();

			root = boot.meshRoot();

			addBootstrappedData();
			addSchemaContainers();
			addUserGroupRoleProject();
			if (getSize() == FULL) {
				addMicroschemaContainers();
				addTagFamilies();
				addTags();
			}
			addFolderStructure();
			if (getSize() == FULL) {
				addContents();
			}
			tx.getGraph().commit();

			long startPerm = System.currentTimeMillis();
			addPermissions(tagFamilies.values());
			addPermissions(roles.values());
			addPermissions(groups.values());
			addPermissions(users.values());
			addPermissions(folders.values());
			addPermissions(contents.values());
			addPermissions(tags.values());
			addPermissions(schemaContainers.values());
			addPermissions(microschemaContainers.values());
			addPermissions(project);
			addPermissions(project.getBaseNode());
			addPermissions(project.getMicroschemaContainerRoot());
			addPermissions(project.getSchemaContainerRoot());
			addPermissions(project.getBranchRoot());
			addPermissions(project.getInitialBranch());
			addPermissions(project.getTagFamilyRoot());
			addPermissions(boot.projectRoot());
			addPermissions(boot.userRoot());
			addPermissions(boot.groupRoot());
			addPermissions(boot.roleRoot());
			addPermissions(boot.microschemaContainerRoot());
			addPermissions(boot.schemaContainerRoot());
			log.debug("Added BasicPermissions to nodes took {" + (System.currentTimeMillis() - startPerm) + "} ms.");
			tx.getGraph().commit();
		}

		long duration = System.currentTimeMillis() - start;
		log.debug("Setup took: {" + duration + "}");
	}

	private void addPermissions(MeshVertex vertex) {
		addPermissions(Arrays.asList(vertex));
	}

	public TestSize getSize() {
		return size;
	}

	private void addPermissions(Collection<? extends MeshVertex> elements) {
		Role role = userInfo.getRole();
		for (MeshVertex meshVertex : elements) {
			if (log.isTraceEnabled()) {
				log.trace("Granting CRUD permissions on {" + meshVertex.getElement().getId() + "} with role {" + role.getElement().getId() + "}");
			}
			role.grantPermissions(meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM, READ_PUBLISHED_PERM, PUBLISH_PERM);
		}
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
		addContent(folders.get("march"), "New_in_March_2014", "This is new in march 2014.", "Das ist neu im März 2014", contentSchema);

		Node content = addContent(folders.get("news"), "News Overview", "News Overview", "News Übersicht", contentSchema);
		contentUuid = content.getUuid();

		addContent(folders.get("deals"), "Super Special Deal 2015", "Buy two get nine!", "Kauf zwei und nimm neun mit!", contentSchema);
		addContent(folders.get("deals"), "Special Deal June 2015", "Buy two get three!", "Kauf zwei und nimm drei mit!", contentSchema);

		addContent(folders.get("2015"), "Special News_2014", "News!", "Neuigkeiten!", contentSchema);
		addContent(folders.get("2015"), "News_2015", "News!", "Neuigkeiten!", contentSchema);

		Node concorde = addContent(folders.get("products"), "Concorde",
			"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
			"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.",
			contentSchema);
		concorde.addTag(tags.get("plane"), project.getLatestBranch());
		concorde.addTag(tags.get("twinjet"), project.getLatestBranch());
		concorde.addTag(tags.get("red"), project.getLatestBranch());

		Node hondaNR = addContent(folders.get("products"), "Honda NR",
			"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
			"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.",
			contentSchema);
		hondaNR.addTag(tags.get("vehicle"), project.getLatestBranch());
		hondaNR.addTag(tags.get("motorcycle"), project.getLatestBranch());
		hondaNR.addTag(tags.get("green"), project.getLatestBranch());

	}

	private void addFolderStructure() {

		Node baseNode = project.getBaseNode();
		// rootNode.addProject(project);

		Node news = addFolder(baseNode, "News", "Neuigkeiten");
		Node news2015 = addFolder(news, "2015", null);
		if (getSize() == FULL) {
			news2015.addTag(tags.get("car"), project.getLatestBranch());
			news2015.addTag(tags.get("bike"), project.getLatestBranch());
			news2015.addTag(tags.get("plane"), project.getLatestBranch());
			news2015.addTag(tags.get("jeep"), project.getLatestBranch());

			Node news2014 = addFolder(news, "2014", null);
			addFolder(news2014, "March", "März");

			addFolder(baseNode, "Products", "Produkte");
			addFolder(baseNode, "Deals", "Angebote");
		}

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
		String hashedPassword = "$2a$10$n/UeWGbY9c1FHFyCqlVsY.XvNYmZ7Jjgww99SF94q/B5nomYuquom";

		log.debug("Creating user with username: " + username + " and password: " + password);

		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";
		User user = root.getUserRoot().create(username, null);
		// Precomputed hash since hashing takes some time and we want to keep out tests fast
		user.setPasswordHash(hashedPassword);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmailAddress(email);

		user.setCreator(user);
		user.setCreationTimestamp();
		user.setEditor(user);
		user.setLastEditedTimestamp();
		users.put(username, user);

		String groupName = username + "_group";
		Group group = root.getGroupRoot().create(groupName, user);
		group.addUser(user);
		group.setCreator(user);
		group.setCreationTimestamp();
		group.setEditor(user);
		group.setLastEditedTimestamp();
		groups.put(groupName, group);

		String roleName = username + "_role";
		Role role = root.getRoleRoot().create(roleName, user);
		group.addRole(role);
		role.grantPermissions(role, READ_PERM);
		roles.put(roleName, role);

		return new UserInfo(user, group, role, password);
	}

	private void addUserGroupRoleProject() {
		// User, Groups, Roles
		userInfo = createUserInfo("joe1", "Joe", "Doe");
		UserRoot userRoot = getMeshRoot().getUserRoot();
		GroupRoot groupRoot = getMeshRoot().getGroupRoot();
		RoleRoot roleRoot = getMeshRoot().getRoleRoot();

		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		project = root.getProjectRoot().create(PROJECT_NAME, null, null, null, userInfo.getUser(), getSchemaContainer("folder").getLatestVersion(), batch);
		User jobUser = userInfo.getUser();
		project.getSchemaContainerRoot().addSchemaContainer(jobUser, getSchemaContainer("folder"), batch);
		project.getSchemaContainerRoot().addSchemaContainer(jobUser, getSchemaContainer("content"), batch);
		project.getSchemaContainerRoot().addSchemaContainer(jobUser, getSchemaContainer("binary_content"), batch);
		projectUuid = project.getUuid();
		branchUuid = project.getInitialBranch().getUuid();

		if (getSize() == FULL) {
			// Guest Group / Role
			Group guestGroup = root.getGroupRoot().create("guests", userInfo.getUser());
			groups.put("guests", guestGroup);

			Role guestRole = root.getRoleRoot().create("guest_role", userInfo.getUser());
			guestGroup.addRole(guestRole);
			roles.put(guestRole.getName(), guestRole);

			// Extra User
			User user = userRoot.create("guest", userInfo.getUser());
			user.addGroup(guestGroup);
			user.setFirstname("Guest Firstname");
			user.setLastname("Guest Lastname");
			user.setEmailAddress("guest@spam.gentics.com");
			users.put(user.getUsername(), user);

			Group group = groupRoot.create("extra_group", userInfo.getUser());
			groups.put(group.getName(), group);

			Role role = roleRoot.create("extra_role", userInfo.getUser());
			roles.put(role.getName(), role);
		}
		// Publish the project basenode
		project.getBaseNode().publish(getEnglish(), getProject().getLatestBranch(), getUserInfo().getUser());
		contentCount++;

	}

	public void addTagFamilies() {
		TagFamily basicTagFamily = getProject().getTagFamilyRoot().create("basic", userInfo.getUser());
		basicTagFamily.setDescription("Description for basic tag family");
		tagFamilies.put("basic", basicTagFamily);

		TagFamily colorTagFamily = getProject().getTagFamilyRoot().create("colors", userInfo.getUser());
		colorTagFamily.setDescription("Description for color tag family");
		tagFamilies.put("colors", colorTagFamily);
	}

	private void addSchemaContainers() throws MeshSchemaException {
		addBootstrapSchemas();
	}

	private void addBootstrapSchemas() {

		// folder
		SchemaContainer folderSchemaContainer = boot.schemaContainerRoot().findByName("folder");
		schemaContainers.put("folder", folderSchemaContainer);

		// content
		SchemaContainer contentSchemaContainer = boot.schemaContainerRoot().findByName("content");
		schemaContainers.put("content", contentSchemaContainer);

		// binary_content
		SchemaContainer binaryContentSchemaContainer = boot.schemaContainerRoot().findByName("binary_content");
		schemaContainers.put("binary_content", binaryContentSchemaContainer);

	}

	/**
	 * Add microschemas
	 * 
	 * @throws MeshJsonException
	 */
	private void addMicroschemaContainers() throws MeshJsonException {
		addVCardMicroschema();
		addCaptionedImageMicroschema();
	}

	/**
	 * Add microschema "vcard" to db
	 * 
	 * @throws MeshJsonException
	 */
	private void addVCardMicroschema() throws MeshJsonException {
		MicroschemaModel vcardMicroschema = new MicroschemaModelImpl();
		vcardMicroschema.setName("vcard");
		vcardMicroschema.setDescription("Microschema for a vcard");

		// firstname field
		StringFieldSchema firstNameFieldSchema = new StringFieldSchemaImpl();
		firstNameFieldSchema.setName("firstName");
		firstNameFieldSchema.setLabel("First Name");
		firstNameFieldSchema.setRequired(true);
		vcardMicroschema.addField(firstNameFieldSchema);

		// lastname field
		StringFieldSchema lastNameFieldSchema = new StringFieldSchemaImpl();
		lastNameFieldSchema.setName("lastName");
		lastNameFieldSchema.setLabel("Last Name");
		lastNameFieldSchema.setRequired(true);
		vcardMicroschema.addField(lastNameFieldSchema);

		// address field
		StringFieldSchema addressFieldSchema = new StringFieldSchemaImpl();
		addressFieldSchema.setName("address");
		addressFieldSchema.setLabel("Address");
		vcardMicroschema.addField(addressFieldSchema);

		// postcode field
		StringFieldSchema postcodeFieldSchema = new StringFieldSchemaImpl();
		postcodeFieldSchema.setName("postcode");
		postcodeFieldSchema.setLabel("Post Code");
		vcardMicroschema.addField(postcodeFieldSchema);

		MicroschemaContainer vcardMicroschemaContainer = boot.microschemaContainerRoot().create(vcardMicroschema, userInfo.getUser(), EventQueueBatch.create());
		microschemaContainers.put(vcardMicroschemaContainer.getName(), vcardMicroschemaContainer);
		project.getMicroschemaContainerRoot().addMicroschema(user(), vcardMicroschemaContainer, EventQueueBatch.create());
	}

	/**
	 * Add microschema "captionedImage" to db
	 * 
	 * @throws MeshJsonException
	 */
	private void addCaptionedImageMicroschema() throws MeshJsonException {
		MicroschemaModel captionedImageMicroschema = new MicroschemaModelImpl();
		captionedImageMicroschema.setName("captionedImage");
		captionedImageMicroschema.setDescription("Microschema for a captioned image");

		// image field
		NodeFieldSchema imageFieldSchema = new NodeFieldSchemaImpl();
		imageFieldSchema.setName("image");
		imageFieldSchema.setLabel("Image");
		imageFieldSchema.setAllowedSchemas("image");
		captionedImageMicroschema.addField(imageFieldSchema);

		// caption field
		StringFieldSchema captionFieldSchema = new StringFieldSchemaImpl();
		captionFieldSchema.setName("caption");
		captionFieldSchema.setLabel("Caption");
		captionedImageMicroschema.addField(captionFieldSchema);

		MicroschemaContainer microschemaContainer = boot.microschemaContainerRoot().create(captionedImageMicroschema, userInfo.getUser(), EventQueueBatch.create());
		microschemaContainers.put(captionedImageMicroschema.getName(), microschemaContainer);
		project.getMicroschemaContainerRoot().addMicroschema(user(), microschemaContainer, EventQueueBatch.create());
	}

	public Node addFolder(Node rootNode, String englishName, String germanName) {
		SchemaContainerVersion schemaVersion = schemaContainers.get("folder").getLatestVersion();
		Node folderNode = rootNode.create(userInfo.getUser(), schemaVersion, project);
		Branch branch = project.getLatestBranch();
		if (germanName != null) {
			NodeGraphFieldContainer germanContainer = folderNode.createGraphFieldContainer(german, branch, userInfo.getUser());
			// germanContainer.createString("displayName").setString(germanName);
			germanContainer.createString("teaser").setString(germanName);
			germanContainer.createString("slug").setString(germanName);
			germanContainer.updateDisplayFieldValue();
			contentCount++;
			folderNode.publish(getGerman(), branch, getUserInfo().getUser());
		}
		if (englishName != null) {
			NodeGraphFieldContainer englishContainer = folderNode.createGraphFieldContainer(english, branch, userInfo.getUser());
			// englishContainer.createString("displayName").setString(englishName);
			englishContainer.createString("name").setString(englishName);
			englishContainer.createString("slug").setString(englishName);
			englishContainer.updateDisplayFieldValue();
			contentCount++;
			folderNode.publish(getEnglish(), branch, getUserInfo().getUser());
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
		Node node = parentNode.create(userInfo.getUser(), schemaContainers.get("content").getLatestVersion(), project);
		Branch branch = project.getLatestBranch();
		if (englishContent != null) {
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, branch, userInfo.getUser());
			englishContainer.createString("teaser").setString(name + "_english_name");
			englishContainer.createString("title").setString(name + " english title");
			englishContainer.createString("displayName").setString(name + " english displayName");
			englishContainer.createString("slug").setString(name + ".en.html");
			englishContainer.createHTML("content").setHtml(englishContent);
			englishContainer.updateDisplayFieldValue();
			contentCount++;
			node.publish(getEnglish(), branch, getUserInfo().getUser());
		}

		if (germanContent != null) {
			NodeGraphFieldContainer germanContainer = node.createGraphFieldContainer(german, branch, userInfo.getUser());
			germanContainer.createString("teaser").setString(name + " german");
			germanContainer.createString("title").setString(name + " german title");
			germanContainer.createString("displayName").setString(name + " german");
			germanContainer.createString("slug").setString(name + ".de.html");
			germanContainer.createHTML("content").setHtml(germanContent);
			germanContainer.updateDisplayFieldValue();
			contentCount++;
			node.publish(getGerman(), branch, getUserInfo().getUser());
		}

		if (contents.containsKey(name.toLowerCase())) {
			throw new RuntimeException("Collision of contents detected for key " + name.toLowerCase());
		}

		contents.put(name.toLowerCase(), node);
		return node;
	}

	/**
	 * Returns the path to the tag for the given language.
	 */
	public String getPathForNews2015Tag(String languageTag) {

		String name = folders.get("news").getLatestDraftFieldContainer(languageTag).getString("name").getString();
		String name2 = folders.get("2015").getLatestDraftFieldContainer(languageTag).getString("name").getString();
		return name + "/" + name2;
	}

	public String getEnglish() {
		return english;
	}

	public String getGerman() {
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

	public Map<String, MicroschemaContainer> getMicroschemaContainers() {
		return microschemaContainers;
	}

	public MeshRoot getMeshRoot() {
		return root;
	}

	public int getNodeCount() {
		// folders, contents + basenode
		return folders.size() + contents.size() + 1;
	}

	public Map<String, TagFamily> getTagFamilies() {
		return tagFamilies;
	}

	public Role role() {
		return getUserInfo().getRole();
	}

	public User user() {
		return getUserInfo().getUser();
	}

	public Group group() {
		return getUserInfo().getGroup();
	}

	public Role getAnonymousRole() {
		return roles.get("anonymous");
	}

	public String projectUuid() {
		return projectUuid;
	}

	public String getContentUuid() {
		return contentUuid;
	}

	public String branchUuid() {
		return branchUuid;
	}

	public long getContentCount() {
		return contentCount;
	}

}
