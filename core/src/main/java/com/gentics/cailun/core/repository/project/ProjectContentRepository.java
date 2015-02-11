package com.gentics.cailun.core.repository.project;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.ContentRepository;
import com.gentics.cailun.core.repository.UUIDGraphRepository;
import com.gentics.cailun.core.rest.model.Content;

/**
 * Wrapper for the core spring data content repository.
 * 
 * @author johannes2
 *
 */
public class ProjectContentRepository<T extends Content> extends AbstractProjectRepository<T> {

	@Autowired
	ContentRepository<T> contentRepository;

	@Override
	public UUIDGraphRepository<T> getRepository() {
		// TODO Auto-generated method stub
		return null;
	}

}
