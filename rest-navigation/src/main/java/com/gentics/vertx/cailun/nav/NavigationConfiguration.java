package com.gentics.vertx.cailun.nav;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gentics.vertx.cailun.nav.model.NavigationRequestHandler;

@Configuration
public class NavigationConfiguration {

	@Bean
	public NavigationRequestHandler navigationRequestHandler() {
		return new NavigationRequestHandler();
	}

}
