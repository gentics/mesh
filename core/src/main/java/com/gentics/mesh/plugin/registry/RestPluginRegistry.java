package com.gentics.mesh.plugin.registry;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.router.PluginRouter;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.util.MeshOpenAPIv3Generator;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.reactivex.Completable;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * {@link PluginRegistry} which keeps track of plugins which extend the REST API.
 */
@Singleton
public class RestPluginRegistry implements PluginRegistry {

	private static final Logger log = LoggerFactory.getLogger(RestPluginRegistry.class);

	/**
	 * Set which is used to detect conflicting deployments.
	 */
	private Set<String> apiNameSyncSet = Collections.synchronizedSet(new HashSet<>());

	private final RouterStorageRegistryImpl routerStorageRegistry;

	private final MeshOptions options;

	private final ClusterManager clusterManager;

	@Inject
	public RestPluginRegistry(RouterStorageRegistryImpl routerStorageRegistry, MeshOptions options, ClusterManager clustterManager) {
		this.routerStorageRegistry = routerStorageRegistry;
		this.options = options;
		this.clusterManager = clustterManager;
	}

	@Override
	public Completable register(MeshPlugin plugin) {
		return Completable.create(sub -> {
			if (plugin instanceof RestPlugin) {
				RestPlugin restPlugin = ((RestPlugin) plugin);
				apiNameSyncSet.add(restPlugin.restApiName());
				String name = plugin.name();
				String apiName = restPlugin.restApiName();
				log.info("Registering rest plugin {" + name + "} with id {" + plugin.id() + "}");
				for (RouterStorage rs : routerStorageRegistry.getInstances()) {
					String pluginsWithOpenAPI = options.getOpenAPIOptions().getPluginsWithOwnEndpoints();
					PluginRouter globalPluginRouter = rs.root().apiRouter().pluginRouter();
					PluginRouter projectPluginRouter = rs.root().apiRouter().projectsRouter().projectRouter().pluginRouter();

					Router projectRouter = restPlugin.createProjectRouter();
					if (projectRouter != null) {
						projectPluginRouter.addRouter(apiName, projectRouter);
						log.info("Registering REST API Plugin {" + name + "} for projects");
					}
					Router globalRouter = restPlugin.createGlobalRouter();
					if (globalRouter == null && !"NONE".equals(pluginsWithOpenAPI)) {
						globalRouter = Router.router(Vertx.vertx());
					}
					if (globalRouter != null) {
						if (StringUtils.isNotBlank(pluginsWithOpenAPI) 
								&& ("ALL".equals(pluginsWithOpenAPI) 
										|| Arrays.stream(pluginsWithOpenAPI.split(",")).map(String::trim).anyMatch(id -> id.equals(restPlugin.id())))) {
							Version defaultVersion = options.getOpenAPIOptions().getDefaultVersion();
							com.gentics.mesh.etc.config.Format defaultFormat = options.getOpenAPIOptions().getDefaultFormat();
							Pair<Router, String> globalRouterPair = Pair.of(globalRouter, "/api/v" + MeshVersion.CURRENT_API_VERSION + "/plugins/" + restPlugin.restApiName());
							InternalEndpointBuilder.wrap(globalRouter)
									.withPath("/openapi." + defaultFormat.name().toLowerCase())
									.withMethod(GET)
									.withDescription("Endpoint which provides a OpenAPI v" + defaultVersion.pretty() + " " + defaultFormat.name() + " document for all registered endpoints of " + restPlugin.name())
									.withDisplayName("OpenAPI specification")
									.withExampleResponse(OK, "Not yet specified")
									.produces(defaultFormat == com.gentics.mesh.etc.config.Format.JSON ? APPLICATION_JSON : APPLICATION_YAML)
									.withBlockingHandler(rc -> {
											try {
												// Collect available servers
												HttpServerConfig httpServerConfig = options.getHttpServerOptions();	
												Supplier<List<String>> noClusterServerSupplier = () -> Collections.singletonList((httpServerConfig.isSsl() ? "https://" : "http://") + httpServerConfig.getHost() + ":" + (httpServerConfig.isSsl() ? httpServerConfig.getSslPort() : httpServerConfig.getPort()));
												List<String> servers;
												try {
													servers = Optional.ofNullable(clusterManager.getHazelcast())
															.map(hz -> hz.getCluster().getMembers().stream().map(m -> m.getAddress().getHost() + ":" + m.getAddress().getPort()).collect(Collectors.toList()))
															.orElseGet(noClusterServerSupplier);
												} catch (Throwable e) {
													log.error("Could not retrieve the server list out of Hazelcast", e);
													servers = noClusterServerSupplier.get();
												}

												// Make an instance with blacklist path patterns
												MeshOpenAPIv3Generator openApiV3Generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), servers, Optional.empty(), Optional.empty());

												rc.response().send(openApiV3Generator.generate(
														restPlugin.name() + " OpenAPI v" + defaultVersion.pretty() + " specification",
														projectRouter != null 
															? Stream.of(
																	//... from plugin project root
																	Pair.of(projectRouter, "/api/v" + MeshVersion.CURRENT_API_VERSION + "/{project}/plugins/" + restPlugin.restApiName()),
																	//... from generic plugin root
																	globalRouterPair
																).collect(Collectors.toMap(Pair::getKey, Pair::getValue))
															: Map.of(globalRouterPair.getKey(), globalRouterPair.getValue()), 
														defaultFormat == com.gentics.mesh.etc.config.Format.JSON ? Format.JSON : Format.YAML,
														true,
														defaultVersion == Version.V31));
											} catch (OpenAPIGenerationException e) {
												rc.fail(error(INTERNAL_SERVER_ERROR, "error_internal", e));
											}
										}, false)
									.build();
						}
						globalPluginRouter.addRouter(apiName, globalRouter);
						log.info("Registering REST API Plugin {" + name + "} for globally");
					}
				}
			}
			sub.onComplete();
		}).doOnError(error -> {
			if (error instanceof GenericRestException) {
				String key = ((GenericRestException) error).getI18nKey();
				// Don't remove the syncset entry if the plugin has already been deployed.
				if ("admin_plugin_error_plugin_already_deployed".equals(key)) {
					return;
				}
			}
			if (plugin instanceof RestPlugin) {
				apiNameSyncSet.remove(((RestPlugin) plugin).restApiName());
			}

		});

	}

	@Override
	public Completable deregister(MeshPlugin plugin) {
		return Completable.create(sub -> {
			if (plugin instanceof RestPlugin) {
				RestPlugin restPlugin = (RestPlugin) plugin;
				String name = plugin.name();
				log.info("Deregistering {" + name + "} rest plugin.");
				String apiName = restPlugin.restApiName();
				for (RouterStorage rs : routerStorageRegistry.getInstances()) {
					PluginRouter globalPluginRouter = rs.root().apiRouter().pluginRouter();
					PluginRouter projectPluginRouter = rs.root().apiRouter().projectsRouter().projectRouter().pluginRouter();

					// Routers can't be deleted so we need to just clear them of any routes.
					globalPluginRouter.removeRouter(apiName);
					projectPluginRouter.removeRouter(apiName);
				}
				apiNameSyncSet.remove(apiName);
			}
			sub.onComplete();
		});
	}

	@Override
	public void checkForConflict(MeshPlugin plugin) {
		if (plugin instanceof RestPlugin) {
			RestPlugin restPlugin = (RestPlugin) plugin;
			String apiName = restPlugin.restApiName();
			String name = plugin.name();
			if (apiNameSyncSet.contains(apiName)) {
				GenericRestException error = error(BAD_REQUEST, "admin_plugin_error_plugin_already_deployed", name, apiName);
				log.error("The plugin {" + name + "} can't be deployed because another plugin already uses the same apiName {" + apiName + "}",
					error);
				throw error;
			}
		}
	}
}
