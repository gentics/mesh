package com.gentics.mesh.plugin.comment;

import io.vertx.core.json.JsonObject;
import org.junit.Before;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.dagger.DaggerTestComponent;
import com.gentics.mesh.plugin.comment.dagger.TestComponent;
import com.gentics.mesh.plugin.comment.service.CommentService;
import com.gentics.mesh.plugin.comment.testclient.TestClient;

public abstract class AbstractPluginTest {

	protected CommentService service;
	protected TestClient userClient;
	protected TestComponent component;
	protected TestClient adminClient;

	@Before
	public void setUp() throws Exception {
		Configuration config = new Configuration();
		config.setCommentRoot("/comments");

		JsonObject project = new JsonObject().put("name", "dummy");

		component = DaggerTestComponent.builder()
			.project(project)
			.configuration(config)
			.build();
		service = component.commentService();
		adminClient = component.adminClient();
		userClient = component.userClient();
	}
}
