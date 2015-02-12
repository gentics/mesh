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
import com.gentics.cailun.core.repository.GlobalCaiLunNodeRepository;
import com.gentics.cailun.core.repository.CaiLunRootRepository;
import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.repository.GlobalGroupRepository;
import com.gentics.cailun.core.repository.GlobalProjectRepository;
import com.gentics.cailun.core.repository.GlobalRoleRepository;
import com.gentics.cailun.core.repository.GlobalTagRepository;
import com.gentics.cailun.core.repository.GlobalUserRepository;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.auth.CaiLunPermission;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
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
	private GlobalTagRepository tagRepository;

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

		// Project
		Project aloha = new Project("aloha");

		// Users
		List<User> users = addUsers();
		aloha.getUsers().addAll(users);

		// Groups
		Group rootGroup = new Group("superusers");
		aloha.setRootGroup(rootGroup);

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
		Tag rootTag = new Tag("/");
		rootTag.tag("home").tag("jotschi");
		rootTag.tag("root");
		rootTag.tag("var").tag("www");
		Tag wwwTag = rootTag.tag("var").tag("www");
		wwwTag.tag("site");
		Tag postsTag = wwwTag.tag("posts");
		Tag blogsTag = wwwTag.tag("blogs");
		tagRepository.save(rootTag);

		aloha.setRootTag(rootTag);
		projectRepository.save(aloha);

		// Contents
		Page rootPage = new Page("rootPage");
		rootPage.setCreator(users.get(0));
		rootPage.setContent("This is root");
		rootPage.setFilename("index.html");
		rootPage.setTeaser("Yo root");
		rootPage.tag(rootTag);
		contentRepository.save(rootPage);
		rootPage = (Page) contentRepository.findOne(rootPage.getId());

		for (int i = 0; i < 6; i++) {
			Page page = new Page("Hallo Welt");
			page.setFilename("some" + i + ".html");
			page.setCreator(users.get(0));
			page.setContent("some content");
			page.tag(blogsTag);
			contentRepository.save(page);
		}

		for (int i = 0; i < 3; i++) {
			Page page = new Page("Hallo Welt");
			page.setFilename("some_posts" + i + ".html");
			page.setCreator(users.get(0));
			page.setContent("some content");
			page.tag(postsTag);
			contentRepository.save(page);
		}

		Page page = new Page("New BlogPost");
		page.tag(blogsTag);
		page.setCreator(users.get(0));
		page.setFilename("blog.html");
		page.setContent("This is the blogpost content");
		page.setTeaser("Jo this page is the second blogpost");
		contentRepository.save(page);

		page = new Page("Hallo Cailun");
		page.setFilename("some2.html");
		page.setCreator(users.get(0));
		page.setContent("some more content");
		page.tag(postsTag);
		contentRepository.save(page);

		Page indexPage = new Page("Index With Perm");
		indexPage.setCreator(users.get(0));
		indexPage.setFilename("index.html");
		indexPage.setContent("The index page<br/><a href=\"${Page(10)}\">Link</a>");
		indexPage.setTitle("Index Title");
		indexPage.setTeaser("Yo guckste hier");
		indexPage.tag(wwwTag);

		indexPage.linkTo(page);
		contentRepository.save(indexPage);

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
			Content content = contentRepository.findOne(23L);
			boolean perm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(content, READ));
			rh.response().end("User perm for node {" + content.getId() + "} : " + (perm ? "jow" : "noe"));
		});

	}

}
