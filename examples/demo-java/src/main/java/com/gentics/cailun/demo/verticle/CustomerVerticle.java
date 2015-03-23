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
		john.setPasswordHash(springConfiguration.passwordEncoder().encode("test123"));

		User mary = new User("mary2");
		mary.setFirstname("Mary");
		mary.setLastname("Doe");
		mary.setEmailAddress("m.doe@gentics.com");
		mary.setPasswordHash(springConfiguration.passwordEncoder().encode("lalala"));
		List<User> users = Arrays.asList(john, mary);
		userService.save(users);
		return users;

	}

	@Override
	public void registerEndPoints() throws Exception {

		addPermissionTestHandler();

		Role adminRole;
		try (Transaction tx = graphDb.beginTx()) {
			adminRole = setupDemoData();
			tx.success();
		}

		// Update role to load uuid
		adminRole = roleService.reload(adminRole);

		// Add Permissions
		try (Transaction tx = graphDb.beginTx()) {
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
			log.info("Added permissions to {" + i + "} objects.");
			tx.success();
		}

	}

	private Role setupDemoData() {
		CaiLunRoot rootNode = rootService.findRoot();

		Project aloha = new Project("aloha");
		aloha = projectService.save(aloha);

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
		adminRole = roleService.save(adminRole);
		adminRole = roleService.reload(adminRole);
		Role guestRole = new Role("guest role");
		guestRole = roleService.save(guestRole);
		guestRole = roleService.reload(guestRole);

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
		rootTag.addProject(aloha);

		tagService.setName(rootTag, english, "/");

		Tag homeTag = new Tag();
		tagService.setName(homeTag, english, "home");
		tagService.setName(homeTag, german, "heim");
		homeTag.addProject(aloha);
		rootTag.addTag(homeTag);

		Tag jotschiTag = new Tag();
		tagService.setName(jotschiTag, german, "jotschi");
		tagService.setName(jotschiTag, english, "jotschi");
		jotschiTag.addProject(aloha);
		homeTag.addTag(jotschiTag);

		Tag rootFolder = new Tag();
		tagService.setName(rootFolder, german, "wurzel");
		tagService.setName(rootFolder, english, "root");
		rootFolder.addProject(aloha);
		rootTag.addTag(rootFolder);

		Tag varFolder = new Tag();
		tagService.setName(varFolder, german, "var");
		varFolder.addProject(aloha);
		rootTag.addTag(varFolder);

		Tag wwwFolder = new Tag();
		tagService.setName(wwwFolder, english, "www");
		wwwFolder.addProject(aloha);
		varFolder.addTag(wwwFolder);

		Tag siteFolder = new Tag();
		tagService.setName(siteFolder, english, "site");
		siteFolder.addProject(aloha);
		wwwFolder.addTag(siteFolder);

		Tag postsFolder = new Tag();
		tagService.setName(postsFolder, german, "posts");
		postsFolder.addProject(aloha);
		wwwFolder.addTag(postsFolder);

		Tag blogsFolder = new Tag();
		tagService.setName(blogsFolder, german, "blogs");
		blogsFolder.addProject(aloha);
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
		contentSchema = objectSchemaService.save(contentSchema);

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
		rootContent = contentService.save(rootContent);
		rootContent = contentService.reload(rootContent);

		for (int i = 0; i < 6; i++) {
			Content content = new Content();
			contentService.setName(content, german, "Hallo Welt");
			contentService.setFilename(content, german, "some" + i + ".html");
			content.setCreator(users.get(0));
			contentService.setContent(content, german, "some content");
			content.addTag(blogsFolder);
			content.addProject(aloha);
			content = contentService.save(content);
		}

		for (int i = 0; i < 3; i++) {
			Content content = new Content();
			contentService.setName(content, german, "Hallo Welt");
			contentService.setFilename(content, german, "some_posts" + i + ".html");
			content.setCreator(users.get(0));
			contentService.setContent(content, german, "some content");
			content.addTag(postsFolder);
			content.addProject(aloha);
			content = contentService.save(content);
		}

		Content content = new Content();
		contentService.setName(content, german, "Neuer Blog Post");
		content.addTag(blogsFolder);
		content.setCreator(users.get(0));
		contentService.setFilename(content, german, "blog.html");
		contentService.setContent(content, german, "This is the blogpost content");
		contentService.setTeaser(content, german, "Jo this Content is the second blogpost");
		content.addProject(aloha);
		content = contentService.save(content);

		content = new Content();
		contentService.setName(content, german, "Hallo Cailun");
		contentService.setFilename(content, german, "some2.html");
		content.setCreator(users.get(0));
		contentService.setContent(content, german, "some more content");
		content.addTag(postsFolder);
		content.addProject(aloha);
		content = contentService.save(content);

		Content indexContent = new Content();
		contentService.setName(indexContent, german, "Index With Perm");

		indexContent.setCreator(users.get(0));
		contentService.setFilename(indexContent, german, "index.html");
		contentService.setContent(indexContent, german, "The index Content<br/><a href=\"${Content(10)}\">Link</a>");
		contentService.setTitle(indexContent, german, "Index Title");
		contentService.setTeaser(indexContent, german, "Yo guckste hier");
		indexContent.addProject(aloha);
		indexContent.addTag(wwwFolder);

		contentService.createLink(indexContent, content);
		indexContent = contentService.save(indexContent);
		return adminRole;

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
