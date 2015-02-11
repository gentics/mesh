package com.gentics.cailun.core.repository.project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.rest.model.Content;

/**
 * Wrapper for the core spring data content repository.
 * 
 * @author johannes2
 *
 */
@Component
public class ProjectContentRepository extends AbstractProjectRepository<Content> {

	@Autowired
	ContentRepository contentRepository;

	@Override
	public ContentRepository getRepository() {
		return contentRepository;
	}

}
