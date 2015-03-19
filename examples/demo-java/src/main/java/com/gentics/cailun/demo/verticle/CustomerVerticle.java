package com.gentics.cailun.demo.verticle;

import static com.gentics.cailun.core.data.model.auth.PermissionType.CREATE;
import static com.gentics.cailun.core.data.model.auth.PermissionType.DELETE;
import static com.gentics.cailun.core.data.model.auth.PermissionType.READ;
import static com.gentics.cailun.core.data.model.auth.PermissionType.UPDATE;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Session;

import java.util.Arrays;
import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.PropertyType;
import com.gentics.cailun.core.data.model.PropertyTypeSchema;
import com.gentics.cailun.core.data.model.RootTag;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericContent;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.service.CaiLunRootService;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

/**
 * Dummy verticle that is used to setup basic demo data
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class CustomerVerticle extends AbstractProjectRestVerticle {

	private static Logger log = LoggerFactory.getLogger(CustomerVerticle.class);

	@Autowired
	private CaiLunSpringConfiguration cailunConfig;

	@Autowired
	private UserService userService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private GenericNodeService<GenericNode> genericNodeService;

	@Autowired
	private TagService tagService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ObjectSchemaService objectSchemaService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private CaiLunRootService rootService;

	@Autowired
	private ObjectSchemaService schemaService;

	public CustomerVerticle() {
		super("Content");
	}

	/**
	 * Add a set of dummy users to the graph
	 * 
	 * @return
	 */
	private List<User> addUsers() {
		User john = new User("joe1");
		john.setFirstname("John");
		john.setLastname("Doe");
		john.setEmailAddress("j.doe@gentics.com");
		// TODO use user service
		john.setPasswordHash(cailunConfig.passwordEncoder().encode("test123"));

		User mary = new User("mary2");
		mary.setFirstname("Mary");
		mary.setLastname("Doe");
		mary.setEmailAddress("m.doe@gentics.com");
		mary.setPasswordHash(cailunConfig.passwordEncoder().encode("lalala"));
		List<User> users = Arrays.asList(john, mary);
		userService.save(users);
		return users;

	}

	@Override
	public void registerEndPoints() throws Exception {

		addPermissionTestHandler();

		try (Transaction tx = cailunConfig.getGraphDatabaseService().beginTx()) {
			setupDemoData();
			tx.success();
		}

	}

	private void setupDemoData() {
		// contentRepository.findCustomerNodeBySomeStrangeCriteria("dgasdg");

		CaiLunRoot rootNode = rootService.findRoot();

		Project aloha = new Project("aloha");
		aloha = projectService.save(aloha);

		// ObjectSchema contentSchema = new ObjectSchema("content");
		// contentSchema.setDescription("Default schema for contents");

		Language german = languageService.findByName("german");
		Language english = languageService.findByName("english");

		// Users
		List<User> users = addUsers();
		rootNode.getUsers().addAll(users);

		// Groups
		Group rootGroup = new Group("superusers");
		rootNode.getRootGroup().addGroup(rootGroup);

		// Roles
		Role adminRole = new Role("admin role");
		roleService.save(adminRole);
		Role guestRole = new Role("guest role");
		roleService.save(guestRole);

		// Groups
		rootGroup.getUsers().add(users.get(0));
		rootGroup.getRoles().add(adminRole);

		groupService.save(rootGroup);
		Group guests = new Group("guests");
		guests.getParents().add(rootGroup);
		guests.getUsers().add(users.get(1));
		guests.getRoles().add(guestRole);
		groupService.save(guests);

		// Tags
		RootTag rootTag = new RootTag();
		tagService.setName(rootTag, english, "/");

		Tag homeFolder = new Tag();
		tagService.setName(homeFolder, english, "home");
		tagService.setName(homeFolder, german, "heim");
		rootTag.addTag(homeFolder);

		Tag jotschiFolder = new Tag();
		tagService.setName(jotschiFolder, german, "jotschi");
		tagService.setName(jotschiFolder, english, "jotschi");
		homeFolder.addTag(jotschiFolder);

		Tag rootFolder = new Tag();
		tagService.setName(rootFolder, german, "wurzel");
		tagService.setName(rootFolder, english, "root");
		rootTag.addTag(rootFolder);

		Tag varFolder = new Tag();
		tagService.setName(varFolder, german, "var");
		rootTag.addTag(varFolder);

		Tag wwwFolder = new Tag();
		tagService.setName(wwwFolder, english, "www");
		varFolder.addTag(wwwFolder);

		Tag siteFolder = new Tag();
		tagService.setName(siteFolder, english, "site");
		wwwFolder.addTag(siteFolder);

		Tag postsFolder = new Tag();
		tagService.setName(postsFolder, german, "posts");
		wwwFolder.addTag(postsFolder);

		Tag blogsFolder = new Tag();
		tagService.setName(blogsFolder, german, "blogs");
		wwwFolder.addTag(blogsFolder);

		aloha.setRootTag(rootTag);
		projectService.save(aloha);

		// Save the default object schema
		ObjectSchema contentSchema = new ObjectSchema("content");
		contentSchema.addProject(aloha);
		contentSchema.setDescription("Default schema for contents");
		contentSchema.setCreator(users.get(0));
		contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.NAME_KEYWORD, PropertyType.I18N_STRING));
		contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericFile.FILENAME_KEYWORD, PropertyType.I18N_STRING));
		contentSchema.addPropertyTypeSchema(new PropertyTypeSchema(GenericContent.CONTENT_KEYWORD, PropertyType.I18N_STRING));
		objectSchemaService.save(contentSchema);

		// Contents
		Content rootContent = new Content();
		contentService.setName(rootContent, german, "german name");
		contentService.setFilename(rootContent, german, "german.html");
		contentService.setContent(rootContent, german, "Mahlzeit!");

		contentService.setName(rootContent, english, "english name");
		contentService.setFilename(rootContent, english, "english.html");
		contentService.setContent(rootContent, english, "Blessed mealtime!");

		rootContent.addProject(aloha);
		rootContent.setCreator(users.get(0));
		// rootContent.tag(rootTag);
		contentService.save(rootContent);

		rootContent = contentService.findOne(rootContent.getId());

		for (int i = 0; i < 6; i++) {
			Content content = new Content();
			contentService.setName(content, german, "Hallo Welt");
			contentService.setFilename(content, german, "some" + i + ".html");
			content.setCreator(users.get(0));
			contentService.setContent(content, german, "some content");
			content.addTag(blogsFolder);
			contentService.save(content);
		}

		for (int i = 0; i < 3; i++) {
			Content content = new Content();
			contentService.setName(content, german, "Hallo Welt");
			contentService.setFilename(content, german, "some_posts" + i + ".html");
			content.setCreator(users.get(0));
			contentService.setContent(content, german, "some content");
			content.addTag(postsFolder);
			contentService.save(content);
		}

		Content content = new Content();
		contentService.setName(content, german, "Neuer Blog Post");
		content.addTag(blogsFolder);
		content.setCreator(users.get(0));
		contentService.setFilename(content, german, "blog.html");
		contentService.setContent(content, german, "This is the blogpost content");
		contentService.setTeaser(content, german, "Jo this Content is the second blogpost");
		contentService.save(content);

		content = new Content();
		contentService.setName(content, german, "Hallo Cailun");
		contentService.setFilename(content, german, "some2.html");
		content.setCreator(users.get(0));
		contentService.setContent(content, german, "some more content");
		content.addTag(postsFolder);
		contentService.save(content);

		Content indexContent = new Content();
		contentService.setName(indexContent, german, "Index With Perm");

		indexContent.setCreator(users.get(0));
		contentService.setFilename(indexContent, german, "index.html");
		contentService.setContent(indexContent, german, "The index Content<br/><a href=\"${Content(10)}\">Link</a>");
		contentService.setTitle(indexContent, german, "Index Title");
		contentService.setTeaser(indexContent, german, "Yo guckste hier");
		indexContent.addTag(wwwFolder);

		contentService.createLink(indexContent, content);
		contentService.save(indexContent);

		// Permissions
		try (Transaction tx = cailunConfig.getGraphDatabaseService().beginTx()) {
			// Add admin permissions to all nodes
			int i = 0;
			for (GenericNode currentNode : genericNodeService.findAll()) {
				log.info("Adding BasicPermission to node {" + currentNode.getId() + "}");
				if (adminRole.getId() == currentNode.getId()) {
					log.info("Skipping role");
					continue;
				}
				roleService.addPermission(adminRole, currentNode, CREATE, READ, UPDATE, DELETE);
				genericNodeService.save(currentNode);
				i++;
			}
			tx.success();
		}

	}

	private void addPermissionTestHandler() {
		route("/permtest").method(GET).handler(rh -> {
			Session session = rh.session();
			GenericContent content = contentService.findOne(23L);
			boolean perm = getAuthService().hasPermission(session.getLoginID(), new CaiLunPermission(content, READ));
			rh.response().end("User perm for node {" + content.getId() + "} : " + (perm ? "jow" : "noe"));
		});

	}

}
