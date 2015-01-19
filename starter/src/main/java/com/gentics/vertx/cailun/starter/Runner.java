package com.gentics.vertx.cailun.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jacpfx.vertx.spring.SpringVerticleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.gentics.vertx.cailun.demo.CustomerVerticle;
import com.gentics.vertx.cailun.page.PageVerticle;
import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;

public class Runner {
	private static final Vertx vertx = Vertx.vertx();

	private static final transient Logger log = LoggerFactory.getLogger(Runner.class);

	public static void main(String[] args) throws IOException, InterruptedException {

		// For testing - We cleanup all the data. The customer module contains a class that will setup a fresh graph each startup.
		FileUtils.deleteDirectory(new File("/tmp/graphdb"));

		deployNeo4Vertx();
		Thread.sleep(10400);
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Neo4jSpringConfiguration.class)) {
			SpringVerticleFactory.setParentContext(ctx);
			ctx.start();
			deployAndWait(CustomerVerticle.class);
			startUp(PageVerticle.class);
			deploySelf(ctx);
			ctx.registerShutdownHook();
			System.in.read();
		}
	}

	private static void deployAndWait(Class<? extends AbstractCailunRestVerticle> clazz) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		String prefix = SpringVerticleFactory.PREFIX + ":";
		vertx.deployVerticle(prefix + clazz.getCanonicalName(), handler -> {
			System.out.println("ID:" + handler.result());
			latch.countDown();
		});
		latch.await();
	}

	private static void startUp(Class<? extends AbstractVerticle> clazz) {

		String prefix = SpringVerticleFactory.PREFIX + ":";
		vertx.deployVerticle(prefix + clazz.getCanonicalName(), handler -> {
			log.info("Started verticle " + handler.result());
		});
	}

	/**
	 * Deploy the neo4vertx extension using the given json configuration.
	 * 
	 * @throws IOException
	 */
	private static void deployNeo4Vertx() throws IOException {

		InputStream is = Runner.class.getResourceAsStream("neo4vertx_gui.json");
		String jsonTxt = IOUtils.toString(is);
		JsonObject config = new JsonObject(jsonTxt);
		vertx.deployVerticle(Neo4jGraphVerticle.class.getCanonicalName(), new DeploymentOptions().setConfig(config));

	}

	/**
	 * Deploy the main jersey verticle that will startup jersey and the http server.
	 * 
	 * @param ctx
	 * @throws IOException
	 */
	private static void deploySelf(AnnotationConfigApplicationContext ctx) throws IOException {

		JsonObject config = new JsonObject();
		config.put("resources", new JsonArray().add("com.gentics.vertx.cailun"));
		config.put("port", 8000);

		DeploymentOptions options = new DeploymentOptions();
		options.setConfig(config);

	}

}
