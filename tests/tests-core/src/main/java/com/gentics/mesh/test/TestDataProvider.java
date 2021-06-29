package com.gentics.mesh.test;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestSize.EMPTY;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.MeshJsonException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TestDataProvider {

	private static final Logger log = LoggerFactory.getLogger(TestDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String INITIAL_BRANCH_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	public static final String CONTENT_UUID = "43ee8f9ff71e4016ae8f9ff71e10161c";
	public static final String NEWS_UUID = "4b1346a2163a4ff89346a2163a9ff883";

	private static TestDataProvider instance;

	public static TestDataProvider getInstance() {
		return instance;
	}

	private Database db;

	private BootstrapInitializer boot;

	// References to dummy data

	private String english = "en";

	private String german = "de";

	private HibProject project;
	private String projectUuid;
	private String branchUuid;

	private UserInfo userInfo;

	private MeshRoot root;

	private TestSize size;

	private Map<String, HibSchema> schemaContainers = new HashMap<>();
	private Map<String, HibMicroschema> microschemaContainers = new HashMap<>();
	private Map<String, HibTagFamily> tagFamilies = new HashMap<>();
	private long contentCount = 0;
	private Map<String, HibNode> folders = new HashMap<>();
	private Map<String, HibNode> contents = new HashMap<>();
	private Map<String, HibTag> tags = new HashMap<>();
	private Map<String, HibUser> users = new HashMap<>();
	private Map<String, HibRole> roles = new HashMap<>();
	private Map<String, HibGroup> groups = new HashMap<>();

	private String contentUuid;

	private Provider<EventQueueBatch> queueProvider;

	public TestDataProvider(TestSize size, BootstrapInitializer boot, Database database,
		Provider<EventQueueBatch> queueProvider) {
		this.size = size;
		this.boot = boot;
		this.db = database;
		this.queueProvider = queueProvider;
		instance = this;
	}

	public void setup(MeshOptions meshOptions, boolean setAdminPassword)
		throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		long start = System.currentTimeMillis();
		if (getSize() == EMPTY) {
			return;
		}

		db.tx(tx -> {
			boot.globalCacheClear();
			if (meshOptions.getInitialAdminPassword() != null
				&& !meshOptions.getInitialAdminPassword().startsWith("debug")) {
				// We omit creating the initial admin password since hashing the password would
				// slow down tests
				meshOptions.setInitialAdminPassword(null);
			}
			boot.initMandatoryData(meshOptions);
			if (setAdminPassword) {
				setAdminPassword();
			}
			boot.initOptionalData(true);
			schemaContainers.clear();
			microschemaContainers.clear();
			tagFamilies.clear();
			contents.clear();
			folders.clear();
			tags.clear();
			users.clear();
			roles.clear();
			groups.clear();

			if (db.requiresTypeInit()) {
				root = boot.meshRoot();
			}

			addBootstrappedData(tx);
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
			// TODO HIB probably not necessary. Remove if tests are positive
			// addPermissions(toGraph(project).getMicroschemaContainerRoot());
			// addPermissions(toGraph(project).getSchemaContainerRoot());
			addPermissions(project.getBranchPermissionRoot());
			addPermissions(project.getInitialBranch());
			addPermissions(project.getTagFamilyPermissionRoot());
			PermissionRoots permissionRoots = tx.data().permissionRoots();
			addPermissions(permissionRoots.project());
			addPermissions(permissionRoots.user());
			addPermissions(permissionRoots.group());
			addPermissions(permissionRoots.role());
			addPermissions(permissionRoots.microschema());
			addPermissions(permissionRoots.schema());
			log.debug("Added BasicPermissions to nodes took {" + (System.currentTimeMillis() - startPerm) + "} ms.");
			tx.success();
		});

		long duration = System.currentTimeMillis() - start;
		log.debug("Setup took: {" + duration + "}");
	}

	private void setAdminPassword() {
		String hash = "$2a$10$X7NA0kiqrFlyX0NUhPdW1e7jevHyoaoB4OyoxV1pdA7B3SLVSkx22";
		boot.userDao().findByUsername("admin").setPasswordHash(hash);
	}

	private void addPermissions(HibBaseElement element) {
		addPermissions(Arrays.asList(element));
	}

	public TestSize getSize() {
		return size;
	}

	private void addPermissions(Collection<? extends HibBaseElement> elements) {
		RoleDaoWrapper roleDao = Tx.get().roleDao();

		HibRole role = userInfo.getRole();
		for (HibBaseElement meshVertex : elements) {
			if (log.isTraceEnabled()) {
				log.trace("Granting CRUD permissions on {" + meshVertex.getId() + "} with role {" + role.getId() + "}");
			}
			roleDao.grantPermissions(role, meshVertex, READ_PERM, CREATE_PERM, DELETE_PERM, UPDATE_PERM,
				READ_PUBLISHED_PERM, PUBLISH_PERM);
		}
	}

	/**
	 * Add data to the internal maps which was created within the {@link BootstrapInitializer} (eg. admin groups, roles, users)
	 * 
	 * @param tx
	 */
	private void addBootstrappedData(Tx tx) {
		for (HibGroup group : tx.groupDao().findAll()) {
			groups.put(group.getName(), group);
		}
		for (HibUser user : tx.userDao().findAll()) {
			users.put(user.getUsername(), user);
		}
		for (HibRole role : tx.roleDao().findAll()) {
			roles.put(role.getName(), role);
		}
	}

	private void addContents() {
		TagDaoWrapper tagDao = Tx.get().tagDao();

		HibSchema contentSchema = schemaContainers.get("content");

		addContent(folders.get("2014"), "News_2014", "News!", "Neuigkeiten!");
		addContent(folders.get("march"), "New_in_March_2014", "This is new in march 2014.", "Das ist neu im März 2014");

		HibNode content = addContent(folders.get("news"), "News Overview", "News Overview", "News Übersicht",
			CONTENT_UUID);
		contentUuid = content.getUuid();

		addContent(folders.get("deals"), "Super Special Deal 2015", "Buy two get nine!",
			"Kauf zwei und nimm neun mit!");
		addContent(folders.get("deals"), "Special Deal June 2015", "Buy two get three!",
			"Kauf zwei und nimm drei mit!");

		addContent(folders.get("2015"), "Special News_2014", "News!", "Neuigkeiten!");
		addContent(folders.get("2015"), "News_2015", "News!", "Neuigkeiten!");

		HibNode concorde = addContent(folders.get("products"), "Concorde",
			"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
			"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.");
		tagDao.addTag(concorde, tags.get("plane"), project.getLatestBranch());
		tagDao.addTag(concorde, tags.get("twinjet"), project.getLatestBranch());
		tagDao.addTag(concorde, tags.get("red"), project.getLatestBranch());

		HibNode hondaNR = addContent(folders.get("products"), "Honda NR",
			"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
			"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.");
		tagDao.addTag(hondaNR, tags.get("vehicle"), project.getLatestBranch());
		tagDao.addTag(hondaNR, tags.get("motorcycle"), project.getLatestBranch());
		tagDao.addTag(hondaNR, tags.get("green"), project.getLatestBranch());

	}

	private void addFolderStructure() {
		TagDaoWrapper tagDao = Tx.get().tagDao();
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();

		HibNode baseNode = project.getBaseNode();
		// rootNode.addProject(project);

		HibNode news = addFolder(baseNode, "News", "Neuigkeiten", NEWS_UUID);
		HibNode news2015 = addFolder(news, "2015", null);
		if (getSize() == FULL) {
			tagDao.addTag(news2015, tags.get("car"), project.getLatestBranch());
			tagDao.addTag(news2015, tags.get("bike"), project.getLatestBranch());
			tagDao.addTag(news2015, tags.get("plane"), project.getLatestBranch());
			tagDao.addTag(news2015, tags.get("jeep"), project.getLatestBranch());

			HibNode news2014 = addFolder(news, "2014", null);
			addFolder(news2014, "March", "März");

			addFolder(baseNode, "Products", "Produkte");
			addFolder(baseNode, "Deals", "Angebote");
		}

	}

	private void addTags() {

		HibTagFamily colorTags = tagFamilies.get("colors");
		HibTagFamily basicTags = tagFamilies.get("basic");

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
		UserDaoWrapper userDao = Tx.get().userDao();
		GroupDaoWrapper groupDao = Tx.get().groupDao();
		RoleDaoWrapper roleDao = Tx.get().roleDao();

		String password = "test123";
		String hashedPassword = "$2a$10$n/UeWGbY9c1FHFyCqlVsY.XvNYmZ7Jjgww99SF94q/B5nomYuquom";

		log.debug("Creating user with username: " + username + " and password: " + password);

		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";
		HibUser user = userDao.create(username, null);
		// Precomputed hash since hashing takes some time and we want to keep out tests
		// fast
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
		HibGroup group = groupDao.create(groupName, user);
		groupDao.addUser(group, user);
		group.setCreator(user);
		group.setCreationTimestamp();
		group.setEditor(user);
		group.setLastEditedTimestamp();
		groups.put(groupName, group);

		String roleName = username + "_role";
		HibRole role = roleDao.create(roleName, user);
		groupDao.addRole(group, role);
		roleDao.grantPermissions(role, role, READ_PERM);
		roles.put(roleName, role);

		return new UserInfo(user, group, role, password);
	}

	private void addUserGroupRoleProject() {
		UserDaoWrapper userDao = Tx.get().userDao();
		RoleDaoWrapper roleDao = Tx.get().roleDao();
		GroupDaoWrapper groupDao = Tx.get().groupDao();
		SchemaDaoWrapper schemaDao = Tx.get().schemaDao();
		ProjectDaoWrapper projectDao = Tx.get().projectDao();

		// User, Groups, Roles
		userInfo = createUserInfo("joe1", "Joe", "Doe");
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		project = projectDao.create(PROJECT_NAME, null, null, null, userInfo.getUser(),
			getSchemaContainer("folder").getLatestVersion(), batch);
		HibUser jobUser = userInfo.getUser();
		schemaDao.addSchema(getSchemaContainer("folder"), project, jobUser, batch);
		schemaDao.addSchema(getSchemaContainer("content"), project, jobUser, batch);
		schemaDao.addSchema(getSchemaContainer("binary_content"), project, jobUser, batch);
		projectUuid = project.getUuid();
		branchUuid = project.getInitialBranch().getUuid();

		if (getSize() == FULL) {
			// Guest Group / Role
			HibGroup guestGroup = groupDao.create("guests", userInfo.getUser());
			groups.put("guests", guestGroup);

			HibRole guestRole = roleDao.create("guest_role", userInfo.getUser());
			groupDao.addRole(guestGroup, guestRole);
			roles.put(guestRole.getName(), guestRole);

			// Extra User
			HibUser user = userDao.create("guest", userInfo.getUser());
			userDao.addGroup(user, guestGroup);
			user.setFirstname("Guest Firstname");
			user.setLastname("Guest Lastname");
			user.setEmailAddress("guest@spam.gentics.com");
			users.put(user.getUsername(), user);

			HibGroup group = groupDao.create("extra_group", userInfo.getUser());
			groups.put(group.getName(), group);

			HibRole role = roleDao.create("extra_role", userInfo.getUser());
			roles.put(role.getName(), role);
		}
		// Publish the project basenode
		InternalActionContext ac = new NodeMigrationActionContextImpl();
		boot.contentDao().publish(project.getBaseNode(), ac, getEnglish(), getProject().getLatestBranch(),
			getUserInfo().getUser());
		contentCount++;

	}

	public void addTagFamilies() {
		TagFamilyDaoWrapper tagFamilyDao = Tx.get().tagFamilyDao();
		HibTagFamily basicTagFamily = tagFamilyDao.create(getProject(), "basic", userInfo.getUser());
		basicTagFamily.setDescription("Description for basic tag family");
		tagFamilies.put("basic", basicTagFamily);

		HibTagFamily colorTagFamily = tagFamilyDao.create(getProject(), "colors", userInfo.getUser());
		colorTagFamily.setDescription("Description for color tag family");
		tagFamilies.put("colors", colorTagFamily);
	}

	private void addSchemaContainers() throws MeshSchemaException {
		addBootstrapSchemas();
	}

	private void addBootstrapSchemas() {
		SchemaDaoWrapper schemaDao = Tx.get().schemaDao();

		// folder
		HibSchema folderSchemaContainer = schemaDao.findByName("folder");
		schemaContainers.put("folder", folderSchemaContainer);

		// content
		HibSchema contentSchemaContainer = schemaDao.findByName("content");
		schemaContainers.put("content", contentSchemaContainer);

		// binary_content
		HibSchema binaryContentSchemaContainer = schemaDao.findByName("binary_content");
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
		MicroschemaDaoWrapper microschemaDao = Tx.get().microschemaDao();

		MicroschemaVersionModel vcardMicroschema = new MicroschemaModelImpl();
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

		HibMicroschema vcardMicroschemaContainer = microschemaDao.create(vcardMicroschema, userInfo.getUser(),
			createBatch());
		microschemaContainers.put(vcardMicroschemaContainer.getName(), vcardMicroschemaContainer);
		microschemaDao.addMicroschema(project, user(), vcardMicroschemaContainer, createBatch());
	}

	/**
	 * Add microschema "captionedImage" to db
	 * 
	 * @throws MeshJsonException
	 */
	private void addCaptionedImageMicroschema() throws MeshJsonException {
		MicroschemaDaoWrapper microschemaDao = Tx.get().microschemaDao();

		MicroschemaVersionModel captionedImageMicroschema = new MicroschemaModelImpl();
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

		HibMicroschema microschema = microschemaDao.create(captionedImageMicroschema, userInfo.getUser(),
			createBatch());
		microschemaContainers.put(captionedImageMicroschema.getName(), microschema);
		microschemaDao.addMicroschema(project, user(), microschema, createBatch());
	}

	public HibNode addFolder(HibNode rootNode, String englishName, String germanName) {
		return addFolder(rootNode, englishName, germanName, null);
	}

	public HibNode addFolder(HibNode rootNode, String englishName, String germanName, String uuid) {
		NodeDaoWrapper nodeDao = boot.nodeDao();
		InternalActionContext ac = new NodeMigrationActionContextImpl();
		HibSchemaVersion schemaVersion = schemaContainers.get("folder").getLatestVersion();
		HibBranch branch = project.getLatestBranch();
		HibNode folderNode;
		if (uuid == null) {
			folderNode = nodeDao.create(rootNode, userInfo.getUser(), schemaVersion, project);
		} else {
			folderNode = nodeDao.create(rootNode, userInfo.getUser(), schemaVersion, project, branch, uuid);
		}
		if (germanName != null) {
			NodeGraphFieldContainer germanContainer = boot.contentDao().createGraphFieldContainer(folderNode, german,
				branch, userInfo.getUser());
			// germanContainer.createString("displayName").setString(germanName);
			germanContainer.createString("teaser").setString(germanName);
			germanContainer.createString("slug").setString(germanName);
			germanContainer.updateDisplayFieldValue();
			contentCount++;
			boot.contentDao().publish(folderNode, ac, getGerman(), branch, getUserInfo().getUser());
		}
		if (englishName != null) {
			NodeGraphFieldContainer englishContainer = boot.contentDao().createGraphFieldContainer(folderNode, english,
				branch, userInfo.getUser());
			// englishContainer.createString("displayName").setString(englishName);
			englishContainer.createString("name").setString(englishName);
			englishContainer.createString("slug").setString(englishName);
			englishContainer.updateDisplayFieldValue();
			contentCount++;
			boot.contentDao().publish(folderNode, ac, getEnglish(), branch, getUserInfo().getUser());
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

	public HibTag addTag(String name) {
		return addTag(name, getTagFamily("demo"));
	}

	public HibTag addTag(String name, HibTagFamily tagFamily) {
		TagDaoWrapper tagDao = Tx.get().tagDao();
		if (name == null || StringUtils.isEmpty(name)) {
			throw new RuntimeException("Name for tag empty");
		}
		HibTag tag = tagDao.create(tagFamily, name, project, userInfo.getUser());
		tags.put(name.toLowerCase(), tag);
		return tag;
	}

	private HibNode addContent(HibNode parentNode, String name, String englishContent, String germanContent) {
		return addContent(parentNode, name, englishContent, germanContent, null);
	}

	private HibNode addContent(HibNode parentNode, String name, String englishContent, String germanContent,
		String uuid) {
		NodeDaoWrapper nodeDao = boot.nodeDao();
		InternalActionContext ac = new NodeMigrationActionContextImpl();
		HibBranch branch = project.getLatestBranch();
		HibNode node;
		if (uuid == null) {
			node = nodeDao.create(parentNode, userInfo.getUser(), schemaContainers.get("content").getLatestVersion(),
				project);
		} else {
			node = nodeDao.create(parentNode, userInfo.getUser(), schemaContainers.get("content").getLatestVersion(),
				project, branch, uuid);
		}
		if (englishContent != null) {
			NodeGraphFieldContainer englishContainer = boot.contentDao().createGraphFieldContainer(node, english,
				branch, userInfo.getUser());
			englishContainer.createString("teaser").setString(name + "_english_name");
			englishContainer.createString("title").setString(name + " english title");
			englishContainer.createString("displayName").setString(name + " english displayName");
			englishContainer.createString("slug").setString(name + ".en.html");
			englishContainer.createHTML("content").setHtml(englishContent);
			englishContainer.updateDisplayFieldValue();
			contentCount++;
			boot.contentDao().publish(node, ac, getEnglish(), branch, getUserInfo().getUser());
		}

		if (germanContent != null) {
			NodeGraphFieldContainer germanContainer = boot.contentDao().createGraphFieldContainer(node, german, branch,
				userInfo.getUser());
			germanContainer.createString("teaser").setString(name + " german");
			germanContainer.createString("title").setString(name + " german title");
			germanContainer.createString("displayName").setString(name + " german");
			germanContainer.createString("slug").setString(name + ".de.html");
			germanContainer.createHTML("content").setHtml(germanContent);
			germanContainer.updateDisplayFieldValue();
			contentCount++;
			boot.contentDao().publish(node, ac, getGerman(), branch, getUserInfo().getUser());
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
		ContentDaoWrapper contentDao = boot.contentDao();

		String name = contentDao.getLatestDraftFieldContainer(folders.get("news"), languageTag).getString("name")
			.getString();
		String name2 = contentDao.getLatestDraftFieldContainer(folders.get("2015"), languageTag).getString("name")
			.getString();
		return name + "/" + name2;
	}

	public String getEnglish() {
		return english;
	}

	public String getGerman() {
		return german;
	}

	public HibProject getProject() {
		return project;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Getter
	public HibNode getFolder(String name) {
		return folders.get(name);
	}

	@Getter
	public HibTagFamily getTagFamily(String key) {
		return tagFamilies.get(key);
	}

	@Getter
	public HibNode getContent(String name) {
		return contents.get(name);
	}

	@Getter
	public HibTag getTag(String name) {
		return tags.get(name);
	}

	@Getter
	public HibSchema getSchemaContainer(String name) {
		return schemaContainers.get(name);
	}

	public Map<String, HibTag> getTags() {
		return tags;
	}

	public Map<String, HibNode> getContents() {
		return contents;
	}

	public Map<String, HibNode> getFolders() {
		return folders;
	}

	public Map<String, HibUser> getUsers() {
		return users;
	}

	public Map<String, HibGroup> getGroups() {
		return groups;
	}

	public Map<String, HibRole> getRoles() {
		return roles;
	}

	public Map<String, HibSchema> getSchemaContainers() {
		return schemaContainers;
	}

	public Map<String, HibMicroschema> getMicroschemaContainers() {
		return microschemaContainers;
	}

	public MeshRoot getMeshRoot() {
		return root;
	}

	public int getNodeCount() {
		// folders, contents + basenode
		return folders.size() + contents.size() + 1;
	}

	public Map<String, HibTagFamily> getTagFamilies() {
		return tagFamilies;
	}

	@Getter
	public HibRole role() {
		return getUserInfo().getRole();
	}

	@Getter
	public HibUser user() {
		return getUserInfo().getUser();
	}

	@Getter
	public HibGroup group() {
		return getUserInfo().getGroup();
	}

	public HibRole getAnonymousRole() {
		return roles.get("anonymous");
	}

	@Getter
	public String projectUuid() {
		return projectUuid;
	}

	public String getContentUuid() {
		return contentUuid;
	}

	@Getter
	public String branchUuid() {
		return branchUuid;
	}

	public long getContentCount() {
		return contentCount;
	}

	/**
	 * Create a new batch for operations.
	 * 
	 * @return
	 */
	public EventQueueBatch createBatch() {
		return queueProvider.get();
	}

}
