package com.gentics.mesh.test;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class TestTinyDataProvider implements TestDataProvider {

	private static final Logger log = LoggerFactory.getLogger(TestTinyDataProvider.class);

	public static final String PROJECT_NAME = "dummy";
	public static final String TAG_CATEGORIES_SCHEMA_NAME = "tagCategories";
	public static final String TAG_DEFAULT_SCHEMA_NAME = "tag";

	private static TestTinyDataProvider instance;

	public static TestTinyDataProvider getInstance() {
		return instance;
	}
	
	private Map<String, SchemaContainer> schemaContainers = new HashMap<>();


	private Database db;

	private BootstrapInitializer boot;

	// References to dummy data

	private Language english;

	private Language german;

	private Project project;

	private UserInfo userInfo;

	private MeshRoot root;

	private Map<String, User> users = new HashMap<>();
	private Map<String, Role> roles = new HashMap<>();
	private Map<String, Group> groups = new HashMap<>();

	@Inject
	public TestTinyDataProvider(BootstrapInitializer boot, Database database) {
		this.boot = boot;
		this.db = database;
		instance = this;
	}

	public void setup() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		long start = System.currentTimeMillis();

		try (Tx tx = db.tx()) {
			boot.initMandatoryData();
			tx.getGraph().commit();
			users.clear();
			roles.clear();
			groups.clear();

			root = boot.meshRoot();
			english = boot.languageRoot().findByLanguageTag("en");
			german = boot.languageRoot().findByLanguageTag("de");

			addBootstrappedData();
			addBootstrapSchemas();
			addUserGroupRoleProject();

			tx.getGraph().commit();
		}

		long duration = System.currentTimeMillis() - start;
		log.debug("Setup took: {" + duration + "}");
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
	
	private void addBootstrapSchemas() {

		// folder
		SchemaContainer folderSchemaContainer = boot.schemaContainerRoot().findByName("folder");
		schemaContainers.put("folder", folderSchemaContainer);

		// content
		SchemaContainer contentSchemaContainer = boot.schemaContainerRoot().findByName("content");
		schemaContainers.put("content", contentSchemaContainer);

		// binary-content
		SchemaContainer binaryContentSchemaContainer = boot.schemaContainerRoot().findByName("binary-content");
		schemaContainers.put("binary-content", binaryContentSchemaContainer);

	}

	public UserInfo createUserInfo(String username, String firstname, String lastname) {

		String password = "test123";
		log.debug("Creating user with username: " + username + " and password: " + password);

		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";
		User user = root.getUserRoot().create(username, null);
		user.setPassword(password);
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

		project = root.getProjectRoot().create(PROJECT_NAME, userInfo.getUser(),
				getSchemaContainer("folder").getLatestVersion());
		project.addLanguage(getEnglish());
		project.addLanguage(getGerman());
		project.getSchemaContainerRoot().addSchemaContainer(getSchemaContainer("folder"));
		project.getSchemaContainerRoot().addSchemaContainer(getSchemaContainer("content"));
		project.getSchemaContainerRoot().addSchemaContainer(getSchemaContainer("binary-content"));

		// Guest Group / Role
		Group guestGroup = root.getGroupRoot().create("guests", userInfo.getUser());
		groups.put("guests", guestGroup);

		Role guestRole = root.getRoleRoot().create("guest_role", userInfo.getUser());
		guestGroup.addRole(guestRole);
		roles.put(guestRole.getName(), guestRole);


		// Publish the project basenode
		project.getBaseNode().publish(getEnglish(), getProject().getLatestRelease(), getUserInfo().getUser());

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


	public Map<String, User> getUsers() {
		return users;
	}

	public Map<String, Group> getGroups() {
		return groups;
	}

	public Map<String, Role> getRoles() {
		return roles;
	}

	public MeshRoot getMeshRoot() {
		return root;
	}

	@Override
	public TagFamily getTagFamily(String key) {
		return null;
	}

	@Override
	public Node getContent(String key) {
		return null;
	}

	@Override
	public Node getFolder(String key) {
		return null;
	}

	@Override
	public Map<String, TagFamily> getTagFamilies() {
		return null;
	}

	@Override
	public Tag getTag(String key) {
		return null;
	}

	@Override
	public Map<String, ? extends Tag> getTags() {
		return null;
	}

	@Override
	public int getNodeCount() {
		return 0;
	}

	@Override
	public SchemaContainer getSchemaContainer(String key) {
		return schemaContainers.get(key);
	}

	@Override
	public Map<String, SchemaContainer> getSchemaContainers() {
		return schemaContainers;
	}

	@Override
	public Map<String, MicroschemaContainer> getMicroschemaContainers() {
		return null;
	}

}
