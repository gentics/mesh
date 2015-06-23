package com.gentics.mesh.test;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshConfiguration;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
@EnableAspectJAutoProxy
public class SpringTestConfiguration {

//	@Bean
//	public GraphDatabaseService graphDatabaseService() {
//		final File storeDir = new File(System.getProperty("java.io.tmpdir"), "random_db_" + TestUtil.getRandomHash(12));
//		storeDir.mkdirs();
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			@Override
//			public void run() {
//				try {
//					FileUtils.deleteDirectory(storeDir);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		});
//
//		GraphDatabaseBuilder builder = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder(storeDir.getAbsolutePath());
//		GraphDatabaseService graphService = builder.newGraphDatabase();
//
//		ServerConfigurator webConfig = new ServerConfigurator((GraphDatabaseAPI) graphService);
//		WrappingNeoServerBootstrapper bootStrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) graphService, webConfig);
//		bootStrapper.start();
//
//		return graphService;
//	}

//	@Bean
//	public GraphBackedAuthorizingRealm customSecurityRealm() {
//		GraphBackedAuthorizingRealm realm = new GraphBackedAuthorizingRealm();
//		realm.setCacheManager(new MemoryConstrainedCacheManager());
//		// Disable caching for testing
//		realm.setAuthenticationCachingEnabled(false);
//		realm.setCachingEnabled(false);
//		return realm;
//	}

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
