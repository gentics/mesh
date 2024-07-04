package com.gentics.mesh.plugin.like;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.like.config.LikePluginConfig;
import com.gentics.mesh.plugin.like.dagger.DaggerLikePluginComponent;
import com.gentics.mesh.plugin.like.dagger.LikePluginComponent;
import com.gentics.mesh.plugin.like.error.LikePluginError;
import com.gentics.mesh.plugin.like.handler.AbstractLikeHandler;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class LikePlugin extends AbstractPlugin implements RestPlugin {

	public final static String NAME = "Like Plugin";

	private static final Logger log = LoggerFactory.getLogger(LikePlugin.class);

	private LikePluginConfig configuration;

	private Map<String, String> rootNodeUuids = new HashMap<>();

	public LikePlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createProjectRouter() {
		log.info("Registering routes for {" + name() + "}");
		Router projectRouter = Router.router(vertx());

		// post a like for the given node
		projectRouter.route("/likes/:nodeUuid/:lang").method(POST).handler(fromComponent(LikePluginComponent::storeLikeHandler));

		// get the likes for a given node
		projectRouter.route("/likes/:nodeUuid/:lang").method(GET).handler(fromComponent(LikePluginComponent::getLikeHandler));

		// delete the like for a node
		projectRouter.route("/likes/:nodeUuid/:lang").method(DELETE).handler(fromComponent(LikePluginComponent::deleteLikeHandler));

		// get the statistics for a given node
		projectRouter.route("/likes/:nodeUuid/:lang/statistics").method(GET).handler(fromComponent(LikePluginComponent::getStatisticsHandler));

		// get the templates
		projectRouter.route("/templates").method(GET).handler(fromComponent(LikePluginComponent::templateEngineHandler));
		return projectRouter;
	}

	@Override
	public Completable initialize() {
		try {
			configuration = readConfig(LikePluginConfig.class);
			if (configuration == null) {
				configuration = new LikePluginConfig();
			}
			configuration.overrideWithEnv();

			LicenseService licenseService = new LicenseService(configuration);
			licenseService.isLicenseSufficient(LicensedPlugin.MLKE);

			// initialize schemas, etc.
			return component(null).schemaInitializer().initialize();
		} catch (Exception e) {
			return Completable.error(e);
		}

	}

	private LikePluginComponent component(MeshRestClient userClient) {
		return DaggerLikePluginComponent.builder()
			.configuration(configuration)
			.adminClient(adminClient())
			.userClient(userClient)
			.rootNodeUuids(rootNodeUuids)
			.vertx(vertx())
			.build();
	}

	private Handler<RoutingContext> fromComponent(Function<LikePluginComponent, AbstractLikeHandler> handler) {
		return context -> {
			PluginContext meshContext = wrap(context);
			MeshRestClient userClient = meshContext.client();

			// TODO better error handler
			handler.apply(component(userClient)).handle(meshContext).doOnError(t -> {
				log.error("Error while handling request", t);
				if (t instanceof MeshRestClientMessageException) {
					MeshRestClientMessageException meshException = (MeshRestClientMessageException) t;
					context.response().setStatusCode(meshException.getStatusCode()).setStatusMessage(meshException.getMessage()).end();
				} else if (t instanceof GenericRestException) {
					GenericRestException restException = (GenericRestException) t;
					context.response().setStatusCode(restException.getStatus().code()).setStatusMessage(restException.getMessage()).end();
				} else if (t instanceof LikePluginError) {
					LikePluginError likePluginError = (LikePluginError) t;
					context.response().setStatusCode(likePluginError.getHttpStatusCode()).setStatusMessage(likePluginError.getMessage()).end();
				} else {
					context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).setStatusMessage("Internal Server Error").end();
				}
			}).subscribe();
		};
	}
}
