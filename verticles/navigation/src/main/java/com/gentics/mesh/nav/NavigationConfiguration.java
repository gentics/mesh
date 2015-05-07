package com.gentics.mesh.nav;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.nav.model.NavigationRequestHandler;

@Configuration
public class NavigationConfiguration {

	@Bean
	public NavigationRequestHandler navigationRequestHandler() {
		return new NavigationRequestHandler();
	}

}
