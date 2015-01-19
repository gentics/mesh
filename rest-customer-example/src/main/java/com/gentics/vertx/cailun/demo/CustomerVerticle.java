package com.gentics.vertx.cailun.demo;

import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import java.util.Arrays;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.page.PageRepository;
import com.gentics.vertx.cailun.page.model.Page;
import com.gentics.vertx.cailun.perm.GroupRepository;
import com.gentics.vertx.cailun.perm.RoleRepository;
import com.gentics.vertx.cailun.perm.UserRepository;
import com.gentics.vertx.cailun.perm.model.Group;
import com.gentics.vertx.cailun.perm.model.Permission;
import com.gentics.vertx.cailun.perm.model.Role;
import com.gentics.vertx.cailun.perm.model.User;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;
import com.gentics.vertx.cailun.shiro.spring.SecurityConfiguration;
import com.gentics.vertx.cailun.tag.TagRepository;
import com.gentics.vertx.cailun.tag.model.Tag;

@Component
@Scope("singleton")
@SpringVerticle
public class CustomerVerticle extends AbstractCailunRestVerticle {

	private static Logger log = LoggerFactory.getLogger(CustomerVerticle.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private SecurityConfiguration securityConfig;

	public CustomerVerticle() {
		super("page");
	}

	@Override
	public void start() throws Exception {
		super.start();
		route("/custom").method(GET).handler(rc -> {
			System.out.println("This is custom");
			rc.response().end("END");
		});
		log.info("Setup of test data");

		// Users
		User john = new User("joe1");
		john.setFirstname("John");
		john.setLastname("Doe");
		john.setEmailAddress("j.doe@gentics.com");
		john.setPasswordHash(securityConfig.passwordEncoder().encode("enemenemuh"));

		User mary = new User("mary2");
		mary.setFirstname("Mary");
		mary.setLastname("Doe");
		mary.setEmailAddress("m.doe@gentics.com");
		mary.setPasswordHash(securityConfig.passwordEncoder().encode("lalala"));
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
		pageRepository.save(rootPage);

		for (int i = 0; i < 6; i++) {
			Page page = new Page("Hallo Welt");
			page.setFilename("some" + i + ".html");
			page.setContent("some content");
			page.tag(blogsTag);
			pageRepository.save(page);

		}

		for (int i = 0; i < 3; i++) {
			Page page = new Page("Hallo Welt");
			page.setFilename("some_posts" + i + ".html");
			page.setContent("some content");
			page.tag(postsTag);
			pageRepository.save(page);

		}
		Page page = new Page("New BlogPost");
		page.tag(blogsTag);
		page.setFilename("blog.html");
		page.setContent("This is the blogpost content");
		page.setAuthor("Jotschi");
		page.setTeaser("Jo this page is the second blogpost");
		pageRepository.save(page);

		page = new Page("Hallo Cailun");
		page.setFilename("some2.html");
		page.setContent("some more content");
		page.tag(postsTag);
		pageRepository.save(page);

		Page indexPage = new Page("Index");
		indexPage.setFilename("index.html");
		indexPage.setContent("The index page<br/><a href=\"${Page(10)}\">Link</a>");
		indexPage.setTitle("Index Title");
		indexPage.setAuthor("Jotschi");
		indexPage.setTeaser("Yo guckste hier");
		indexPage.tag(wwwTag);

		indexPage.linkTo(page);
		pageRepository.save(indexPage);

		// Permissions
		Permission perm = indexPage.addPermission(adminRole);
		perm.setCanCreate(true);
		perm.setCanRead(true);
		pageRepository.save(indexPage);

		log.info("COUNT: " + pageRepository.count());

	}

}
