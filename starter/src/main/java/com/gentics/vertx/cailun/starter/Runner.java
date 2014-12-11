package com.gentics.vertx.cailun.starter;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.englishtown.vertx.jersey.JerseyVerticle;

public class Runner {
	private static final Vertx vertx = Vertx.vertx();

	public static void main(String[] args) throws IOException, InterruptedException {
		FileUtils.deleteDirectory(new File("/tmp/graphdb"));
		deployNeo4Vertx();
		Thread.sleep(7400);
		try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Neo4jConfig.class)) {
			ctx.start();
			deploySelf(ctx);
			MainBean bean = ctx.getBean(MainBean.class);
			bean.start();
			ctx.registerShutdownHook();
			System.in.read();
		}

		// deployArtifact();

	}

	private static void deployNeo4Vertx() throws IOException {

		InputStream is = Runner.class.getResourceAsStream("neo4vertx_gui.json");
		String jsonTxt = IOUtils.toString(is);
		JsonObject config = new JsonObject(jsonTxt);
		vertx.deployVerticle(Neo4jGraphVerticle.class.getCanonicalName(), new DeploymentOptions().setConfig(config));

	}

	private static void deploySelf(AnnotationConfigApplicationContext ctx) throws IOException {

		JerseyOptionsWithContextInfo.context = ctx;
		JsonObject config = new JsonObject();
		config.put("resources", new JsonArray().add("com.gentics.vertx.cailun"));
		config.put("hk2_binder" , "com.gentics.vertx.cailun.starter.AppBinder");
		config.put("port", 8000);

		DeploymentOptions options = new DeploymentOptions();
		// options.setIsolationGroup("A");
		options.setConfig(config);

		vertx.deployVerticle("java-hk2:" + JerseyVerticle.class.getCanonicalName(), options);
		// config.put("resources", new JsonArray().add("com.gentics.vertx.cailun.starter.resources2"));
		// vertx.deployVerticle("java-hk2:" + JerseyVerticle.class.getCanonicalName(), options);
		// System.in.read();
	}

	// private static void deployArtifact() {
	// JsonObject config = new JsonObject();
	// config.put("resources", new JsonArray().add("com.gentics.resources"));
	// config.put("port", 8000);
	//
	// DeploymentOptions options = new DeploymentOptions();
	// options.setConfig(config);
	//
	// vertx.deployVerticle("service:com.gentics.vertx:cailun-rest-page:0.1.0-SNAPSHOT", options);
	// }
}
