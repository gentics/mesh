package com.gentics.vertx.cailun.demo;

import static java.util.Arrays.asList;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.gentics.vertx.cailun.model.Page;
import com.gentics.vertx.cailun.model.Tag;
import com.gentics.vertx.cailun.model.perm.Group;
import com.gentics.vertx.cailun.model.perm.User;
import com.gentics.vertx.cailun.repository.GroupRepository;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.TagRepository;
import com.gentics.vertx.cailun.repository.UserRepository;

@Configuration
public class CustomerConfiguration {

	private static Logger log = LoggerFactory.getLogger(CustomerConfiguration.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private GroupRepository groupRepository;

	@PostConstruct
	public void setupACL() {
		User john = new User("Joe", "Doe", "j.doe@gentics.com");
		User mary = new User("Mary", "Doe", "m.doe@gentics.com");
		userRepository.save(asList(john, mary));

		Group rootGroup = new Group("superusers");
		rootGroup.getMembers().add(john);
		groupRepository.save(rootGroup);

		Group groupWithNoViewPerm = new Group("nopermusers");
		groupWithNoViewPerm.getParents().add(rootGroup);
		groupWithNoViewPerm.getMembers().add(mary);
		groupRepository.save(groupWithNoViewPerm);

	}

	@PostConstruct
	public void setupDB() {
		log.info("Starting main bean.");

		Tag rootTag = new Tag("/");
		rootTag.tag("home").tag("jotschi");
		rootTag.tag("root");
		Tag wwwTag = rootTag.tag("var").tag("www");
		Tag siteTag = wwwTag.tag("site");
		Tag postsTag = wwwTag.tag("posts");
		Tag blogsTag = wwwTag.tag("blogs");
		tagRepository.save(rootTag);

		Page rootPage = new Page("rootPage");
		rootPage.setContent("This is root");
		rootPage.setFilename("index.html");
		rootPage.setTeaser("Yo root");
		rootPage.tag(rootTag);
		pageRepository.save(rootPage);

		Page page = new Page("Hallo Welt");
		page.setFilename("some.html");
		page.setContent("some content");
		page.tag(blogsTag);
		page.tag(siteTag);
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

		System.out.println("COUNT: " + pageRepository.count());
	}

}
