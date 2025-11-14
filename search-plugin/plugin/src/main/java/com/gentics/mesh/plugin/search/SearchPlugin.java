package com.gentics.mesh.plugin.search;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.dagger.DaggerInitializationComponent;
import com.gentics.mesh.plugin.search.dagger.DaggerSearchPluginComponent;
import com.gentics.mesh.plugin.search.dagger.InitializationComponent;
import com.gentics.mesh.plugin.search.dagger.SearchPluginComponent;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.handler.AbstractQueryHandler;
import com.gentics.mesh.plugin.search.handler.custom.CustomQueryHandler;
import com.gentics.mesh.plugin.search.service.BranchService;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.vertx.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.PluginWrapper;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchPlugin extends AbstractPlugin implements RestPlugin {

	private static final Logger log = LoggerFactory.getLogger(SearchPlugin.class);

	private SearchConfiguration config;
	private QueryService queryService;
	private BranchService branchService;

	public SearchPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.fromAction(() -> {
			// TODO once Mesh offers a uniform way to handle configurations, this should be simplified.
			config = readConfig(SearchConfiguration.class);

			if (config == null) {
				log.info("No configuration found, using defaults");
				config = new SearchConfiguration();
			}
			config.overrideWithEnv();
			config.validate(getStorageDir());

			LicenseService licenseService = new LicenseService(config);
			licenseService.isLicenseSufficient(LicensedPlugin.MSRH);

			InitializationComponent init = DaggerInitializationComponent.builder()
				.configuration(config)
				.adminClient(adminClient())
				.storageDir(getStorageDir())
				.pluginManager(getWrapper().getPluginManager())
				.build();

			queryService = init.queryService();
			branchService = init.branchService();
		});
	}

	@Override
	public Router createProjectRouter() {
		log.info("Registering project routes for {}", name());

		Router router = Router.router(vertx());

		router.route("/search")
			.produces(HttpConstants.APPLICATION_JSON)
			.handler(fromComponent(SearchPluginComponent::searchHandler));

		router.route("/autocomplete")
			.produces(HttpConstants.APPLICATION_JSON)
			.handler(fromComponent(SearchPluginComponent::autocompleteHandler));

		registerCustomEndpoints(router);

		return router;
	}

	@Override
	public Router createGlobalRouter() {
		log.info("Registering global routes for {}", name());

		Router router = Router.router(vertx());
		registerCacheEndpoints(router);
		return router;
	}

	private void registerCustomEndpoints(Router projectRouter) {
		Collection<CustomHandler> customHandlers = getCustomHandlers();

		if (customHandlers.isEmpty()) {
			log.debug("No custom handlers or queries configured");
			return;
		}

		Router customRouter = Router.router(vertx());

		customHandlers.forEach(handler -> {
			String path = "/" + handler.name();

			if (log.isDebugEnabled()) {
				log.debug("Registering custom route /custom{} with handler {}", path, handler.getClass().getSimpleName());
			}

			customRouter.route(path)
					.method(HttpMethod.GET)
					.produces(HttpConstants.APPLICATION_JSON)
					.handler(fromCustomhandler(handler));

			Handler<RoutingContext> wrappedHandler = handler.type() == HandlerType.AUTOCOMPLETE
					? fromComponent(SearchPluginComponent::autocompleteHandler, handler)
					: fromComponent(SearchPluginComponent::searchHandler, handler);

			customRouter.route(path)
					.method(HttpMethod.GET)
					.produces(HttpConstants.APPLICATION_JSON)
					.handler(wrappedHandler);
		});

		projectRouter.route("/custom*").subRouter(customRouter);
	}

	private Collection<CustomHandler> getCustomHandlers() {
		// Since CustomHandler is an interface we cannot enforce that the equals() method compares the names, and
		// therefore we cannot simply use a Set<CustomHandler> here.
		Map<String, CustomHandler> handlers = getWrapper().getPluginManager().getExtensions(CustomHandler.class).stream()
			.map(handler -> Pair.of(handler.name(), handler))
			.collect(Collectors.toMap(
				Pair::getKey,
				Pair::getValue,
				(u, v) -> {
					if (log.isWarnEnabled()) {
						log.warn(
							"Multiple custom handlers with name {} found, using {}",
							u.name(),
							u.getClass().getName());
					}

					return u;
				}));

		config.getCustomQueries().stream()
			.filter(query -> !handlers.containsKey(query.getName()))
			.map(query -> new CustomQueryHandler(queryService, query))
			.forEach(handler -> handlers.put(handler.name(), handler));

		return handlers.values();
	}

	private void registerCacheEndpoints(Router globalRouter) {
		log.trace("Registering caching routes");

		globalRouter.route("/cache/clear")
			.method(HttpMethod.POST)
			.handler(cachingHandler(null));

		globalRouter.route("/cache/enable")
			.method(HttpMethod.POST)
			.handler(cachingHandler(true));

		globalRouter.route("/cache/disable")
			.method(HttpMethod.POST)
			.handler(cachingHandler(false));
	}

	private Handler<RoutingContext> fromComponent(Function<SearchPluginComponent, AbstractQueryHandler> handler) {
		return fromComponent(handler, null);
	}

	private Handler<RoutingContext> fromComponent(Function<SearchPluginComponent, AbstractQueryHandler> handler, CustomHandler customHandler) {
		return context -> {
			PluginContext pluginContext = wrap(context);
			MeshRestClient userClient = pluginContext.client();

			SearchPluginComponent component = DaggerSearchPluginComponent.builder()
				.adminClient(adminClient())
				.configuration(config)
				.pluginContext(pluginContext)
				.queryService(queryService)
				.branchService(branchService)
				.customHandler(customHandler)
				.pluginManager(getWrapper().getPluginManager())
				.build();

			handler.apply(component)
				.handle()
				.doFinally(userClient::close)
				.subscribe();
		};
	}

	private Handler<RoutingContext> fromCustomhandler(CustomHandler handler) {
		return context -> {
			PluginContext pluginContext = wrap(context);
			MeshRestClient userClient = pluginContext.client();

			handler.handle(pluginContext)
				.doFinally(userClient::close)
				.subscribe();
		};
	}

	private Handler<RoutingContext> cachingHandler(Boolean enabled) {
		return rc -> {
			String msg;

			if (enabled == null) {
				msg = "Cache cleared";
			} else {
				config.setCaching(enabled);
				msg = "Caching " + (enabled ? "enabled" : "disabled");
			}
			log.info(msg);

			queryService.clearCache();
			branchService.clearCache();

			rc.response()
				.setStatusCode(HttpResponseStatus.OK.code())
				.setStatusMessage(msg)
				.end();
		};
	}
}
