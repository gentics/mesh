package com.gentics.mesh.core.verticle.project;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectSearchVerticle extends AbstractProjectRestVerticle {

	public ProjectSearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
	}

}
