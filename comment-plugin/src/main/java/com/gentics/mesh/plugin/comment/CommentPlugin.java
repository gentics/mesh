package com.gentics.mesh.plugin.comment;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.dagger.CommentPluginComponent;
import com.gentics.mesh.plugin.comment.dagger.DaggerCommentPluginComponent;
import com.gentics.mesh.plugin.comment.dagger.DaggerInitializationComponent;
import com.gentics.mesh.plugin.comment.dagger.InitializationComponent;
import com.gentics.mesh.plugin.comment.handler.AbstractCommentHandler;
import com.gentics.mesh.plugin.comment.service.TemplateService;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class CommentPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(CommentPlugin.class);
	private Configuration configuration;
	// TODO ugly hack?
	private TemplateService templateService;

	public CommentPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createProjectRouter() {
		Router projectRouter = Router.router(vertx());
		log.info("Registering routes for {" + name() + "}");
		// Create
		projectRouter.route("/comments")
			.method(POST)
			.consumes(HttpConstants.APPLICATION_JSON)
			.handler(BodyHandler.create())
			.handler(fromComponent(CommentPluginComponent::createCommentHandler));

		// Read
		projectRouter.route("/comments/:uuid")
			.method(GET)
			.handler(fromComponent(CommentPluginComponent::fetchCommentHandler));

		projectRouter.route("/comments")
			.method(GET)
			.handler(fromComponent(CommentPluginComponent::fetchCommentsHandler));

		// Update
		projectRouter.route("/comments/:uuid")
			.method(POST)
			.consumes(HttpConstants.APPLICATION_JSON)
			.handler(BodyHandler.create())
			.handler(fromComponent(CommentPluginComponent::updateCommentHandler));

		// Delete
		projectRouter.route("/comments/:uuid")
			.method(DELETE)
			.handler(fromComponent(CommentPluginComponent::deleteCommentHandler));

		projectRouter.route("/node/:uuid/comments")
			.method(GET)
			.handler(fromComponent(CommentPluginComponent::sectionCommentsHandler));

		// Approval handling
		projectRouter.route("/comments/:uuid/publish")
			.method(POST)
			.handler(fromComponent(CommentPluginComponent::publishCommentHandler));

		projectRouter.route("/comments/:uuid/unpublish")
			.method(POST)
			.handler(fromComponent(CommentPluginComponent::unpublishCommentHandler));

		// Javascript template engine
		projectRouter.route("/templates/js")
			.produces("application/javascript")
			// TODO caching headers or static handler
			.method(GET)
			.handler(fromComponent(CommentPluginComponent::templateEngineHandler));
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

			LicenseService licenseService = new LicenseService(configuration.getLicenseKey());
			licenseService.isLicenseSufficient(LicensedPlugin.MCP);

			InitializationComponent component = DaggerInitializationComponent.builder()
				.configuration(configuration)
				.adminClient(adminClient())
				.storageDir(getStorageDir())
				.vertx(getRxVertx())
				.build();

			this.templateService = component.templateService();
			MeshPluginService pluginService = component.meshPluginService();
			return Completable.mergeArray(
				pluginService.prepareProjects(configuration.getProjectNames(),
					pluginService.ensureSchemas(configuration.getSchemaPrefix(), Arrays.asList("comment", "section", "folder"))
				),
				templateService.initialize());
		} catch (Exception e) {
			return Completable.error(e);
		}
	}
}
