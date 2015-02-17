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
import com.gentics.cailun.core.repository.GlobalCaiLunNodeRepository;
import com.gentics.cailun.core.repository.GlobalGroupRepository;
import com.gentics.cailun.core.repository.GlobalLanguageRepository;
import com.gentics.cailun.core.repository.GlobalProjectRepository;
import com.gentics.cailun.core.repository.GlobalRoleRepository;
import com.gentics.cailun.core.repository.GlobalUserRepository;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.CaiLunRoot;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.FolderTag;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.auth.CaiLunPermission;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.service.FolderTagService;
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
	private GlobalPageRepository pageRepository;

	@Autowired
	private GlobalCaiLunNodeRepository<CaiLunNode> nodeRepository;

	@Autowired
	private CaiLunRootRepository rootRepository;

	@Autowired
	private GlobalProjectRepository projectRepository;

	@Autowired
	private CaiLunRootRepository caiLunRootRepository;

	@Autowired
	private PageService pageService;

	@Autowired
	private FolderTagService folderTagService;

	public CustomerVerticle() {
		super("page");
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
		pageRepository.findCustomerNodeBySomeStrangeCriteria("dgasdg");

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
		FolderTag rootTag = new FolderTag();
		folderTagService.setName(rootTag, english, "/");

		FolderTag homeFolder = new FolderTag();
		folderTagService.setName(homeFolder, english, "home");
		folderTagService.setName(homeFolder, german, "heim");
		rootTag.addTag(homeFolder);

		FolderTag jotschiFolder = new FolderTag();
		folderTagService.setName(jotschiFolder, german, "jotschi");
		folderTagService.setName(jotschiFolder, english, "jotschi");
		homeFolder.addTag(jotschiFolder);

		FolderTag rootFolder = new FolderTag();
		folderTagService.setName(rootFolder, german, "wurzel");
		folderTagService.setName(rootFolder, english, "root");
		rootTag.addTag(rootFolder);

		FolderTag varFolder = new FolderTag();
		folderTagService.setName(varFolder, german, "var");
		rootTag.addTag(varFolder);

		FolderTag wwwFolder = new FolderTag();
		folderTagService.setName(wwwFolder, english, "www");
		varFolder.addTag(wwwFolder);

		FolderTag siteFolder = new FolderTag();
		folderTagService.setName(siteFolder, english, "site");
		wwwFolder.addTag(siteFolder);

		FolderTag postsFolder = new FolderTag();
		folderTagService.setName(postsFolder, german, "posts");
		wwwFolder.addTag(postsFolder);

		FolderTag blogsFolder = new FolderTag();
		folderTagService.setName(blogsFolder, german, "blogs");
		wwwFolder.addTag(blogsFolder);

		aloha.setRootTag(rootTag);
		projectRepository.save(aloha);

		// Contents
		Page rootPage = new Page();
		pageService.setName(rootPage, german, "german name");
		pageService.setFilename(rootPage, german, "german.html");
		pageService.setContent(rootPage, german, "Mahlzeit!");

		pageService.setName(rootPage, english, "english name");
		pageService.setFilename(rootPage, english, "english.html");
		pageService.setContent(rootPage, english, "Blessed mealtime!");

		rootPage.setCreator(users.get(0));
		// rootPage.tag(rootTag);
		pageRepository.save(rootPage);

		rootPage = (Page) pageRepository.findOne(rootPage.getId());

		for (int i = 0; i < 6; i++) {
			Page page = new Page();
			pageService.setName(page, german, "Hallo Welt");
			pageService.setFilename(page, german, "some" + i + ".html");
			page.setCreator(users.get(0));
			pageService.setContent(page, german, "some content");
			page.addTag(blogsFolder);
			pageRepository.save(page);
		}

		for (int i = 0; i < 3; i++) {
			Page page = new Page();
			pageService.setName(page, german, "Hallo Welt");
			pageService.setFilename(page, german, "some_posts" + i + ".html");
			page.setCreator(users.get(0));
			pageService.setContent(page, german, "some content");
			page.addTag(postsFolder);
			pageRepository.save(page);
		}

		Page page = new Page();
		pageService.setName(page, german, "Neuer Blog Post");
		page.addTag(blogsFolder);
		page.setCreator(users.get(0));
		pageService.setFilename(page, german, "blog.html");
		pageService.setContent(page, german, "This is the blogpost content");
		pageService.setTeaser(page, german, "Jo this page is the second blogpost");
		pageRepository.save(page);

		page = new Page();
		pageService.setName(page, german, "Hallo Cailun");
		pageService.setFilename(page, german, "some2.html");
		page.setCreator(users.get(0));
		pageService.setContent(page, german, "some more content");
		page.addTag(postsFolder);
		pageRepository.save(page);

		Page indexPage = new Page();
		pageService.setName(indexPage, german, "Index With Perm");

		indexPage.setCreator(users.get(0));
		pageService.setFilename(indexPage, german, "index.html");
		pageService.setContent(indexPage, german, "The index page<br/><a href=\"${Page(10)}\">Link</a>");
		pageService.setTitle(indexPage, german, "Index Title");
		pageService.setTeaser(indexPage, german, "Yo guckste hier");
		indexPage.addTag(wwwFolder);

		pageService.createLink(indexPage, page);
		pageRepository.save(indexPage);

		// Permissions
		try (Transaction tx = cailunConfig.getGraphDatabaseService().beginTx()) {
			// Add admin permissions to all nodes
			int i = 0;
			for (CaiLunNode currentNode : nodeRepository.findAll()) {
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
			Content content = (Content) pageRepository.findOne(23L);
			boolean perm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(content, READ));
			rh.response().end("User perm for node {" + content.getId() + "} : " + (perm ? "jow" : "noe"));
		});

	}

}
