package com.gentics.vertx.cailun.demo.resource;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.repository.Page;
import com.gentics.vertx.cailun.repository.PageRepository;
import com.gentics.vertx.cailun.repository.Tag;
import com.gentics.vertx.cailun.repository.TagRepository;
import com.gentics.vertx.cailun.rest.resources.AbstractCaiLunResource;

/**
 * Simple page resource to load and render pages
 */
@Component
@Scope("singleton")
@Path("/page")
public class CustomPageResource extends AbstractCaiLunResource {

	private static Logger log = LoggerFactory.getLogger(CustomPageResource.class);

	@Autowired
	private PageRepository pageRepository;

	@Autowired
	private TagRepository tagRepository;

	@GET
	@Path("id")
	@Produces(MediaType.APPLICATION_JSON)
	public String getId(@Context Vertx vertx) throws Exception {
		return "CustomPageResource";
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

		System.out.println("COUNT:  " + pageRepository.count());
	}

}
