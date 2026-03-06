package com.gentics.mesh.plugin.comment;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.CommercialRestPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.dagger.CommentPluginComponent;
import com.gentics.mesh.plugin.comment.dagger.DaggerCommentPluginComponent;
import com.gentics.mesh.plugin.comment.dagger.DaggerInitializationComponent;
import com.gentics.mesh.plugin.comment.dagger.InitializationComponent;
import com.gentics.mesh.plugin.comment.handler.AbstractCommentHandler;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.comment.model.request.CreateCommentRequest;
import com.gentics.mesh.plugin.comment.model.request.UpdateCommentRequest;
import com.gentics.mesh.plugin.comment.service.TemplateService;
import com.gentics.mesh.plugin.comment.util.Examples;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class CommentPlugin extends AbstractPlugin implements CommercialRestPlugin {

	private static final Logger log = LoggerFactory.getLogger(CommentPlugin.class);
	private Configuration configuration;
	// TODO ugly hack?
	private TemplateService templateService;

	static {
		JsonUtil.getMapper().registerModule(new Jdk8Module());
	}

	public CommentPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createProjectRouter() {
		Router projectRouter = Router.router(vertx());
		log.info("Registering routes for {" + name() + "}");

		// Create
		InternalEndpointBuilder.wrap(projectRouter)
			.withPath("/comments")
			.withDescription("Post a new comment")
			.withMethod(POST)
			.consumes(HttpConstants.APPLICATION_JSON)
			.produces(HttpConstants.APPLICATION_JSON)
			.withExampleRequest(new CreateCommentRequest())
			.withExampleResponse(HttpResponseStatus.OK, Examples.createComment(), "A new comment")
			.withHandler(BodyHandler.create())
			.withHandler(fromComponent(CommentPluginComponent::createCommentHandler))
			.build();

		// Read
		InternalEndpointBuilder.wrap(projectRouter)
			.withPath("/comments/:uuid")
			.withDescription("Find a comment with UUID")
			.withMethod(GET)
			.withUriParameter("uuid", "UUID of a comment node", UUIDUtil.randomUUID())
			.withExampleResponse(HttpResponseStatus.OK, Examples.createComment(), "Found comment")
			.withHandler(fromComponent(CommentPluginComponent::fetchCommentHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withPath("/comments")
			.withDescription("Find comments")
			.withMethod(GET)
			.withExampleResponse(HttpResponseStatus.OK, Examples.createComment(), "Found comment")
			.withHandler(fromComponent(CommentPluginComponent::fetchCommentsHandler))
			.build();

		// Update
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Update a comment with given UUID")
			.withPath("/comments/:uuid")
			.withMethod(POST)
			.consumes(HttpConstants.APPLICATION_JSON)
			.produces(HttpConstants.APPLICATION_JSON)
			.withUriParameter("uuid", "UUID of a comment node", UUIDUtil.randomUUID())
			.withExampleRequest(new UpdateCommentRequest())
			.withExampleResponse(HttpResponseStatus.OK, Examples.createComment(), "An updated comment")
			.withHandler(BodyHandler.create())
			.withHandler(fromComponent(CommentPluginComponent::updateCommentHandler))
			.build();

		// Delete
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Delete acomment with given UUID")
			.withPath("/comments/:uuid")
			.withUriParameter("uuid", "UUID of a comment node", UUIDUtil.randomUUID())
			.withMethod(DELETE)
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Comment successfully deleted")
			.withHandler(fromComponent(CommentPluginComponent::deleteCommentHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get comments of a node of a given UUID")
			.withPath("/node/:uuid/comments")
			.withMethod(GET)
			.withUriParameter("uuid", "UUID of a node", UUIDUtil.randomUUID())
			.withHandler(fromComponent(CommentPluginComponent::sectionCommentsHandler))
			.withExampleResponse(HttpResponseStatus.OK, new NestedCommentSection(), "Comments section")
			.build();

		// Approval handling
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Approve a comment with given UUID")
			.withPath("/comments/:uuid/publish")
			.withMethod(POST)
			.withUriParameter("uuid", "UUID of a comment node", UUIDUtil.randomUUID())
			.withHandler(fromComponent(CommentPluginComponent::publishCommentHandler))
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Comment published")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Revoke a comment with given UUID")
			.withPath("/comments/:uuid/unpublish")
			.withMethod(POST)
			.withUriParameter("uuid", "UUID of a comment node", UUIDUtil.randomUUID())
			.withHandler(fromComponent(CommentPluginComponent::unpublishCommentHandler))
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Comment unpublished")
			.build();

		// Javascript template engine
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get JS Template")
			.withPath("/templates/js")
			.produces("application/javascript")
			.withExampleResponse(HttpResponseStatus.OK, "function example() {}", "Compiled JS template")
			// TODO caching headers or static handler
			.withMethod(GET)
			.withHandler(fromComponent(CommentPluginComponent::templateEngineHandler))
			.build();
		return projectRouter;
	}

	private Handler<RoutingContext> fromComponent(Function<CommentPluginComponent, AbstractCommentHandler> handler) {
		return context -> {
			PluginContext meshContext = wrap(context);
			MeshRestClient userClient = meshContext.client();

			CommentPluginComponent component = DaggerCommentPluginComponent.builder()
				.project(meshContext.project())
				.adminClient(adminClient())
				.userClient(userClient)
				.configuration(configuration)
				.templateService(templateService)
				.vertx(getRxVertx())
				.routingContext(new io.vertx.reactivex.ext.web.RoutingContext(context))
				.storageDir(getStorageDir())
				.build();

			handler.apply(component).handle(meshContext)
				.doFinally(userClient::close)
				.subscribe();
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T readConfig(Class<T> clazz) throws FileNotFoundException, IOException {
		T config = super.readConfig(clazz);
		if (config == null) {
			config = (T) new Configuration();
		}
		return config;
	}

	@Override
	public Completable initialize() {
		try {
			this.configuration = readConfig(Configuration.class);
			this.configuration.overrideWithEnv();

			LicenseService licenseService = new LicenseService(configuration);
			licenseService.isLicenseSufficient(LicensedPlugin.MCMT);

			InitializationComponent component = DaggerInitializationComponent.builder()
				.configuration(configuration)
				.adminClient(adminClient())
				.storageDir(getStorageDir())
				.vertx(getRxVertx())
				.build();

			this.templateService = component.templateService();
			MeshPluginService pluginService = component.meshPluginService();
			return pluginService.ensureSchemasGlobal(configuration.getSchemaPrefix(), Arrays.asList("comment", "section", "folder"))
					.flatMapCompletable(schemas -> Completable.mergeArray(
							pluginService.prepareProjects(configuration.getProjectNames(),
									pluginService.ensureSchemas(configuration.getSchemaPrefix(), Arrays.asList("comment", "section", "folder"), Optional.of(schemas))
								),
								templateService.initialize()));
			
		} catch (Exception e) {
			return Completable.error(e);
		}
	}
}
