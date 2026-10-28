package com.gentics.mesh.plugin.comment.dagger;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.handler.CreateCommentHandler;
import com.gentics.mesh.plugin.comment.handler.DeleteCommentHandler;
import com.gentics.mesh.plugin.comment.handler.FetchCommentHandler;
import com.gentics.mesh.plugin.comment.handler.FetchCommentsHandler;
import com.gentics.mesh.plugin.comment.handler.PublishCommentHandler;
import com.gentics.mesh.plugin.comment.handler.SectionCommentsHandler;
import com.gentics.mesh.plugin.comment.handler.TemplateEngineHandler;
import com.gentics.mesh.plugin.comment.handler.UnpublishCommentHandler;
import com.gentics.mesh.plugin.comment.handler.UpdateCommentHandler;
import com.gentics.mesh.plugin.comment.service.TemplateService;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;

@Singleton
@Component(modules = { CommentPluginModule.class })
public interface CommentPluginComponent {

	CreateCommentHandler createCommentHandler();

	FetchCommentHandler fetchCommentHandler();

	FetchCommentsHandler fetchCommentsHandler();

	PublishCommentHandler publishCommentHandler();

	UnpublishCommentHandler unpublishCommentHandler();

	DeleteCommentHandler deleteCommentHandler();

	UpdateCommentHandler updateCommentHandler();

	SectionCommentsHandler sectionCommentsHandler();

	TemplateEngineHandler templateEngineHandler();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder project(JsonObject project);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		Builder userClient(@UserClient MeshRestClient client);

		@BindsInstance
		Builder configuration(@Named("original") Configuration config);

		@BindsInstance
		Builder templateService(TemplateService templateService);

		@BindsInstance
		Builder vertx(Vertx vertx);

		@BindsInstance
		Builder routingContext(RoutingContext context);

		@BindsInstance
		Builder storageDir(File dir);

		CommentPluginComponent build();
	}
}
