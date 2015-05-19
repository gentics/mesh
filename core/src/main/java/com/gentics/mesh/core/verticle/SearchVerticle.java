package com.gentics.mesh.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.repository.MeshNodeRepository;
import com.gentics.mesh.core.repository.TagRepository;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private MeshNodeRepository contentRepository;

	@Autowired
	private TagRepository tagRepository;


	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
	}

}
