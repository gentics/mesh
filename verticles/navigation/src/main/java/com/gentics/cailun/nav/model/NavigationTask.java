package com.gentics.cailun.nav.model;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.core.repository.PageRepository;
import com.gentics.cailun.core.rest.model.Page;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.util.Neo4jPageUtils;

/**
 * A navigation task is a recursivetask that is used to buildup a navigation object. This task is used within the {@link NavigationRequestHandler} to build the
 * navigation using the forkjoin concurrency utility class.
 * 
 * @author johannes2
 *
 */
public class NavigationTask extends RecursiveTask<Void> {

	private static final Logger log = LoggerFactory.getLogger(NavigationTask.class);

	private static final long serialVersionUID = 8773519857036585642L;
	private Tag tag;
	private NavigationElement element;
	private NavigationRequestHandler handler;
	private PageRepository pageRepository;
	private Neo4jPageUtils pageUtils;

	public NavigationTask(Tag tag, NavigationElement element, NavigationRequestHandler handler, PageRepository pageRepository, Neo4jPageUtils pageUtils) {
		this.tag = tag;
		this.element = element;
		this.handler = handler;
		this.pageRepository = pageRepository;
		this.pageUtils = pageUtils;
	}

	@Override
	protected Void compute() {

		Set<ForkJoinTask<Void>> tasks = new HashSet<>();
		tag.getContents().parallelStream().forEachOrdered(tagging -> {
			if (tagging.getClass().isAssignableFrom(Page.class)) {
				Page page = (Page) tagging;
				if (handler.canView(tag)) {
					NavigationElement pageNavElement = new NavigationElement();
					pageNavElement.setName(page.getFilename());
					pageNavElement.setType(NavigationElementType.PAGE);
					String path = pageUtils.getPath(tag, page);

//					String path = pageRepository.getPath(page.getId());
					log.debug("Loaded path { " + path + "} for page {" + page.getId() + "}");
					pageNavElement.setPath(path);
					element.getChildren().add(pageNavElement);
				}
			}
		});

		tag.getChildTags().parallelStream().forEachOrdered(currentTag -> {
			if (handler.canView(currentTag)) {
				NavigationElement navElement = new NavigationElement();
				navElement.setType(NavigationElementType.TAG);
				navElement.setName(currentTag.getName());
				element.getChildren().add(navElement);
				NavigationTask subTask = new NavigationTask(currentTag, navElement, handler, pageRepository, pageUtils);
				tasks.add(subTask.fork());
			}
		});

		// Wait for all forked tasks to finish
		tasks.forEach(action -> action.join());
		return null;
	}
}
