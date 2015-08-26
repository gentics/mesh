package com.gentics.mesh.core.verticle.webroot;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class WebRootVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(WebRootVerticle.class);

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
			handler.handleGetPath(rc);
		});

	}

}
