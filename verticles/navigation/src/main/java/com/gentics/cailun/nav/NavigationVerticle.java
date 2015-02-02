package com.gentics.cailun.nav;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCailunRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class NavigationVerticle extends AbstractCailunRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(NavigationVerticle.class);

	@Autowired
	NavigationConfiguration navigationConfig;

	public NavigationVerticle() {
		super("nav");
	}

	@Override
	public void start() throws Exception {
		super.start();
		addNavigationHandler();
	}

	private void addNavigationHandler() {
		route().method(GET).handler(navigationConfig.navigationRequestHandler());
	}

}
