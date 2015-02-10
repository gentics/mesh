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

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.repository.CaiLunNodeRepository;
import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.auth.CaiLunPermission;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.Role;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.Neo4jSpringConfiguration;

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
	private UserRepository userRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private CaiLunSpringConfiguration cailunConfig;

	@Autowired
	private ContentRepository contentRepository;

	@Autowired
	private CaiLunNodeRepository nodeRepository;

	@Autowired
	private Neo4jSpringConfiguration neo4jSpringConfiguration;

	public CustomerVerticle() {
		super("page");
	}

	@Override
	public void registerEndPoints() throws Exception {

		addPermissionTestHandler();

		// Users
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
		userRepository.save(Arrays.asList(john, mary));

		// Roles
		Role adminRole = new Role("admin role");
		roleRepository.save(adminRole);
		Role guestRole = new Role("guest role");
		roleRepository.save(guestRole);

		// Groups
		Group rootGroup = new Group("superusers");
		rootGroup.getMembers().add(john);
		rootGroup.getRoles().add(adminRole);

		groupRepository.save(rootGroup);
		Group guests = new Group("guests");
		guests.getParents().add(rootGroup);
		guests.getMembers().add(mary);
		guests.getRoles().add(guestRole);
		groupRepository.save(guests);

		// Content
		Tag rootTag = new Tag("/");
		rootTag.tag("home").tag("jotschi");
		rootTag.tag("root");
		rootTag.tag("var").tag("www");
		Tag wwwTag = rootTag.tag("var").tag("www");
		wwwTag.tag("site");
		Tag postsTag = wwwTag.tag("posts");
		Tag blogsTag = wwwTag.tag("blogs");
		tagRepository.save(rootTag);

		Page rootPage = new Page("rootPage");
		rootPage.setContent("This is root");
		rootPage.setFilename("index.html");
		rootPage.setTeaser("Yo root");
		rootPage.tag(rootTag);
		contentRepository.save(rootPage);

		for (int i = 0; i < 6; i++) {
			Page page = new Page("Hallo Welt");
			page.setFilename("some" + i + ".html");
			page.setContent("some content");
			page.tag(blogsTag);
			contentRepository.save(page);
		}

		for (int i = 0; i < 3; i++) {
			Page page = new Page("Hallo Welt");
			page.setFilename("some_posts" + i + ".html");
			page.setContent("some content");
			page.tag(postsTag);
			contentRepository.save(page);
		}

		Page page = new Page("New BlogPost");
		page.tag(blogsTag);
		page.setFilename("blog.html");
		page.setContent("This is the blogpost content");
		page.setTeaser("Jo this page is the second blogpost");
		contentRepository.save(page);

		page = new Page("Hallo Cailun");
		page.setFilename("some2.html");
		page.setContent("some more content");
		page.tag(postsTag);
		contentRepository.save(page);

		Page indexPage = new Page("Index With Perm");
		indexPage.setFilename("index.html");
		indexPage.setContent("The index page<br/><a href=\"${Page(10)}\">Link</a>");
		indexPage.setTitle("Index Title");
		indexPage.setTeaser("Yo guckste hier");
		indexPage.tag(wwwTag);

		indexPage.linkTo(page);
		contentRepository.save(indexPage);

		try (Transaction tx = neo4jSpringConfiguration.getGraphDatabaseService().beginTx()) {
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
