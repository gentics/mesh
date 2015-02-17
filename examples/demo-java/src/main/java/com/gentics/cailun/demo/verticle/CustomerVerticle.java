package com.gentics.cailun.demo.verticle;

import static com.gentics.cailun.core.rest.model.auth.PermissionType.READ;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.core.Session;

import java.util.Arrays;
import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.repository.GlobalCaiLunNodeRepository;
import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.repository.GlobalGroupRepository;
import com.gentics.cailun.core.repository.GlobalLanguageRepository;
import com.gentics.cailun.core.repository.GlobalProjectRepository;
import com.gentics.cailun.core.repository.GlobalRoleRepository;
import com.gentics.cailun.core.repository.GlobalUserRepository;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.CaiLunRoot;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.auth.CaiLunPermission;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
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
	private GlobalCaiLunNodeRepository<Tag> tagRepository;

	@Autowired
	private GlobalGroupRepository groupRepository;

	@Autowired
	private GlobalRoleRepository roleRepository;

	@Autowired
	private CaiLunSpringConfiguration cailunConfig;

	@Autowired
	private GlobalContentRepository contentRepository;

	@Autowired
	private GlobalCaiLunNodeRepository<CaiLunNode> nodeRepository;

	@Autowired
	private CaiLunRootRepository rootRepository;

	@Autowired
	private GlobalProjectRepository projectRepository;

	@Autowired
	private CaiLunRootRepository caiLunRootRepository;

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

		contentRepository.findCustomerNodeBySomeStrangeCriteria("dgasdg");

		CaiLunRoot rootNode = caiLunRootRepository.findRoot();

		// Project
		Project aloha = new Project("aloha");

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

		// // Tags
		// Tag rootTag = new Tag();
		// rootTag.addLocalizedTag(german, "/");
		// rootTag.addLocalizedSubTag(german, "heim");
		// rootTag.addLocalizedSubTag(english, "home").addLocalizedSubTag(german, "jotschi");
		// rootTag.addLocalizedSubTag(english, "root");
		// rootTag.addLocalizedSubTag(german, "wurzel");
		// Tag wwwTag = rootTag.addLocalizedSubTag(english, "var").addLocalizedSubTag(english, "www");
		// wwwTag.addLocalizedSubTag(english, "site");
		// wwwTag.addLocalizedSubTag(english, "posts");
		// wwwTag.addLocalizedSubTag(english, "blogs");
		// //tagRepository.save(rootTag);
		//
		// aloha.setRootTag(rootTag);
		// projectRepository.save(aloha);
		//
		// // Contents
		// Page rootPage = new Page(german, "german name", "german.html");
		// LocalizedPage germanPage= rootPage.getLocalisation(german);
		// germanPage.setContent("Mahlzeit!");
		//
		// rootPage.addLocalizedContent(english, "english name", "english.html").setContent("Blessed mealtime!");
		// rootPage.setCreator(users.get(0));
		// // rootPage.tag(rootTag);
		// contentRepository.save((Content)rootPage);

		// rootPage = (LocalizedPage) contentRepository.findOne(rootPage.getId());
		//
		// for (int i = 0; i < 6; i++) {
		// LocalizedPage page = new LocalizedPage("Hallo Welt");
		// page.setFilename("some" + i + ".html");
		// page.setCreator(users.get(0));
		// page.setContent("some content");
		// page.tag(blogsTag);
		// contentRepository.save(page);
		// }
		//
		// for (int i = 0; i < 3; i++) {
		// LocalizedPage page = new LocalizedPage("Hallo Welt");
		// page.setFilename("some_posts" + i + ".html");
		// page.setCreator(users.get(0));
		// page.setContent("some content");
		// page.tag(postsTag);
		// contentRepository.save(page);
		// }
		//
		// LocalizedPage page = new LocalizedPage("New BlogPost");
		// page.tag(blogsTag);
		// page.setCreator(users.get(0));
		// page.setFilename("blog.html");
		// page.setContent("This is the blogpost content");
		// page.setTeaser("Jo this page is the second blogpost");
		// contentRepository.save(page);
		//
		// page = new LocalizedPage("Hallo Cailun");
		// page.setFilename("some2.html");
		// page.setCreator(users.get(0));
		// page.setContent("some more content");
		// page.tag(postsTag);
		// contentRepository.save(page);
		//
		// LocalizedPage indexPage = new LocalizedPage("Index With Perm");
		// indexPage.setCreator(users.get(0));
		// indexPage.setFilename("index.html");
		// indexPage.setContent("The index page<br/><a href=\"${Page(10)}\">Link</a>");
		// indexPage.setTitle("Index Title");
		// indexPage.setTeaser("Yo guckste hier");
		// indexPage.tag(wwwTag);
		//
		// indexPage.linkTo(page);
		// contentRepository.save(indexPage);
		//
		// // Permissions
		// try (Transaction tx = cailunConfig.getGraphDatabaseService().beginTx()) {
		// // Add admin permissions to all nodes
		// int i = 0;
		// for (CaiLunNode currentNode : nodeRepository.findAll()) {
		// // if (i % 2 == 0) {
		// log.info("Adding BasicPermission to node {" + currentNode.getId() + "}");
		// GraphPermission permission = new GraphPermission(adminRole, currentNode);
		// permission.grant(CREATE);
		// permission.grant(READ);
		// permission.grant(WRITE);
		// permission.grant(DELETE);
		// currentNode.addPermission(permission);
		// nodeRepository.save(currentNode);
		// i++;
		// }
		// tx.success();
		// }

	}

	private void addPermissionTestHandler() {
		route("/permtest").method(GET).handler(rh -> {
			Session session = rh.session();
			Content content = contentRepository.findOne(23L);
			boolean perm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(content, READ));
			rh.response().end("User perm for node {" + content.getId() + "} : " + (perm ? "jow" : "noe"));
		});

	}

}
