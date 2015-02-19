package com.gentics.cailun.nav;

import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;

@Component
@Scope("singleton")
@SpringVerticle
public class NavigationVerticle extends AbstractProjectRestVerticle {

	@Autowired
	NavigationConfiguration navigationConfig;

	public NavigationVerticle() {
		super("navigation");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addNavigationHandler();
	}

	private void addNavigationHandler() {
		route().method(GET).handler(navigationConfig.navigationRequestHandler());
	}

}
