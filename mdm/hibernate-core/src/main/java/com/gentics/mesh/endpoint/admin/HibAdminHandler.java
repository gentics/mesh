package com.gentics.mesh.endpoint.admin;

import static com.gentics.mesh.core.rest.MeshEvent.CLEAR_CACHES;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.util.MeshOpenAPIv3Generator;
import com.gentics.vertx.openapi.model.Format;
import com.gentics.vertx.openapi.model.OpenAPIGenerationException;

import io.vertx.core.Vertx;

/**
 * An implementation of administration routes handler. The primary place for the admin functionality extension.
 * 
 * @author plyhun
 *
 */
public class HibAdminHandler extends AdminHandler {

	private final ContentCachedStorage contentCache;

	public HibAdminHandler(Vertx vertx, Database db, RouterStorageImpl routerStorage, BootstrapInitializer boot,
			SearchProvider searchProvider, HandlerUtilities utils, MeshOptions options,
			RouterStorageRegistryImpl routerStorageRegistry, Coordinator coordinator, WriteLock writeLock,
			ConsistencyCheckHandler consistencyCheckHandler, CacheRegistry cacheRegistry, ContentCachedStorage contentCache, ClusterManager clusterManager) {
		super(vertx, db, routerStorage, boot, searchProvider, utils, options, routerStorageRegistry, coordinator, writeLock, consistencyCheckHandler, cacheRegistry, clusterManager);
		this.contentCache = contentCache;
	}

	@Override
	public void handleCacheClear(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			HibUser user = ac.getUser();
			if (user != null && !user.isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}

			cacheRegistry.clear();
			contentCache.evictAll();
			vertx.eventBus().publish(CLEAR_CACHES.address, null);

			return message(ac, "cache_clear_invoked");
		}, model -> ac.send(model, OK));
	}

	@Override
	public void handleOpenAPIv3(InternalActionContext ac) {
		boolean useVersion31 = options.getDefaultOpenAPIVersion() == Version.V31;
		String format = options.getDefaultOpenAPIFormat().name().toLowerCase();

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

		/*
		 * Blacklist 
		 * a) all the actual project roots
		 * b) old api version roots
		 * c) an `apiversion` selector parameter root
		 * d) plugin paths
		 */
		Set<String> blacklistedRouteRegex = new HashSet<>(routerStorageRegistry.getInstances().stream()
				.flatMap(rr -> rr.root().apiRouter().projectsRouter().getProjectRouters().keySet().stream())
				.map(project -> "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/" + project + "[.]*").collect(Collectors.toSet()));
		blacklistedRouteRegex.addAll(IntStream.range(1, MeshVersion.CURRENT_API_VERSION).mapToObj(v -> "\\/api\\/v" + v + "[.]*").collect(Collectors.toList()));
		blacklistedRouteRegex.addAll(List.of("\\/api\\/\\{apiversion\\}[.]*", "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/eventbus\\/"));
		blacklistedRouteRegex.addAll(List.of("\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/\\{project\\}\\/plugins[.]*", "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/plugins[.]*"));

		// Make an instance with blacklist path patterns
		MeshOpenAPIv3Generator generator = new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), servers, Optional.of(blacklistedRouteRegex.stream().map(Pattern::compile).collect(Collectors.toList())), Optional.empty());

		// Generate...
		try {
			ac.send(generator.generate(
					"Gentics Mesh REST API",
					Stream.of(
							//... from base root
							routerStorageRegistry.getInstances().stream().map(rr -> Pair.of(rr.root().getRouter(), StringUtils.EMPTY)),
							//... from generic project root
							routerStorageRegistry.getInstances().stream().map(rr -> Pair.of(rr.root().apiRouter().projectsRouter().projectRouter().getRouter(), "/api/v" + MeshVersion.CURRENT_API_VERSION + "/{project}"))
						).flatMap(Function.identity()).collect(Collectors.toMap(Pair::getKey, Pair::getValue)), 
					// ...with desired format
					Format.parse(format),
					// ...with desired pretty printing
					!options.getHttpServerOptions().isMinifyJson(),
					// ...with desired spec version
					useVersion31),
				OK, 
				"yaml".equalsIgnoreCase(format) 
					? HttpConstants.APPLICATION_YAML_UTF8 
					: HttpConstants.APPLICATION_JSON_UTF8
			);
		} catch (OpenAPIGenerationException e) {
			ac.fail(error(INTERNAL_SERVER_ERROR, "error_internal", e));
		}
	}
}
