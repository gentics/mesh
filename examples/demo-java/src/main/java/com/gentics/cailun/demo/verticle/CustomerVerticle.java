package com.gentics.cailun.demo.verticle;

import static com.gentics.cailun.core.rest.model.auth.PermissionType.CREATE;
import static com.gentics.cailun.core.rest.model.auth.PermissionType.DELETE;
import static com.gentics.cailun.core.rest.model.auth.PermissionType.READ;
import static com.gentics.cailun.core.rest.model.auth.PermissionType.WRITE;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.core.Session;

import java.util.Arrays;
import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.repository.GlobalGroupRepository;
import com.gentics.cailun.core.repository.GlobalLanguageRepository;
import com.gentics.cailun.core.repository.GlobalProjectRepository;
import com.gentics.cailun.core.repository.GlobalRoleRepository;
import com.gentics.cailun.core.repository.GlobalUserRepository;
import com.gentics.cailun.core.repository.generic.GlobalGenericNodeRepository;
import com.gentics.cailun.core.rest.model.CaiLunRoot;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.auth.CaiLunPermission;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.model.generic.GenericContent;
import com.gentics.cailun.core.rest.model.generic.GenericNode;
import com.gentics.cailun.core.rest.service.ContentService;
import com.gentics.cailun.core.rest.service.TagService;
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
public class CustomerVerticle extends AbstractCaiLunProjectRestVerticle {

	private static Logger log = LoggerFactory.getLogger(CustomerVerticle.class);

	@Autowired
	private GlobalUserRepository userRepository;

	@Autowired
	private GlobalLanguageRepository languageRepository;

	@Autowired
	private GlobalGroupRepository groupRepository;

	@Autowired
	private GlobalRoleRepository roleRepository;

	@Autowired
	private CaiLunSpringConfiguration cailunConfig;

	@Autowired
	private GlobalContentRepository contentRepository;

	@Autowired
	private GlobalGenericNodeRepository<GenericNode> nodeRepository;

	@Autowired
	private CaiLunRootRepository rootRepository;

	@Autowired
	private GlobalProjectRepository projectRepository;

	@Autowired
	private CaiLunRootRepository caiLunRootRepository;

	@Autowired
	private ContentService contentService;

	@Autowired
	private TagService folderTagService;

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
		john.setPasswordHash(cailunConfig.passwordEncoder().encode("test123"));

		User mary = new User("mary2");
		mary.setFirstname("Mary");
		mary.setLastname("Doe");
		mary.setEmailAddress("m.doe@gentics.com");
		mary.setPasswordHash(cailunConfig.passwordEncoder().encode("lalala"));
		List<User> users = Arrays.asList(john, mary);
		userRepository.save(users);
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
		contentRepository.findCustomerNodeBySomeStrangeCriteria("dgasdg");

		CaiLunRoot rootNode = caiLunRootRepository.findRoot();

		// Project
		Project aloha = new Project("aloha");
		aloha = projectRepository.save(aloha);

		Language german = new Language("german");
		languageRepository.save(german);

		Language english = new Language("english");
		languageRepository.save(english);

		// Users
		List<User> users = addUsers();
		rootNode.getUsers().addAll(users);

		// Groups
		Group rootGroup = new Group("superusers");
		rootNode.getRootGroup().getChildren().add(rootGroup);

		// Roles
		Role adminRole = new Role("admin role");
		roleRepository.save(adminRole);
		Role guestRole = new Role("guest role");
		roleRepository.save(guestRole);

		// Groups
		rootGroup.getMembers().add(users.get(0));
		rootGroup.getRoles().add(adminRole);

		groupRepository.save(rootGroup);
		Group guests = new Group("guests");
		guests.getParents().add(rootGroup);
		guests.getMembers().add(users.get(1));
		guests.getRoles().add(guestRole);
		groupRepository.save(guests);

		// Tags
		Tag rootTag = new Tag();
		folderTagService.setName(rootTag, english, "/");

		Tag homeFolder = new Tag();
		folderTagService.setName(homeFolder, english, "home");
		folderTagService.setName(homeFolder, german, "heim");
		rootTag.addTag(homeFolder);

		Tag jotschiFolder = new Tag();
		folderTagService.setName(jotschiFolder, german, "jotschi");
		folderTagService.setName(jotschiFolder, english, "jotschi");
		homeFolder.addTag(jotschiFolder);

		Tag rootFolder = new Tag();
		folderTagService.setName(rootFolder, german, "wurzel");
		folderTagService.setName(rootFolder, english, "root");
		rootTag.addTag(rootFolder);

		Tag varFolder = new Tag();
		folderTagService.setName(varFolder, german, "var");
		rootTag.addTag(varFolder);

		Tag wwwFolder = new Tag();
		folderTagService.setName(wwwFolder, english, "www");
		varFolder.addTag(wwwFolder);

		Tag siteFolder = new Tag();
		folderTagService.setName(siteFolder, english, "site");
		wwwFolder.addTag(siteFolder);

		Tag postsFolder = new Tag();
		folderTagService.setName(postsFolder, german, "posts");
		wwwFolder.addTag(postsFolder);

		Tag blogsFolder = new Tag();
		folderTagService.setName(blogsFolder, german, "blogs");
		wwwFolder.addTag(blogsFolder);

		aloha.setRootTag(rootTag);
		projectRepository.save(aloha);

		// Contents
		Content rootContent = new Content();
		contentService.setName(rootContent, german, "german name");
		contentService.setFilename(rootContent, german, "german.html");
		contentService.setContent(rootContent, german, "Mahlzeit!");

		contentService.setName(rootContent, english, "english name");
		contentService.setFilename(rootContent, english, "english.html");
		contentService.setContent(rootContent, english, "Blessed mealtime!");

		rootContent.setCreator(users.get(0));
		// rootContent.tag(rootTag);
		contentRepository.save(rootContent);

		rootContent = (Content) contentRepository.findOne(rootContent.getId());

		for (int i = 0; i < 6; i++) {
			Content Content = new Content();
			contentService.setName(Content, german, "Hallo Welt");
			contentService.setFilename(Content, german, "some" + i + ".html");
			Content.setCreator(users.get(0));
			contentService.setContent(Content, german, "some content");
			Content.addTag(blogsFolder);
			contentRepository.save(Content);
		}

		for (int i = 0; i < 3; i++) {
			Content Content = new Content();
			contentService.setName(Content, german, "Hallo Welt");
			contentService.setFilename(Content, german, "some_posts" + i + ".html");
			Content.setCreator(users.get(0));
			contentService.setContent(Content, german, "some content");
			Content.addTag(postsFolder);
			contentRepository.save(Content);
		}

		Content Content = new Content();
		contentService.setName(Content, german, "Neuer Blog Post");
		Content.addTag(blogsFolder);
		Content.setCreator(users.get(0));
		contentService.setFilename(Content, german, "blog.html");
		contentService.setContent(Content, german, "This is the blogpost content");
		contentService.setTeaser(Content, german, "Jo this Content is the second blogpost");
		contentRepository.save(Content);

		Content = new Content();
		contentService.setName(Content, german, "Hallo Cailun");
		contentService.setFilename(Content, german, "some2.html");
		Content.setCreator(users.get(0));
		contentService.setContent(Content, german, "some more content");
		Content.addTag(postsFolder);
		contentRepository.save(Content);

		Content indexContent = new Content();
		contentService.setName(indexContent, german, "Index With Perm");

		indexContent.setCreator(users.get(0));
		contentService.setFilename(indexContent, german, "index.html");
		contentService.setContent(indexContent, german, "The index Content<br/><a href=\"${Content(10)}\">Link</a>");
		contentService.setTitle(indexContent, german, "Index Title");
		contentService.setTeaser(indexContent, german, "Yo guckste hier");
		indexContent.addTag(wwwFolder);

		contentService.createLink(indexContent, Content);
		contentService.save(indexContent);

		// Permissions
		try (Transaction tx = cailunConfig.getGraphDatabaseService().beginTx()) {
			// Add admin permissions to all nodes
			int i = 0;
			for (GenericNode currentNode : nodeRepository.findAll()) {
				// if (i % 2 == 0) {
				log.info("Adding BasicPermission to node {" + currentNode.getId() + "}");
				GraphPermission permission = new GraphPermission(adminRole, currentNode);
				permission.grant(CREATE);
				permission.grant(READ);
				permission.grant(WRITE);
				permission.grant(DELETE);
				currentNode.addPermission(permission);
				nodeRepository.save(currentNode);
				i++;
			}
			tx.success();
		}

	}

	private void addPermissionTestHandler() {
		route("/permtest").method(GET).handler(rh -> {
			Session session = rh.session();
			GenericContent content = contentService.findOne(23L);
			boolean perm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(content, READ));
			rh.response().end("User perm for node {" + content.getId() + "} : " + (perm ? "jow" : "noe"));
		});

	}

}
