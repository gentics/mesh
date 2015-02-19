package com.gentics.cailun.nav.model;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.core.repository.generic.GenericContentRepository;
import com.gentics.cailun.core.rest.model.generic.GenericTag;
import com.gentics.cailun.util.Neo4jGenericContentUtils;

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
	private GenericTag tag;
	private NavigationElement element;
	private NavigationRequestHandler handler;
	private GenericContentRepository genericContentRepository;
	private Neo4jGenericContentUtils genericContentUtils;

	public NavigationTask(GenericTag tag, NavigationElement element, NavigationRequestHandler handler, GenericContentRepository genericContentRepository,
			Neo4jGenericContentUtils genericContentUtils) {
		this.tag = tag;
		this.element = element;
		this.handler = handler;
		this.genericContentRepository = genericContentRepository;
		this.genericContentUtils = genericContentUtils;
	}

	@Override
	protected Void compute() {

		Set<ForkJoinTask<Void>> tasks = new HashSet<>();
		tag.getFiles().parallelStream().forEachOrdered(tagging -> {
		});

//		tag.getContents().parallelStream().forEachOrdered(content -> {
//			for (LocalizedContent localizedContent : content.getLocalisations()) {
//				if (handler.canView(tag)) {
//					NavigationElement pageNavElement = new NavigationElement();
//					pageNavElement.setName(localizedContent.getFilename());
//					pageNavElement.setType(NavigationElementType.CONTENT);
//					// String path = genericContentUtils.getPath(tag, content);
//				String path = "unknown";
//				if (log.isDebugEnabled()) {
//					log.debug("Loaded path { " + path + "} for page {" + content.getId() + "}");
//				}
//				pageNavElement.setPath(path);
//				element.getChildren().add(pageNavElement);
//			}
//		}
//		});

//		tag.getChildTags().parallelStream().forEachOrdered(currentTag -> {
//			if (handler.canView(currentTag)) {
//				NavigationElement navElement = new NavigationElement();
//				navElement.setType(NavigationElementType.TAG);
//				// navElement.setName(currentTag.getName());
//				element.getChildren().add(navElement);
//				NavigationTask subTask = new NavigationTask(currentTag, navElement, handler, genericContentRepository, genericContentUtils);
//				tasks.add(subTask.fork());
//			}
//		});

		// Wait for all forked tasks to finish
		tasks.forEach(action -> action.join());
		return null;
	}
}
