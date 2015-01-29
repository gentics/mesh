package com.gentics.vertx.cailun.nav.model;

import java.util.concurrent.RecursiveTask;

import com.gentics.vertx.cailun.page.PageRepository;
import com.gentics.vertx.cailun.page.model.Page;
import com.gentics.vertx.cailun.tag.model.Tag;

/**
 * A navigation task is a recursivetask that is used to buildup a navigation object. This task is used within the {@link NavigationRequestHandler} to build the
 * navigation using the forkjoin concurrency utility class.
 * 
 * @author johannes2
 *
 */
public class NavigationTask extends RecursiveTask<Navigation> {

	private static final long serialVersionUID = 8773519857036585642L;
	private Tag tag;
	private NavigationElement element;
	private NavigationRequestHandler handler;
	private PageRepository pageRepository;

	public NavigationTask(Tag tag, NavigationElement element, NavigationRequestHandler handler, PageRepository pageRepository) {
		this.tag = tag;
		this.element = element;
		this.handler = handler;
		this.pageRepository = pageRepository;
	}

	@Override
	protected Navigation compute() {

		tag.getContents().parallelStream().forEachOrdered(tagging -> {
			if (tagging.getClass().isAssignableFrom(Page.class)) {
				Page page = (Page) tagging;
				if (handler.canView(tag)) {
					NavigationElement pageNavElement = new NavigationElement();
					pageNavElement.setName(page.getFilename());
					pageNavElement.setType(NavigationElementType.PAGE);
					pageNavElement.setPath(pageRepository.getPath(page.getId()));
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
				NavigationTask subTask = new NavigationTask(currentTag, navElement, handler, pageRepository);
				subTask.fork();
			}
		});
		return null;
	}
}
