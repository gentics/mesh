package com.gentics.cailun.test;

import static org.junit.Assert.assertNotNull;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.exceptions.NotInitializedException;

import com.gentics.cailun.core.rest.model.CaiLunRoot;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.service.CaiLunRootService;
import com.gentics.cailun.core.rest.service.ContentService;
import com.gentics.cailun.core.rest.service.GroupService;
import com.gentics.cailun.core.rest.service.LanguageService;
import com.gentics.cailun.core.rest.service.ProjectService;
import com.gentics.cailun.core.rest.service.RoleService;
import com.gentics.cailun.core.rest.service.TagService;
import com.gentics.cailun.core.rest.service.UserService;
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
			projectService.save(dummyProject);

			content = new Content();
			contentService.setName(content, english, "english content name");
			contentService.setFilename(content, english, "english.html");
			contentService.setContent(content, english, ENGLISH_CONTENT);

			contentService.setName(content, german, "german content name");
			contentService.setFilename(content, german, "german.html");
			contentService.setContent(content, german, "mahlzeit!");
			content = contentService.save(content);

			subTag.addFile(content);
			subTag = tagService.save(subTag);
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
