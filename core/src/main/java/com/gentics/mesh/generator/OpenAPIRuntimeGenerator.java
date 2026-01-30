package com.gentics.mesh.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.rest.InternalEndpointRoute;
import com.github.jknack.handlebars.internal.lang3.StringUtils;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.vertx.ext.web.Router;

public final class OpenAPIRuntimeGenerator {

	public static String fromRouter(Router router) {
		OpenAPI openApi = new OpenAPI();
		Info info = new Info();
		List<Server> servers = new ArrayList<>();
		List<Tag> tags = new ArrayList<>();
		Paths paths = new Paths();

		info.setTitle("Gentics OpenAPI");

		openApi.setInfo(info);
		openApi.setServers(servers);
		openApi.setPaths(paths);
		openApi.setTags(tags);

		router(router, info, paths);		

		return Json.pretty(openApi);
	}

	private static final void router(Router router, Info info, Paths paths) {
		router.getRoutes().stream()
			.filter(r -> StringUtils.isNotBlank(r.getPath()))
			.forEach(r -> {
				String path = Arrays.stream(r.getPath().split("/"))
					.map(segment -> segment.startsWith(":") ? ("{" + segment.substring(1) + "}") : segment)
					.collect(Collectors.joining("/"));
				
				Operation o = new Operation();
				o.setParameters(Arrays.stream(r.getPath().split("/"))
						.filter(segment -> segment.startsWith(":"))
						.map(segment -> segment.substring(1))
						.map(segment -> {
							Parameter p = new Parameter();
							p.setName(segment);
							p.setRequired(true);
							p.setAllowEmptyValue(false);
							return p;
						}).collect(Collectors.toList()));
				PathItem i = new PathItem();
				r.methods().stream().forEach(m -> {
					switch (m.name()) {
					case "GET":
						i.setGet(o);
						break;
					case "POST":
						i.setPost(o);
						break;
					case "PUT":
						i.setPut(o);
						break;
					case "DELETE":
						i.setDelete(o);
						break;
					case "OPTIONS":
						i.setOptions(o);
						break;
					case "PATCH":
						i.setPatch(o);
						break;
					case "TRACE":
						i.setTrace(o);
						break;
					case "HEAD":
						i.setHead(o);
						break;
					}
				});
				Optional.ofNullable(r.getMetadata(InternalEndpointRoute.class.getCanonicalName()))
					.map(InternalEndpointRoute.class::cast)
					.ifPresent(ie -> {
						ie.getQueryParameters().entrySet().stream();
					});
				paths.put(path, i);

				Optional.ofNullable(r.getSubRouter())
					.ifPresent(s -> router(s, info, paths));
			});
	}
}
