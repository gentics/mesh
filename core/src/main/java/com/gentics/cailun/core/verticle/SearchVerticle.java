package com.gentics.cailun.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.repository.generic.GlobalGenericContentRepository;
import com.gentics.cailun.core.repository.generic.GlobalGenericNodeRepository;
import com.gentics.cailun.core.rest.model.generic.GenericContent;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCaiLunProjectRestVerticle {

	@Autowired
	@Qualifier("globalGenericContentRepository")
	private GlobalGenericContentRepository<GenericContent> contentRepository;

	@Autowired
	private GlobalGenericNodeRepository<GenericTag> tagRepository;


	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
	}

}
