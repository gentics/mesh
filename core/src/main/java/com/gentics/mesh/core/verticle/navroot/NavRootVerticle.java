package com.gentics.mesh.core.verticle.navroot;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class NavRootVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private NavRootHandler handler;

	protected NavRootVerticle() {
		super("navroot");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addPathHandler();
	}

	private Route pathRoute() {
		return getRouter().routeWithRegex("\\/(.*)");
	}

	private void addPathHandler() {
		pathRoute().method(GET).handler(rc -> handler.handleGetPath(rc));
	}
}
