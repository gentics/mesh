package com.gentics.mesh.dagger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.hibernate.util.UuidGenerator;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.util.MeshOpenAPIv3Generator;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger 2 module for providing various utility bindings.
 * 
 * @author plyhun
 *
 */
@Module
public class UtilModule {
	@Provides
	@Singleton
	public UuidGenerator uuidGenerator() {
		return new UuidGenerator(new Random());
	}


	@Provides
	@Singleton
	public static MeshOpenAPIv3Generator provideOpenAPIv3Generator(MeshOptions options, ClusterManager clusterManager, RouterStorageRegistry routerStorageRegistry) {
		// Collect available servers
		HttpServerConfig httpServerConfig = options.getHttpServerOptions();	
		Supplier<List<String>> noClusterServerSupplier = () -> Collections.singletonList((httpServerConfig.isSsl() ? "https://" : "http://") + httpServerConfig.getHost() + ":" + (httpServerConfig.isSsl() ? httpServerConfig.getSslPort() : httpServerConfig.getPort()));
		List<String> servers;
		try {
			servers = Optional.ofNullable(clusterManager.getHazelcast())
					.map(hz -> hz.getCluster().getMembers().stream().map(m -> m.getAddress().getHost() + ":" + m.getAddress().getPort()).collect(Collectors.toList()))
					.orElseGet(noClusterServerSupplier);
		} catch (Throwable e) {
			Logger log = LoggerFactory.getLogger(AdminEndpointProviderModule.class);
			log.error("Could not retrieve the server list out of Hazelcast", e);
			servers = noClusterServerSupplier.get();
		}

		/*
		 * Blacklist 
		 * a) all the actual project roots
		 * b) old api version roots
		 * c) an `apiversion` selector parameter root
		 * d) plugins root, if forbidden
		 * e) eventbus
		 */
		Set<String> blacklistedRouteRegex = new HashSet<>(routerStorageRegistry.getInstances().stream()
				.flatMap(rr -> rr.root().apiRouter().projectsRouter().getProjectRouters().keySet().stream())
				.map(project -> "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/" + project + "[.]*").collect(Collectors.toSet()));
		blacklistedRouteRegex.addAll(IntStream.range(1, MeshVersion.CURRENT_API_VERSION).mapToObj(v -> "\\/api\\/v" + v + "[.]*").collect(Collectors.toList()));
		blacklistedRouteRegex.addAll(List.of("\\/api\\/\\{apiversion\\}[.]*", "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/eventbus\\/"));
		if (options.getOpenAPIOptions().isExcludePlugins()) {
			blacklistedRouteRegex.addAll(List.of("\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/plugins[.]*", "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/[a-zA-Z0-9\\-\\._@$%!]+\\/plugins[.]*", "\\/api\\/v" + MeshVersion.CURRENT_API_VERSION + "\\/\\{project\\}\\/plugins[.]*"));
		}

		// Make an instance with blacklist path patterns
		return new MeshOpenAPIv3Generator(MeshVersion.getPlainVersion(), servers, Optional.of(blacklistedRouteRegex.stream().map(Pattern::compile).collect(Collectors.toList())), Optional.empty());
	}
}
