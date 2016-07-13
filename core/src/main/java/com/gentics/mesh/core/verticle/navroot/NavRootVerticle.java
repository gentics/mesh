package com.gentics.mesh.core.verticle.navroot;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.rest.Endpoint;

@Component
@Scope("singleton")
@SpringVerticle
public class NavRootVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private NavRootHandler handler;

	public NavRootVerticle() {
		super("navroot");
	}

	@Override
	public void registerEndPoints() throws Exception {
		if (springConfiguration != null) {
			route("/*").handler(springConfiguration.authHandler());
		}
		addPathHandler();
	}

	private void addPathHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.pathRegex("\\/(.*)");
		endpoint.method(GET);
		endpoint.description("Return a navigation for the node which is located using the given path.");
		endpoint.handler(rc -> handler.handleGetPath(rc));
	}
}
