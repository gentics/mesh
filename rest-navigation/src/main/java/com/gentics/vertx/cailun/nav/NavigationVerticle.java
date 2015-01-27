package com.gentics.vertx.cailun.nav;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class NavigationVerticle extends AbstractCailunRestVerticle {

	@Autowired
	NavigationConfiguration navigationConfig;

	private static final Logger log = LoggerFactory.getLogger(NavigationVerticle.class);

	public NavigationVerticle() {
		super("nav");
	}

	@Override
	public void start() throws Exception {
		super.start();
		addNavigationHandler();
	}

	private void addNavigationHandler() {
		// route("/get").method(GET).failureHandler(eh -> {
		// log.error("Error while handling request.", eh.failure());
		// });

		route("/get").method(GET).handler(navigationConfig.navigationRequestHandler());
	}

}
