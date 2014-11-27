package com.gentics.vertx.cailun.starter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.vertx.cailun.repository.Page;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.Tag;
import com.gentics.vertx.cailun.repository.TagRepository;

public class MainBean {

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;
	private static Logger log = LoggerFactory.getLogger(MainBean.class);

	public void start() {
		log.info("Starting main bean.");

		Tag rootTag = new Tag("/");
		rootTag.tag("home").tag("jotschi");
		rootTag.tag("root");
		Tag wwwTag = rootTag.tag("var").tag("www");
		Tag siteTag = wwwTag.tag("site");
		Tag postsTag = wwwTag.tag("posts");
		Tag blogsTag = wwwTag.tag("blogs");
		tagRepository.save(rootTag);

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
		indexPage.setContent("The index page<br/><a href=\"somewhere.html\">Link</a>");
		indexPage.setTitle("Index Title");
		indexPage.setAuthor("Jotschi");
		indexPage.setTeaser("Yo guckste hier");
		indexPage.tag(wwwTag);

		indexPage.linkTo(page);
		pageRepository.save(indexPage);

		System.out.println("COUNT:  " + pageRepository.count());
	}
}
