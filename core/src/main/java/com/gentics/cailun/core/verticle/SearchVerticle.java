package com.gentics.cailun.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCaiLunProjectRestVerticle;
import com.gentics.cailun.core.repository.GlobalCaiLunNodeRepository;
import com.gentics.cailun.core.repository.GlobalContentRepository;
import com.gentics.cailun.core.rest.model.Tag;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCaiLunProjectRestVerticle {

	@Autowired
	private GlobalContentRepository contentRepository;

	@Autowired
	private GlobalCaiLunNodeRepository<Tag> tagRepository;


	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
	}

}
