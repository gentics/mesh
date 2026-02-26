package com.gentics.mesh.plugin.like;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.like.config.LikePluginConfig;
import com.gentics.mesh.plugin.like.dagger.DaggerLikePluginComponent;
import com.gentics.mesh.plugin.like.dagger.LikePluginComponent;
import com.gentics.mesh.plugin.like.error.LikePluginError;
import com.gentics.mesh.plugin.like.handler.AbstractLikeHandler;
import com.gentics.mesh.plugin.like.model.LikeResponse;
import com.gentics.mesh.plugin.like.model.PlusMinusType;
import com.gentics.mesh.plugin.like.model.PostedLike;
import com.gentics.mesh.plugin.like.model.StatisticsResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
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

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Post a like for the given node")
			.withPath("/likes/:nodeUuid/:lang").withMethod(POST)
			.withUriParameter("nodeUuid", "The target node to like", UUIDUtil.randomUUID())
			.withUriParameter("lang", "Language code", "en")
			.consumes(HttpConstants.APPLICATION_JSON)
			.withExampleRequest(new JsonObject().put("rating", 5))
			.withHandler(fromComponent(LikePluginComponent::storeLikeHandler))
			.withExampleResponse(HttpResponseStatus.CREATED, "Like stored")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get the likes for a given node")
			.withPath("/likes/:nodeUuid/:lang").withMethod(GET)
			.withUriParameter("nodeUuid", "The target node to like", UUIDUtil.randomUUID())
			.withUriParameter("lang", "Language code", "en")
			.withHandler(fromComponent(LikePluginComponent::getLikeHandler))
			.produces(HttpConstants.APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new LikeResponse().setPosted(new PostedLike().setPlusMinus(PlusMinusType.plus)), "Like response")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Delete the like for a node")
			.withPath("/likes/:nodeUuid/:lang").withMethod(DELETE)
			.withUriParameter("nodeUuid", "The target node to like", UUIDUtil.randomUUID())
			.withUriParameter("lang", "Language code", "en")
			.withHandler(fromComponent(LikePluginComponent::deleteLikeHandler))
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Like deleted")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get the statistics for a given node")	
			.withPath("/likes/:nodeUuid/:lang/statistics").withMethod(GET)
			.withUriParameter("nodeUuid", "The target node to like", UUIDUtil.randomUUID())
			.withUriParameter("lang", "Language code", "en")
			.withHandler(fromComponent(LikePluginComponent::getStatisticsHandler))
			.produces(HttpConstants.APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new StatisticsResponse().setLikes(8), "Like statistics")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get the plugin templates")
			.withPath("/templates").withMethod(GET)
			.withHandler(fromComponent(LikePluginComponent::templateEngineHandler))
			.withExampleResponse(HttpResponseStatus.OK, "Template string or JSON or HTML")
			.build();
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
