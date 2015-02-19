package com.gentics.cailun.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.repository.generic.GenericContentRepository;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;
import com.gentics.cailun.core.rest.model.generic.GenericContent;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractProjectRestVerticle {

	@Autowired
	@Qualifier("genericContentRepository")
	private GenericContentRepository<GenericContent> contentRepository;

	@Autowired
	private GenericNodeRepository<GenericTag> tagRepository;


	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
	}

}
