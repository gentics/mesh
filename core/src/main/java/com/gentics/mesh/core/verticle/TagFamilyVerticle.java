package com.gentics.mesh.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class TagFamilyVerticle extends AbstractProjectRestVerticle {

	public TagFamilyVerticle() {
		super("tagFamilies");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addReadHandler();
		addCreateHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addDeleteHandler() {
		// TODO Auto-generated method stub
		
	}

	private void addReadHandler() {
		// TODO Auto-generated method stub
		
	}

	private void addCreateHandler() {
		// TODO Auto-generated method stub
		
	}

	private void addUpdateHandler() {
		// TODO Auto-generated method stub
		
	}
}
