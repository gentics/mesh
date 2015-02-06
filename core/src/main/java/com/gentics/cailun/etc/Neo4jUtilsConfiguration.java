package com.gentics.cailun.etc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gentics.cailun.util.Neo4jPageUtils;

@Configuration
public class Neo4jUtilsConfiguration {

	@Bean
	public Neo4jPageUtils pageUtils() {
		return new Neo4jPageUtils();
	}

}
