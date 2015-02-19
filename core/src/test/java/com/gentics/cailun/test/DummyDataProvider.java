package com.gentics.cailun.test;

import static org.junit.Assert.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

@Component
public class DummyDataProvider {

	public static final String USER_JOE_USERNAME = "joe1";
	public static final String USER_JOE_PASSWORD = "test1234";
	public static final String PROJECT_NAME = "dummy";

	@Autowired
	UserService userService;

	@Autowired
	CaiLunRootService rootService;

	@Autowired
	GroupService groupService;

	@Autowired
	LanguageService languageService;

	@Autowired
	ContentService contentService;

	@Autowired
	TagService tagService;

	@Autowired
	RoleService roleService;

	@Autowired
	ProjectService projectService;

	private DummyDataProvider() {
	}

	public void setup() {

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

		Tag rootTag = new Tag();
		tagService.setName(rootTag, english, "root");
		tagService.save(rootTag);

		Tag subTag = new Tag();
		tagService.setName(subTag, english, "subtag");
		tagService.setName(subTag, german, "unterTag");
		tagService.save(subTag);

		Project dummyProject = new Project(PROJECT_NAME);
		dummyProject.setRootTag(rootTag);
		// TODO add schema
		projectService.save(dummyProject);

		Content content = new Content();
		contentService.setName(content, english, "english content name");
		contentService.setFilename(content, english, "english.html");
		contentService.setContent(content, english, "blessed mealtime!");

		contentService.setName(content, german, "german content name");
		contentService.setFilename(content, german, "german.html");
		contentService.setContent(content, german, "mahlzeit!");
		content = contentService.save(content);

		subTag.addFile(content);
		tagService.save(subTag);
	}
}
