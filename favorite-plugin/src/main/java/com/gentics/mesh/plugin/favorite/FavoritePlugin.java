package com.gentics.mesh.plugin.favorite;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.plugin.AbstractCommercialRestPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.favorite.config.FavoritePluginConfig;
import com.gentics.mesh.plugin.favorite.dagger.DaggerFavoritePluginComponent;
import com.gentics.mesh.plugin.favorite.dagger.FavoritePluginComponent;
import com.gentics.mesh.plugin.favorite.error.FavoritePluginError;
import com.gentics.mesh.plugin.favorite.handler.AbstractFavoriteHandler;
import com.gentics.mesh.plugin.favorite.model.FavoriteResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class FavoritePlugin extends AbstractCommercialRestPlugin<FavoritePluginConfig> implements RestPlugin {

	public final static String NAME = "Favorite Plugin";

	private static final Logger log = LoggerFactory.getLogger(FavoritePlugin.class);

	private FavoritePluginConfig configuration;
	private MeshPluginService meshPluginService;
	private Map<String, String> rootNodeUuids = new HashMap<>();

	public FavoritePlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);

	}

	@Override
	public Router createProjectRouter() {
		log.info("Registering routes for {" + name() + "}");
		Router projectRouter = Router.router(vertx());
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Create favorites")
			.withPath("/:type")
			.withMethod(POST)
			.withUriParameter("type", "Favorite type", "some_known_fav_type")
			.produces(APPLICATION_JSON)
			.consumes(APPLICATION_JSON)
			.withExampleRequest(new JsonObject().put(FavoriteService.FAVORITE_FIELD, new JsonArray().add("some_fav").add("some_other_fav")))
			.withExampleResponse(HttpResponseStatus.CREATED, "Favorites created")
			.withHandler(fromComponent(FavoritePluginComponent::getOverrideFavoriteHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Obtain favorites")
			.withPath("/:type")
			.withUriParameter("type", "Favorite type", "some_known_fav_type")
			.withMethod(GET)
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new FavoriteResponse().setFavorites(List.of("some_fav", "some_other_favs")), "Favorites")
			.withHandler(fromComponent(FavoritePluginComponent::getFavoriteHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Update favorites of given type with the given favorite")
			.withPath("/:type/:id")
			.withUriParameter("type", "Favorite type", "some_known_fav_type")
			.withUriParameter("id", "Favorite", "some_fav")
			.withMethod(POST)
			.withExampleResponse(HttpResponseStatus.CREATED, "Favorites updated")
			.withHandler(fromComponent(FavoritePluginComponent::storeFavoriteHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Delete the given favorite from the list of a given type")
			.withPath("/:type/:id")
			.withUriParameter("type", "Favorite type", "some_known_fav_type")
			.withUriParameter("id", "Favorite", "some_fav")
			.withMethod(DELETE)
			.withHandler(fromComponent(FavoritePluginComponent::getDeleteFavoriteHandler))
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Favorite deleted")
			.build();
		return projectRouter;
	}

	public Completable initialize() {
		try {
			configuration = readConfig(FavoritePluginConfig.class);
			if (configuration == null) {
				configuration = new FavoritePluginConfig();
			}
			configuration.overrideWithEnv();

			LicenseService licenseService = new LicenseService(configuration);
			licenseService.isLicenseSufficient(LicensedPlugin.MFAV);

			meshPluginService = new MeshPluginService(getRxVertx(), adminClient(), getStorageDir());
			return initializeMeshProjects(configuration.getProjects());
		} catch (Exception e) {
			return Completable.error(e);
		}
	}

	private Completable initializeMeshProjects(List<String> projectName) {
		List<String> pluginSchemas = List.of(FavoriteService.PLUGIN_SCHEMA_NAME, FavoriteService.FAVORITE_SCHEMA_NAME, FavoriteService.SECTION_SCHEMA_NAME);

		return meshPluginService.ensureSchemasGlobal(StringUtils.EMPTY, pluginSchemas)
			.flatMapCompletable(schemas -> meshPluginService.prepareProjects(
					projectName,
					meshPluginService.ensureSchemas(StringUtils.EMPTY, pluginSchemas, Optional.of(schemas)),
					meshPluginService.ensureFolders(FavoriteService.PLUGIN_SCHEMA_NAME, "/" + FavoriteService.FAVORITE_PLUGIN_NAME)));
	}

	private Handler<RoutingContext> fromComponent(Function<FavoritePluginComponent, AbstractFavoriteHandler> handler) {
		return context -> {
			PluginContext meshContext = wrap(context);
			MeshRestClient userClient = meshContext.client();
			FavoritePluginComponent component = DaggerFavoritePluginComponent.builder()
					.configuration(configuration)
					.adminClient(adminClient())
					.meshPluginService(meshPluginService)
					.pluginContext(meshContext)
					.rxVertx(getRxVertx())
					.configuration(configuration)
					.adminClient(adminClient())
					.userClient(userClient)
					.build();

			try {
				handler.apply(component).handle(meshContext)
						.subscribe(() -> {}, t -> handleError(context, t));

			} catch (Throwable e){
				handleError(context, e);

			}
		};
	}
	@Override
	public Completable shutdown() {
		return meshPluginService.unregisterEvents();
	}

	private void handleError(RoutingContext context, Throwable t) {
		log.error("Error while handling request", t);
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = (MeshRestClientMessageException) t;
			context.response().setStatusCode(meshException.getStatusCode()).setStatusMessage(meshException.getMessage()).end();
		} else if (t instanceof GenericRestException) {
			GenericRestException restException = (GenericRestException) t;
			context.response().setStatusCode(restException.getStatus().code()).setStatusMessage(restException.getMessage()).end();
		} else if (t instanceof FavoritePluginError) {
			FavoritePluginError FavoritePluginError = (FavoritePluginError) t;
			context.response().setStatusCode(FavoritePluginError.getHttpStatusCode()).setStatusMessage(FavoritePluginError.getMessage())
					.end();
		} else if (t instanceof MeshPluginException meshPluginException) {
			context.response().setStatusCode(meshPluginException.getStatusCode()).setStatusMessage(meshPluginException.getMessage()).end();
		} else {
			context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).setStatusMessage("Internal Server Error").end();
		}
	}

}