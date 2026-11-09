package com.gentics.mesh.plugin.comment.dagger;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Arrays;

import javax.inject.Singleton;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.testclient.TestClient;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.Module;
import dagger.Provides;
import io.vertx.reactivex.ext.mail.MailClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.common.template.TemplateEngine;

@Module
public class TestModule {

	@Singleton
	@Provides
	@AdminClient
	static TestClient provideAdminTestClient() {
		return new TestClient();
	}

	@Singleton
	@Provides
	@UserClient
	static TestClient provideUserTestClient() {
		return new TestClient();
	}

	@Singleton
	@Provides
	@AdminClient
	static MeshRestClient provideAdminClient(@AdminClient TestClient client) {
		return client;
	}

	@Singleton
	@Provides
	@UserClient
	static MeshRestClient provideUserClient(@UserClient TestClient client) {
		return client;
	}

	@Singleton
	@Provides
	static Configuration provideConfiguration() {
		return new Configuration()
			.setCommentRoot("/comments")
			.setProjectNames(Arrays.asList("tvc", "swa"));
	}

	@Singleton
	@Provides
	static MailClient provideMailClient() {
		return mock(MailClient.class);
	}

	@Singleton
	@Provides
	static File provideStorageDir() {
		return mock(File.class);
	}

	@Singleton
	@Provides
	static TemplateEngine provideTemplateEngine() {
		return mock(TemplateEngine.class);
	}

	@Singleton
	@Provides
	static RoutingContext provideRoutingContext() {
		return mock(RoutingContext.class);
	}
}
