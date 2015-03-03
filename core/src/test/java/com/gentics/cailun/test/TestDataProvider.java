package com.gentics.cailun.test;

import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.exceptions.NotInitializedException;

import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.PropertyType;
import com.gentics.cailun.core.data.model.PropertyTypeSchema;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericContent;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.CaiLunRootService;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@Component
public class TestDataProvider {

	public static final String PROJECT_NAME = "dummy";
	public static final String ENGLISH_CONTENT = "blessed mealtime!";

	public enum UserInfoType {
		read_create, all, none, read, read_create_delete, read_update_create, read_update
	}

	private static SecureRandom random = new SecureRandom();

	@Autowired
	private UserService userService;

	@Autowired
	private CaiLunRootService rootService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private TagService tagService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	protected CaiLunSpringConfiguration springConfig;

	// References to dummy data

	private Tag rootTag;

	private Language english;

	private Language german;

	private Project dummyProject;

	private ObjectSchema contentSchema;

	private HashMap<UserInfoType, UserInfo> userInfos = new HashMap<>();

	private User defaultUser;

	private Content contentLevel1A1;
	private Content contentLevel1A2;
	private Content contentLevel1A3;

	private Content contentLevel2C1;
	private Content contentLevel2C2;
	private Content contentLevel2C3;

	private Tag level1a;
	private Tag level1b;
	private Tag level1c;

	private Tag level2a;
	private Tag level2b;
	private Tag level2c;

	private TestDataProvider() {
	}

	public Role addUserInfo(UserInfoType type, String firstname, String lastname) {

		String password = new BigInteger(130, random).toString(32);
		String username = type.name() + "_perm_user";
		String email = firstname.toLowerCase().substring(0, 1) + "." + lastname.toLowerCase() + "@spam.gentics.com";

		User user = new User(username);
		userService.setPassword(user, password);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setEmailAddress(email);
		userService.save(user);
		assertNotNull(userService.findByUsername(username));

		Role role = new Role(type.name() + "_role");
		roleService.save(role);

		Group group = new Group(type.name() + "_group");
		group.addUser(user);
		group.addRole(role);
		group = groupService.save(group);

		UserInfo userInfo = new UserInfo(user, group, role, password);
		userInfos.put(type, userInfo);
		return role;

	}

	/**
	 * Setup a basic project and dummy test data. Tag hierarchy: [/] -> [subtag, unterTag] -> [subtag2, untertag2] Content hierarchy: [subtag, untertag] ->
	 * ["english.html","german.html"]
	 */
	public void setup() {

		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {

			CaiLunRoot root = new CaiLunRoot();
			dummyProject = new Project(PROJECT_NAME);

			// User, Groups, Roles
			addUserInfo(UserInfoType.none, "Spider", "man");
			addUserInfo(UserInfoType.read, "Rocket", "Raccoon");
			addUserInfo(UserInfoType.read_update, "Black", "Widow");
			addUserInfo(UserInfoType.read_update_create, "Star", "Lord");
			addUserInfo(UserInfoType.read_create, "Super", "Man");
			addUserInfo(UserInfoType.read_create_delete, "Iron", "Man");
			Role allRole = addUserInfo(UserInfoType.all, "Tony", "Stark");

			// Setup group hierarchy
			Group adminGroup = userInfos.get(UserInfoType.all).getGroup();
			defaultUser = userInfos.get(UserInfoType.all).getUser();
			userInfos.values().stream().forEach(e -> {
				if (adminGroup.getId() != e.getGroup().getId()) {
					adminGroup.addGroup(e.getGroup());
				}
			});
			groupService.save(adminGroup);

			// Contents, Tags, Projects
			english = new Language("english", "en_US");
			english = languageService.save(english);
			german = new Language("german", "de_DE");
			german = languageService.save(german);

			root.setRootGroup(adminGroup);
			root.addLanguage(english);
			root.addLanguage(german);
			userInfos.values().stream().forEach(e -> {
				root.addUser(e.getUser());
			});
			rootService.save(root);

			// Root Tag
			rootTag = new Tag();
			tagService.setName(rootTag, english, "/");
			rootTag = tagService.save(rootTag);
			dummyProject.setRootTag(rootTag);
			dummyProject = projectService.save(dummyProject);
			dummyProject = projectService.reload(dummyProject);

			// Subtags
			level1a = addTag(rootTag, "level_1_a", "ebene_1_a");
			level1b = addTag(rootTag, "level_1_b", "ebene_1_b");
			level1c = addTag(rootTag, "level_1_c", "ebene_1_c");
			contentLevel1A1 = addContent(level1a, "test_1", "Mahlzeit 1!", "Blessed Mealtime 1!");
			contentLevel1A2 = addContent(level1a, "test_2", "Mahlzeit 2!", "Blessed Mealtime 2!");
			contentLevel1A3 = addContent(level1a, "test_3", "Mahlzeit 3!", "Blessed Mealtime 3!");

			level2a = addTag(level1a, "level_2_a", "ebene_2_a");
			level2b = addTag(level1a, "level_2_b", "ebene_2_b");
			level2c = addTag(level1a, "level_2_c", "ebene_2_c");
			contentLevel2C1 = addContent(level2c, "test_1", "Mahlzeit 1!", "Blessed Mealtime 1!");
			contentLevel2C2 = addContent(level2c, "test_2", "Mahlzeit 2!", "Blessed Mealtime 2!");
			contentLevel2C3 = addContent(level2c, "test_3", "Mahlzeit 3!", "Blessed Mealtime 3!");

			// Save the default object schema
			contentSchema = new ObjectSchema("content");
			contentSchema.setProject(dummyProject);
			contentSchema.setDescription("Default schema for contents");
			contentSchema.setCreator(defaultUser);
			contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.NAME_KEYWORD, PropertyType.I18N_STRING));
			contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericFile.FILENAME_KEYWORD, PropertyType.I18N_STRING));
			contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.CONTENT_KEYWORD, PropertyType.I18N_STRING));
			assertNotNull(objectSchemaService.save(contentSchema));

			ObjectSchema customSchema = new ObjectSchema("custom-content");
			customSchema.setProject(dummyProject);
			customSchema.setDescription("Custom schema for contents");
			customSchema.setCreator(defaultUser);
			customSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.NAME_KEYWORD, PropertyType.I18N_STRING));
			customSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericFile.FILENAME_KEYWORD, PropertyType.I18N_STRING));
			customSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.CONTENT_KEYWORD, PropertyType.I18N_STRING));
			customSchema.addPropertyTypeSchema(new PropertyTypeSchema("secret", PropertyType.STRING));
			assertNotNull(objectSchemaService.save(customSchema));

			// Setup permissions

			contentLevel1A1.addPermission(new GraphPermission(allRole, contentLevel1A1));
			tx.success();
		}
		// Reload various contents to refresh them and load the uuid field
		contentLevel1A1 = contentService.reload(contentLevel1A1);
		contentLevel1A2 = contentService.reload(contentLevel1A2);
		contentLevel1A3 = contentService.reload(contentLevel1A3);

		contentLevel2C1 = contentService.reload(contentLevel2C1);
		contentLevel2C2 = contentService.reload(contentLevel2C2);
		contentLevel2C3 = contentService.reload(contentLevel2C3);

		level1a = tagService.reload(level1a);
		level1b = tagService.reload(level1b);
		level1c = tagService.reload(level1c);

		level2a = tagService.reload(level2a);
		level2b = tagService.reload(level2b);
		level2c = tagService.reload(level2c);

		dummyProject = projectService.reload(dummyProject);
		contentSchema = objectSchemaService.reload(contentSchema);

		userInfos.values().stream().forEach(i -> {
			i.setGroup(groupService.reload(i.getGroup()));
			i.setUser(userService.reload(i.getUser()));
			i.setRole(roleService.reload(i.getRole()));
		});

	}

	private Tag addTag(Tag rootTag, String germanName, String englishName) {
		Tag tag = new Tag();
		tagService.setName(tag, english, "subtag");
		tagService.setName(tag, german, "unterTag");
		tag = tagService.save(tag);
		rootTag.addTag(tag);
		tagService.save(rootTag);
		return tag;
	}

	private Content addContent(Tag tag, String name, String germanContent, String englishContent) {
		Content content = new Content();
		contentService.setName(content, english, name + " english");
		contentService.setFilename(content, english, name + ".en.html");
		contentService.setContent(content, english, ENGLISH_CONTENT);

		contentService.setName(content, german, name + " german");
		contentService.setFilename(content, german, name + ".de.html");
		contentService.setContent(content, german, germanContent);
		// TODO maybe set project should be done inside the save?
		content.setProject(dummyProject);
		content.setCreator(defaultUser);
		content = contentService.save(content);

		// Add the content to the given tag
		tag.addFile(content);
		tag = tagService.save(tag);

		return content;
	}

	public Tag getRootTag() {
		return rootTag;
	}

	public Content getContentLevel1A1() {
		return contentLevel1A1;
	}

	public Content getContentLevel1A2() {
		return contentLevel1A2;
	}

	public Content getContentLevel1A3() {
		return contentLevel1A3;
	}

	public Content getContentLevel2C1() {
		return contentLevel2C1;
	}

	public Content getContentLevel2C2() {
		return contentLevel2C2;
	}

	public Content getContentLevel2C3() {
		return contentLevel2C3;
	}

	public Tag getLevel1a() {
		return level1a;
	}

	public Tag getLevel1b() {
		return level1b;
	}

	public Tag getLevel1c() {
		return level1c;
	}

	public Tag getLevel2a() {
		return level2a;
	}

	public Tag getLevel2b() {
		return level2b;
	}

	public Tag getLevel2c() {
		return level2c;
	}

	public Language getEnglish() {
		return english;
	}

	public Language getGerman() {
		return german;
	}

	public Project getProject() {
		return dummyProject;
	}

	public ObjectSchema getContentSchema() {
		return contentSchema;
	}

	public UserInfo getUserInfoNone() {
		return userInfos.get(UserInfoType.none);
	}

	public UserInfo getUserInfoAll() {
		return userInfos.get(UserInfoType.all);
	}

	public UserInfo getUserInfoRead() {
		return userInfos.get(UserInfoType.read);
	}

	public UserInfo getUserInfoReadUpdate() {
		return userInfos.get(UserInfoType.read_update);
	}

	public UserInfo getUserInfoReadUpdateCreate() {
		return userInfos.get(UserInfoType.read_update_create);
	}

	public UserInfo getUserInfoReadCreateDelete() {
		return userInfos.get(UserInfoType.read_create_delete);
	}

	public UserInfo getUserInfoReadCreate() {
		return userInfos.get(UserInfoType.read_create);
	}

}
