package com.gentics.mesh.core.verticle;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class MicroschemaVerticle extends AbstractCoreApiVerticle {

	protected MicroschemaVerticle() {
		super("microschemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addProjectHandlers();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addReadHandlers() {
		// TODO Auto-generated method stub

	}

	private void addDeleteHandler() {
		// TODO Auto-generated method stub

	}

	private void addUpdateHandler() {
		// TODO Auto-generated method stub

	}

	private void addCreateHandler() {
		// TODO Auto-generated method stub

	}

	private void addProjectHandlers() {
		// TODO Auto-generated method stub

	}

}
