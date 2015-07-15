package com.gentics.mesh.core.verticle.project;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectMoveVerticle extends AbstractCoreApiVerticle {

	protected ProjectMoveVerticle() {
		super("move");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// TODO Auto-generated method stub
		
	}

}
