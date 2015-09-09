package com.gentics.mesh.core.verticle.webroot;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.handler.ActionContext;

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

	// TODO findbyproject path should also handle files and contents and store the type of the segment
	// TODO last segment can also be a file or a content. Handle this
	private void addPathHandler() {

		pathRoute().method(GET).produces(APPLICATION_JSON).handler(rc -> {
			handler.handleGetPath(ActionContext.create(rc));
		});

	}

}
