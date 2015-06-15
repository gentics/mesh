package com.gentics.mesh.test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.graph.neo4j.Neo4VertxConfiguration;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.neo4j.backup.OnlineBackupSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.openpcf.neo4vertx.neo4j.service.GraphService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.gentics.mesh.auth.GraphBackedAuthorizingRealm;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshConfiguration;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
@EnableAspectJAutoProxy
public class SpringTestConfiguration {

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		final File storeDir = new File(System.getProperty("java.io.tmpdir"), "random_neo4jdb_" + TestUtil.getRandomHash(12));
		storeDir.mkdirs();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FileUtils.deleteDirectory(storeDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		GraphDatabaseBuilder builder = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder(storeDir.getAbsolutePath());
		builder.setConfig(OnlineBackupSettings.online_backup_enabled, "false");
		GraphDatabaseService graphService = builder.newGraphDatabase();

		Neo4jGraphVerticle.setService(new GraphService() {

			@Override
			public void initialize(Neo4VertxConfiguration configuration) throws Exception {
			}

			@Override
			public GraphDatabaseService getGraphDatabaseService() {
				return graphService;
			}

			@Override
			public JsonObject query(JsonObject request) throws Exception {
				return null;
			}

			@Override
			public void shutdown() {
			}

		});

		return graphService;
	}

	@Bean
	public GraphBackedAuthorizingRealm customSecurityRealm() {
		GraphBackedAuthorizingRealm realm = new GraphBackedAuthorizingRealm();
		realm.setCacheManager(new MemoryConstrainedCacheManager());
		// Disable caching for testing
		realm.setAuthenticationCachingEnabled(false);
		realm.setCachingEnabled(false);
		return realm;
	}

	@Bean
	public Vertx vertx() {
		VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckPeriod(1000 * 60 * 60);
		return Vertx.vertx(options);
	}

	@PostConstruct
	public void setup() {
		MeshSpringConfiguration.setConfiguration(new MeshConfiguration());
	}

}
