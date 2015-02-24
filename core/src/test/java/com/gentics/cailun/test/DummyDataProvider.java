package com.gentics.cailun.test;

import static org.junit.Assert.assertNotNull;

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

	public static final String USER_JOE_USERNAME = "joe1";
	public static final String USER_JOE_PASSWORD = "test1234";
	public static final String PROJECT_NAME = "dummy";
	public static final String ENGLISH_CONTENT = "blessed mealtime!";

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

	private Content content;

	private Tag rootTag;

	private DummyDataProvider() {
	}

	/**
	 * Setup a basic project and dummy test data. Tag hierarchy: [/] -> [subtag, unterTag] -> [subtag2, untertag2] Content hierarchy: [subtag, untertag] ->
	 * ["english.html","german.html"]
	 */
	public void setup() {

		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			// User, Groups, Roles
			User testUser = new User(USER_JOE_USERNAME);
			userService.setPassword(testUser, USER_JOE_PASSWORD);
			testUser.setFirstname("Joe");
			testUser.setLastname("Doe");
			testUser.setEmailAddress("j.doe@gentics.com");
			userService.save(testUser);
			assertNotNull(userService.findByUsername(USER_JOE_USERNAME));

			Role adminRole = new Role("superadmin");
			roleService.save(adminRole);

			Group adminGroup = new Group("admin");
			adminGroup.addUser(testUser);
			adminGroup.addRole(adminRole);
			groupService.save(adminGroup);

			// Contents, Tags, Projects

			Language english = new Language("english", "en_US");
			english = languageService.save(english);
			Language german = new Language("german", "de_DE");
			german = languageService.save(german);

			CaiLunRoot rootNode = new CaiLunRoot();
			rootNode.setRootGroup(adminGroup);
			rootNode.addLanguage(english);
			rootNode.addLanguage(german);
			rootNode.addUser(testUser);
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

			Project dummyProject = new Project(PROJECT_NAME);
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
			content.setCreator(testUser);
			content = contentService.save(content);

			subTag.addFile(content);
			subTag = tagService.save(subTag);

			// Save the default object schema
			ObjectSchema contentSchema = new ObjectSchema("content");
			contentSchema.setProject(dummyProject);
			contentSchema.setDescription("Default schema for contents");
			contentSchema.setCreator(testUser);
			contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.NAME_KEYWORD, PropertyType.I18N_STRING));
			contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericFile.FILENAME_KEYWORD, PropertyType.I18N_STRING));
			contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.CONTENT_KEYWORD, PropertyType.I18N_STRING));
			assertNotNull(objectSchemaService.save(contentSchema));

			tx.success();
		}
		content = contentService.findOne(content.getId());
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
}
