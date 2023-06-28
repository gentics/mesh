package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.favorite.config.FavoritePluginConfig;
import com.gentics.mesh.plugin.favorite.dagger.DaggerFavoritePluginComponent;
import com.gentics.mesh.plugin.favorite.dagger.FavoritePluginComponent;
import com.gentics.mesh.plugin.favorite.error.FavoritePluginError;
import com.gentics.mesh.plugin.favorite.handler.AbstractFavoriteHandler;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.pf4j.PluginWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.*;

public class FavoritePlugin extends AbstractPlugin implements RestPlugin {

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
		projectRouter.route("/:type").method(POST).handler(fromComponent(FavoritePluginComponent::getOverrideFavoriteHandler));
		projectRouter.route("/:type").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FavoritePluginComponent::getFavoriteHandler));
		projectRouter.route("/:type/:id").method(POST).handler(fromComponent(FavoritePluginComponent::storeFavoriteHandler));
		projectRouter.route("/:type/:id").method(DELETE).handler(fromComponent(FavoritePluginComponent::getDeleteFavoriteHandler));
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
		return meshPluginService.prepareProjects(
				projectName,
				meshPluginService.ensureSchemas(FavoriteService.PLUGIN_SCHEMA_NAME, FavoriteService.FAVORITE_SCHEMA_NAME, FavoriteService.SECTION_SCHEMA_NAME),
				meshPluginService.ensureFolders(FavoriteService.PLUGIN_SCHEMA_NAME, "/" + FavoriteService.FAVORITE_PLUGIN_NAME));
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
		} else {
			context.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).setStatusMessage("Internal Server Error").end();
		}
	}

}