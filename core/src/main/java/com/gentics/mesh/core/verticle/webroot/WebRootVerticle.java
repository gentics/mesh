package com.gentics.mesh.core.verticle.webroot;

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
public class WebRootVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private WebRootHandler handler;

	protected WebRootVerticle() {
		super("webroot");
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
		pathRoute().method(GET).handler(rc -> {
			handler.handleGetPath(rc);
		});
	}

}
