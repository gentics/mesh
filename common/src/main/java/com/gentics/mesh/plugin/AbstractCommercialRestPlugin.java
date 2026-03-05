package com.gentics.mesh.plugin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_YAML;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.OpenAPIOptions;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.util.MeshOpenAPIv3Generator;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.vertx.ext.web.Router;

public abstract class AbstractCommercialRestPlugin<T> extends AbstractCommercialPlugin<T> implements RestPlugin {

	public AbstractCommercialRestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Pair<Router, Router> createRouters() {
		Pair<Router, Router> routers = RestPlugin.super.createRouters();
		Router globalRouter = routers.getLeft();
		Router projectRouter = routers.getRight();
		OpenAPIOptions openApiOptions = environment().options().getOpenAPIOptions();
		String pluginsWithOpenAPI = environment().options().getOpenAPIOptions().getPluginsWithOwnEndpoints();
		if (StringUtils.isNotBlank(pluginsWithOpenAPI) 
				&& ("ALL".equals(pluginsWithOpenAPI) 
						|| Arrays.stream(pluginsWithOpenAPI.split(",")).map(String::trim).anyMatch(id -> id.equals(id())))) {
			Version defaultVersion = openApiOptions.getDefaultVersion();
			com.gentics.mesh.etc.config.Format defaultFormat = openApiOptions.getDefaultFormat();
			Pair<Router, String> globalRouterPair = Pair.of(globalRouter, "/api/v" + MeshVersion.CURRENT_API_VERSION + "/plugins/" + restApiName());
			InternalEndpointBuilder.wrap(globalRouter)
					.withPath("/openapi." + defaultFormat.name().toLowerCase())
					.withMethod(GET)
					.withDescription("Provides an OpenAPI v" + defaultVersion.pretty() + " " + defaultFormat.name() + " document for all registered endpoints of " + name())
					.withDisplayName("OpenAPI specification")
					.withExampleResponse(OK, "Not yet specified")
					.produces(defaultFormat == com.gentics.mesh.etc.config.Format.JSON ? APPLICATION_JSON : APPLICATION_YAML)
					.withBlockingHandler(rc -> {
							try {
								// Collect available servers
								HttpServerConfig httpServerConfig = environment().options().getHttpServerOptions();	
								List<String> servers = Collections.singletonList((httpServerConfig.isSsl() ? "https://" : "http://") + httpServerConfig.getHost() + ":" + (httpServerConfig.isSsl() ? httpServerConfig.getSslPort() : httpServerConfig.getPort()));

								// Make an instance with blacklist path patterns
								MeshOpenAPIv3Generator openApiV3Generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), servers, Optional.empty(), Optional.empty());

								rc.response().send(openApiV3Generator.generate(
										name() + " OpenAPI v" + defaultVersion.pretty() + " specification",
										projectRouter != null 
											? Stream.of(
													//... from plugin project root
													Pair.of(projectRouter, "/api/v" + MeshVersion.CURRENT_API_VERSION + "/{project}/plugins/" + restApiName()),
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
		return routers;
	}
}
