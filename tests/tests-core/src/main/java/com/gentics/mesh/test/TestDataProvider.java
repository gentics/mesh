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
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
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
import com.gentics.mesh.json.MeshJsonException;
import com.github.jknack.handlebars.internal.lang3.tuple.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDataProvider {

	private static final Logger log = LoggerFactory.getLogger(TestDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String INITIAL_BRANCH_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	public static final String CONTENT_UUID = "43ee8f9ff71e4016ae8f9ff71e10161c";
	public static final String NEWS_UUID = "4b1346a2163a4ff89346a2163a9ff883";
	public static final String NEWS_2015_UUID = "1234567890abcdef1234567890abcdef";

	private static TestDataProvider instance;

	public static TestDataProvider getInstance() {
		return instance;
	}

	private Database db;

	private BootstrapInitializer boot;

	// References to dummy data

	private final String english = "en";
	private final String german = "de";
	private final String italian = "it";
	private final String french = "fr";

	private Project project;
	private String projectUuid;
	private String branchUuid;

	private UserInfo userInfo;

	private TestSize size;

	private Map<String, Schema> schemaContainers = new HashMap<>();
	private Map<String, Microschema> microschemaContainers = new HashMap<>();
	private Map<String, TagFamily> tagFamilies = new HashMap<>();
	private long contentCount = 0;
	private Map<String, Node> folders = new HashMap<>();
	private Map<String, Node> contents = new HashMap<>();
	private Map<String, Tag> tags = new HashMap<>();
	private Map<String, User> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

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
			boot.initBasicData(meshOptions);
			if (setAdminPassword) {
				setAdminPassword(tx);
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

			addBootstrappedData(tx);
			addSchemaContainers();
			addUserGroupRoleProject(tx);
			if (getSize() == FULL) {
				addMicroschemaContainers();
				addTagFamilies();
				addTags();
			}
			addFolderStructure(tx);
			if (getSize() == FULL) {
				addContents(tx);
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

	private void setAdminPassword(Tx tx) {
		String hash = "$2a$10$X7NA0kiqrFlyX0NUhPdW1e7jevHyoaoB4OyoxV1pdA7B3SLVSkx22";
		UserDao userDao = tx.userDao();
		userDao.updatePasswordHash(userDao.findByUsername("admin"), hash);
	}

	private void addPermissions(BaseElement element) {
		addPermissions(Arrays.asList(element));
	}

	public TestSize getSize() {
		return size;
	}

	private void addPermissions(Collection<? extends BaseElement> elements) {
		RoleDao roleDao = Tx.get().roleDao();

		Role role = userInfo.getRole();
		for (BaseElement meshVertex : elements) {
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
		for (Group group : tx.groupDao().findAll()) {
			groups.put(group.getName(), group);
		}
		for (User user : tx.userDao().findAll()) {
			users.put(user.getUsername(), user);
		}
		for (Role role : tx.roleDao().findAll()) {
			roles.put(role.getName(), role);
		}
	}

	private void addContents(Tx tx) {
		TagDao tagDao = tx.tagDao();

		addContent(tx, folders.get("2014"), "News_2014", "News!", "Neuigkeiten!");
		addContent(tx, folders.get("march"), "New_in_March_2014", "This is new in march 2014.", "Das ist neu im März 2014");

		Node content = addContent(tx, folders.get("news"), "News Overview", "News Overview", "News Übersicht",
			CONTENT_UUID);
		contentUuid = content.getUuid();

		addContent(tx, folders.get("deals"), "Super Special Deal 2015", "Buy two get nine!",
			"Kauf zwei und nimm neun mit!");
		addContent(tx, folders.get("deals"), "Special Deal June 2015", "Buy two get three!",
			"Kauf zwei und nimm drei mit!");

		addContent(tx, folders.get("2015"), "Special News_2014", "News!", "Neuigkeiten!");
		addContent(tx, folders.get("2015"), "News_2015", "News!", "Neuigkeiten!", NEWS_2015_UUID);

		Node concorde = addContent(tx, folders.get("products"), "Concorde",
			"Aérospatiale-BAC Concorde is a turbojet-powered supersonic passenger jet airliner that was in service from 1976 to 2003.",
			"Die Aérospatiale-BAC Concorde 101/102, kurz Concorde (französisch und englisch für Eintracht, Einigkeit), ist ein Überschall-Passagierflugzeug, das von 1976 bis 2003 betrieben wurde.");
		tagDao.addTag(concorde, tags.get("plane"), project.getLatestBranch());
		tagDao.addTag(concorde, tags.get("twinjet"), project.getLatestBranch());
		tagDao.addTag(concorde, tags.get("red"), project.getLatestBranch());

		Node hondaNR = addContent(tx, folders.get("products"), "Honda NR",
			"The Honda NR (New Racing) was a V-four motorcycle engine series started by Honda in 1979 with the 500cc NR500 Grand Prix racer that used oval pistons.",
			"Die NR750 ist ein Motorrad mit Ovalkolben-Motor des japanischen Motorradherstellers Honda, von dem in den Jahren 1991 und 1992 300 Exemplare gebaut wurden.");
		tagDao.addTag(hondaNR, tags.get("vehicle"), project.getLatestBranch());
		tagDao.addTag(hondaNR, tags.get("motorcycle"), project.getLatestBranch());
		tagDao.addTag(hondaNR, tags.get("green"), project.getLatestBranch());

	}

	private void addFolderStructure(Tx tx) {
		TagDao tagDao = tx.tagDao();

		Node baseNode = project.getBaseNode();
		// rootNode.addProject(project);

		Node news = addFolder(tx, baseNode, "News", "Neuigkeiten", NEWS_UUID);
		Node news2015 = addFolder(tx, news, "2015", null);
		if (getSize() == FULL) {
			tagDao.addTag(news2015, tags.get("car"), project.getLatestBranch());
			tagDao.addTag(news2015, tags.get("bike"), project.getLatestBranch());
			tagDao.addTag(news2015, tags.get("plane"), project.getLatestBranch());
			tagDao.addTag(news2015, tags.get("jeep"), project.getLatestBranch());

			Node news2014 = addFolder(tx, news, "2014", null);
			addFolder(tx, news2014, "March", "März");

			addFolder(tx, baseNode, "Products", "Produkte");
			addFolder(tx, baseNode, "Deals", "Angebote");
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
		UserDao userDao = Tx.get().userDao();
		GroupDao groupDao = Tx.get().groupDao();
		RoleDao roleDao = Tx.get().roleDao();

		String groupName = username + "_group";
		String roleName = username + "_role";

		User user = userDao.findByUsername(username);
		Group group = groupDao.findByName(groupName);
		Role role = roleDao.findByName(roleName);
		String password = "test123";

		if (user == null) {
			String hashedPassword = "$2a$10$n/UeWGbY9c1FHFyCqlVsY.XvNYmZ7Jjgww99SF94q/B5nomYuquom";

			log.debug("Creating user with username: " + username + " and password: " + password);

			String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";
			user = userDao.create(username, null);
			// Precomputed hash since hashing takes some time and we want to keep out tests
			// fast
			userDao.updatePasswordHash(user, hashedPassword);
			user.setFirstname(firstname);
			user.setLastname(lastname);
			user.setEmailAddress(email);

			user.setCreator(user);
			user.setCreationTimestamp();
			user.setEditor(user);
			user.setLastEditedTimestamp();
			users.put(username, user);
		}
		if (group == null) {
			group = groupDao.create(groupName, user);
			groupDao.addUser(group, user);
			group.setCreator(user);
			group.setCreationTimestamp();
			group.setEditor(user);
			group.setLastEditedTimestamp();
			groups.put(groupName, group);
		}
		if (role == null) {
			role = roleDao.create(roleName, user);
			groupDao.addRole(group, role);
			roleDao.grantPermissions(role, role, READ_PERM);
			roles.put(roleName, role);
		}
		return new UserInfo(user, group, role, password);
	}

	private void addUserGroupRoleProject(Tx tx) {
		UserDao userDao = tx.userDao();
		RoleDao roleDao = tx.roleDao();
		GroupDao groupDao = tx.groupDao();
		SchemaDao schemaDao = tx.schemaDao();
		ProjectDao projectDao = tx.projectDao();

		// User, Groups, Roles
		userInfo = createUserInfo("joe1", "Joe", "Doe");
		EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
		tx.commit();
		tx.<CommonTx>unwrap().data().setEventQueueBatch(batch);
		batch.dispatch();
		project = projectDao.findByName(PROJECT_NAME);
		if (project == null) {
			project = projectDao.create(PROJECT_NAME, null, null, null, userInfo.getUser(),
					getSchemaContainer("folder").getLatestVersion(), batch);
		}
		if (project.findLanguageByTag(getEnglish()) == null) {
			project.addLanguage(tx.languageDao().findByLanguageTag(getEnglish()));
		}
		if (project.findLanguageByTag(getGerman()) == null) {
			project.addLanguage(tx.languageDao().findByLanguageTag(getGerman()));
		}		
		User jobUser = userInfo.getUser();
		//schemaDao.assign(getSchemaContainer("folder"), project, jobUser, batch); // already done
		schemaDao.assign(getSchemaContainer("content"), project, jobUser, batch);
		schemaDao.assign(getSchemaContainer("binary_content"), project, jobUser, batch);
		projectUuid = project.getUuid();
		branchUuid = project.getInitialBranch().getUuid();

		if (getSize() == FULL) {
			// Guest Group / Role
			Group guestGroup = groupDao.findByName("guests");
			if (guestGroup == null) {
				guestGroup = groupDao.create("guests", userInfo.getUser());
			}
			groups.put("guests", guestGroup);

			Role guestRole = roleDao.findByName("guest_role");
			if (guestRole == null) {
				guestRole = roleDao.create("guest_role", userInfo.getUser());				
			}
			groupDao.addRole(guestGroup, guestRole);
			roles.put(guestRole.getName(), guestRole);

			// Extra User
			User user = userDao.findByName("guest");
			if (user == null) {
				user = userDao.create("guest", userInfo.getUser());
				userDao.addGroup(user, guestGroup);
				user.setFirstname("Guest Firstname");
				user.setLastname("Guest Lastname");
				user.setEmailAddress("guest@spam.gentics.com");
			}
			users.put(user.getUsername(), user);

			Group group = groupDao.findByName("extra_group");
			if (group == null) {
				group = groupDao.create("extra_group", userInfo.getUser());
			}
			groups.put(group.getName(), group);

			Role role = roleDao.findByName("extra_role");
			if (role == null) {
				role = roleDao.create("extra_role", userInfo.getUser());
			}
			roles.put(role.getName(), role);
		}
		// Publish the project basenode
		InternalActionContext ac = new NodeMigrationActionContextImpl();
		tx.contentDao().publish(project.getBaseNode(), ac, getEnglish(), getProject().getLatestBranch(),
			getUserInfo().getUser());
		contentCount++;
	}

	public void addTagFamilies() {
		TagFamilyDao tagFamilyDao = Tx.get().tagFamilyDao();
		TagFamily basicTagFamily = tagFamilyDao.create(getProject(), "basic", userInfo.getUser());
		basicTagFamily.setDescription("Description for basic tag family");
		tagFamilies.put("basic", basicTagFamily);

		TagFamily colorTagFamily = tagFamilyDao.create(getProject(), "colors", userInfo.getUser());
		colorTagFamily.setDescription("Description for color tag family");
		tagFamilies.put("colors", colorTagFamily);
	}

	private void addSchemaContainers() throws MeshSchemaException {
		addBootstrapSchemas();
		Tx.get().commit();
	}

	private void addBootstrapSchemas() {
		SchemaDao schemaDao = Tx.get().schemaDao();

		// folder
		Schema folderSchemaContainer = schemaDao.findByName("folder");
		schemaContainers.put("folder", folderSchemaContainer);

		// content
		Schema contentSchemaContainer = schemaDao.findByName("content");
		schemaContainers.put("content", contentSchemaContainer);

		// binary_content
		Schema binaryContentSchemaContainer = schemaDao.findByName("binary_content");
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
		MicroschemaDao microschemaDao = Tx.get().microschemaDao();

		Microschema vcardMicroschemaContainer = microschemaDao.findByName("vcard");
		if (vcardMicroschemaContainer == null) {

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
			vcardMicroschemaContainer = microschemaDao.create(vcardMicroschema, userInfo.getUser(), createBatch());
		}
		microschemaContainers.put(vcardMicroschemaContainer.getName(), vcardMicroschemaContainer);
		microschemaDao.assign(vcardMicroschemaContainer, project, user(), createBatch());
	}

	/**
	 * Add microschema "captionedImage" to db
	 * 
	 * @throws MeshJsonException
	 */
	private void addCaptionedImageMicroschema() throws MeshJsonException {
		MicroschemaDao microschemaDao = Tx.get().microschemaDao();

		Microschema microschema = microschemaDao.findByName("captionedImage");
		if (microschema == null) {
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
			microschema = microschemaDao.create(captionedImageMicroschema, userInfo.getUser(), createBatch());
		}
		microschemaContainers.put(microschema.getName(), microschema);
		microschemaDao.assign(microschema, project, user(), createBatch());
	}

	public Node addFolder(Tx tx, Node rootNode, String englishName, String germanName) {
		return addFolder(tx, rootNode, englishName, germanName, null);
	}

	public Node addFolder(Tx tx, Node rootNode, String englishName, String germanName, String uuid) {
		NodeDao nodeDao = tx.nodeDao();
		ContentDao contentDao = tx.contentDao();
		InternalActionContext ac = new NodeMigrationActionContextImpl();
		SchemaVersion schemaVersion = schemaContainers.get("folder").getLatestVersion();
		Branch branch = project.getLatestBranch();
		Node folderNode;
		if (uuid == null) {
			folderNode = nodeDao.create(rootNode, userInfo.getUser(), schemaVersion, project);
		} else {
			folderNode = nodeDao.create(rootNode, userInfo.getUser(), schemaVersion, project, branch, uuid);
		}
		if (germanName != null) {
			NodeFieldContainer germanContainer = contentDao.createFieldContainer(folderNode, german,
				branch, userInfo.getUser());
			germanContainer.createString("slug").setString(germanName);
			contentDao.updateDisplayFieldValue(germanContainer);
			contentCount++;
			contentDao.publish(folderNode, ac, getGerman(), branch, getUserInfo().getUser());
		}
		if (englishName != null) {
			NodeFieldContainer englishContainer = contentDao.createFieldContainer(folderNode, english,
				branch, userInfo.getUser());
			englishContainer.createString("name").setString(englishName);
			englishContainer.createString("slug").setString(englishName);
			contentDao.updateDisplayFieldValue(englishContainer);
			contentCount++;
			contentDao.publish(folderNode, ac, getEnglish(), branch, getUserInfo().getUser());
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
		TagDao tagDao = Tx.get().tagDao();
		if (name == null || StringUtils.isEmpty(name)) {
			throw new RuntimeException("Name for tag empty");
		}
		Tag tag = tagDao.create(tagFamily, name, project, userInfo.getUser());
		tags.put(name.toLowerCase(), tag);
		return tag;
	}

	private Node addContent(Tx tx, Node parentNode, String name, String englishContent, String germanContent) {
		return addContent(tx, parentNode, name, englishContent, germanContent, null);
	}

	private Node addContent(Tx tx, Node parentNode, String name, String englishContent, String germanContent,
		String uuid) {
		NodeDao nodeDao = tx.nodeDao();
		ContentDao contentDao = tx.contentDao();
		InternalActionContext ac = new NodeMigrationActionContextImpl();
		Branch branch = project.getLatestBranch();
		Node node;
		if (uuid == null) {
			node = nodeDao.create(parentNode, userInfo.getUser(), schemaContainers.get("content").getLatestVersion(),
				project);
		} else {
			node = nodeDao.create(parentNode, userInfo.getUser(), schemaContainers.get("content").getLatestVersion(),
				project, branch, uuid);
		}
		if (englishContent != null) {
			NodeFieldContainer englishContainer = contentDao.createFieldContainer(node, english,
				branch, userInfo.getUser());
			englishContainer.createString("teaser").setString(name + "_english_name");
			englishContainer.createString("title").setString(name + " english title");
			englishContainer.createString("slug").setString(name + ".en.html");
			englishContainer.createHTML("content").setHtml(englishContent);
			contentDao.updateDisplayFieldValue(englishContainer);
			contentCount++;
			contentDao.publish(node, ac, getEnglish(), branch, getUserInfo().getUser());
		}

		if (germanContent != null) {
			NodeFieldContainer germanContainer = contentDao.createFieldContainer(node, german, branch,
				userInfo.getUser());
			germanContainer.createString("teaser").setString(name + " german");
			germanContainer.createString("title").setString(name + " german title");
			germanContainer.createString("slug").setString(name + ".de.html");
			germanContainer.createHTML("content").setHtml(germanContent);
			contentDao.updateDisplayFieldValue(germanContainer);
			contentCount++;
			contentDao.publish(node, ac, getGerman(), branch, getUserInfo().getUser());
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
	private String getPathForNews2015Tag(Tx tx, String languageTag) {
		ContentDao contentDao = tx.contentDao();

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

	public String getItalian() {
		return italian;
	}

	public String getFrench() {
		return french;
	}

	public Project getProject() {
		Tx.maybeGet().ifPresent(tx -> {
			project = tx.<CommonTx>unwrap().load(project.getId(), tx.<CommonTx>unwrap().projectDao().getPersistenceClass());
		});
		return project;
	}

	public UserInfo getUserInfo() {
		return userInfo;
	}

	@Getter
	public Node getFolder(String name) {
		return getBaseElement(name, folders);
	}

	@Getter
	public TagFamily getTagFamily(String key) {
		return getBaseElement(key, tagFamilies);
	}

	@Getter
	public Node getContent(String name) {
		return getBaseElement(name, contents);
	}

	@Getter
	public Tag getTag(String name) {
		return getBaseElement(name, tags);
	}

	@Getter
	public Schema getSchemaContainer(String name) {
		return getBaseElement(name, schemaContainers);
	}

	@Getter
	public Microschema getMicroschemaContainer(String name) {
		return getBaseElement(name, microschemaContainers);
	}

	@Getter
	public Role getRole(String name) {
		return getBaseElement(name, roles);
	}

	@SuppressWarnings("unchecked")
	private <T extends BaseElement> T getBaseElement(String name, Map<String, T> cache) {
		Tx.maybeGet().ifPresent(tx -> {
			T element = cache.get(name);
			CommonTx ctx = tx.unwrap();
			element = (T) ctx.load(element.getId(), (Class<T>) ctx.entityClassOf(element));
			cache.put(name, element);
		});
		return cache.get(name);
	}

	private <T extends BaseElement> Map<String, T> getBaseElements(Map<String, T> cache) {
		return cache.keySet().stream().map(key -> Pair.of(key, getBaseElement(key, cache))).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	public Map<String, Tag> getTags() {
		return getBaseElements(tags);
	}

	public Map<String, Node> getContents() {
		return getBaseElements(contents);
	}

	public Map<String, Node> getFolders() {
		return getBaseElements(folders);
	}

	public Map<String, User> getUsers() {
		return getBaseElements(users);
	}

	public Map<String, Group> getGroups() {
		return getBaseElements(groups);
	}

	public Map<String, Role> getRoles() {
		return getBaseElements(roles);
	}

	public Map<String, Schema> getSchemaContainers() {
		return getBaseElements(schemaContainers);
	}

	public Map<String, Microschema> getMicroschemaContainers() {
		return getBaseElements(microschemaContainers);
	}

	public Map<String, TagFamily> getTagFamilies() {
		return getBaseElements(tagFamilies);
	}

	public BootstrapInitializer getMeshBoot() {
		return boot;
	}

	public int getNodeCount() {
		// folders, contents + basenode
		return folders.size() + contents.size() + 1;
	}

	@Getter
	public Role role() {
		return getUserInfo().getRole();
	}

	@Getter
	public User user() {
		return getUserInfo().getUser();
	}

	@Getter
	public Group group() {
		return getUserInfo().getGroup();
	}

	public Role getAnonymousRole() {
		return getRole("anonymous");
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
