package com.gentics.mesh.plugin.comment.dagger;

import javax.inject.Named;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.service.CommentService;
import com.gentics.mesh.plugin.comment.testclient.TestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.json.JsonObject;

@Singleton
@Component(modules = TestModule.class)
public interface TestComponent {

	@AdminClient
	TestClient adminClient();

	@UserClient
	TestClient userClient();

	Configuration configuration();

	CommentService commentService();

	@Component.Builder
	interface Builder {

		/**
		 * Name of the current project.
		 * 
		 * @param project
		 * @return
		 */
		@BindsInstance
		Builder project(JsonObject project);

		/**
		 * Configuration to be injected.
		 * 
		 * @param config
		 * @return
		 */
		@BindsInstance
		Builder configuration(@Named("original") Configuration config);

		TestComponent build();
	}
}
