package com.gentics.mesh.test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.graph.neo4j.Neo4VertxConfiguration;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.File;
import java.io.IOException;

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
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.mesh.auth.Neo4jAuthorizingRealm;
import com.gentics.mesh.etc.neo4j.UUIDTransactionEventHandler;

@Configuration
@EnableNeo4jRepositories("com.gentics.mesh")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.mesh" })
@EnableAspectJAutoProxy
public class SpringTestConfiguration extends Neo4jConfiguration {

	public SpringTestConfiguration() {
		setBasePackage("com.gentics.mesh");
	}

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

		graphService.registerTransactionEventHandler(new UUIDTransactionEventHandler(graphService));
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
	public Neo4jAuthorizingRealm customSecurityRealm() {
		Neo4jAuthorizingRealm realm = new Neo4jAuthorizingRealm();
		realm.setCacheManager(new MemoryConstrainedCacheManager());
		// Disable caching for testing
		realm.setAuthenticationCachingEnabled(false);
		realm.setCachingEnabled(false);
		return realm;
	}

	@Bean
	public Vertx vertx() {
		VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckPeriod(1000*60*60);
		return Vertx.vertx(options);
	}

}
