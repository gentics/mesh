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
public class DummyDataProvider {

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

	private Content content;

	private Tag rootTag;

	private Language english;

	private Language german;

	private Project dummyProject;

	private ObjectSchema contentSchema;

	private HashMap<UserInfoType, UserInfo> userInfos = new HashMap<>();

	private DummyDataProvider() {
	}

	public UserInfo addUserInfo(UserInfoType type, String firstname, String lastname) {

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
		return userInfo;

	}

	/**
	 * Setup a basic project and dummy test data. Tag hierarchy: [/] -> [subtag, unterTag] -> [subtag2, untertag2] Content hierarchy: [subtag, untertag] ->
	 * ["english.html","german.html"]
	 */
	public void setup() {

		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			// User, Groups, Roles
			addUserInfo(UserInfoType.none, "Spider", "man");
			addUserInfo(UserInfoType.read, "Rocket", "Raccoon");
			addUserInfo(UserInfoType.read_update, "Black", "Widow");
			addUserInfo(UserInfoType.read_update_create, "Star", "Lord");
			addUserInfo(UserInfoType.read_create, "Super", "Man");
			addUserInfo(UserInfoType.read_create_delete, "Iron", "Man");
			addUserInfo(UserInfoType.all, "Tony", "Stark");

			Group adminGroup = userInfos.get(UserInfoType.all).getGroup();
			User defaultUser = userInfos.get(UserInfoType.all).getUser();
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

			CaiLunRoot rootNode = new CaiLunRoot();
			rootNode.setRootGroup(adminGroup);
			rootNode.addLanguage(english);
			rootNode.addLanguage(german);
			userInfos.values().stream().forEach(e -> {
				rootNode.addUser(e.getUser());
			});
			rootService.save(rootNode);

			// Root Tag
			rootTag = new Tag();
			tagService.setName(rootTag, english, "/");
			rootTag = tagService.save(rootTag);

			// Sub Tag
			Tag subTag = new Tag();
			tagService.setName(subTag, english, "subtag");
			tagService.setName(subTag, german, "unterTag");
			subTag = tagService.save(subTag);
			rootTag.addTag(subTag);
			tagService.save(rootTag);

			// Sub Tag 2
			Tag subTag2 = new Tag();
			tagService.setName(subTag2, english, "subtag2");
			tagService.setName(subTag2, german, "unterTag2");
			subTag2 = tagService.save(subTag2);
			subTag.addTag(subTag2);
			subTag = tagService.save(subTag);

			dummyProject = new Project(PROJECT_NAME);
			dummyProject.setRootTag(rootTag);
			// TODO add schema
			dummyProject = projectService.save(dummyProject);
			dummyProject = projectService.reload(dummyProject);

			content = new Content();
			contentService.setName(content, english, "english content name");
			contentService.setFilename(content, english, "english.html");
			contentService.setContent(content, english, ENGLISH_CONTENT);

			contentService.setName(content, german, "german content name");
			contentService.setFilename(content, german, "german.html");
			contentService.setContent(content, german, "mahlzeit!");
			// TODO maybe set project should be done inside the save?
			content.setProject(dummyProject);
			content.setCreator(defaultUser);
			content = contentService.save(content);

			subTag.addFile(content);
			subTag = tagService.save(subTag);

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

			tx.success();
		}
		// Reload various contents to refresh them and load the uuid field
		content = contentService.reload(content);
		dummyProject = projectService.reload(dummyProject);
		contentSchema = objectSchemaService.reload(contentSchema);

		userInfos.values().stream().forEach(i -> {
			i.setGroup(groupService.reload(i.getGroup()));
			i.setUser(userService.reload(i.getUser()));
			i.setRole(roleService.reload(i.getRole()));
		});

	}

	public Tag getRootTag() {
		return rootTag;
	}

	public Content getContent() {
		if (content == null) {
			throw new NotInitializedException("Dummy data not yet setup. Invoke setup first.");
		}
		return content;
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
