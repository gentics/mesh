package com.gentics.mesh.changelog;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ChangelogRunner {

	private static final Logger log = LoggerFactory.getLogger(ChangelogRunner.class);

	public static void main(String[] args) {
		log.info("Starting changelog runner");
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ChangelogSpringConfiguration.class)) {
			ChangelogSystem cls = ctx.getBean(ChangelogSystem.class);
			ctx.start();
			cls.applyChanges();
		}
		log.info("Terminating changelog runner");
	}
}
