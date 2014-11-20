package com.gentics.vertx.cailun.starter;

import java.io.IOException;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import com.englishtown.vertx.jersey.JerseyVerticle;

public class Runner {
	private static final Vertx vertx = Vertx.vertx();

	public static void main(String[] args) throws IOException {
//		vertx.registerVerticleFactory(new HK2VerticleFactory());
//		vertx.registerVerticleFactory(new HK2MavenVerticleFactory());

//		deploySelf();
		deployArtifact();

	}

	private static void deploySelf() throws IOException {
		JsonObject config = new JsonObject();
		config.put("resources", new JsonArray().add("com.gentics.vertx.cailun.starter.resources"));
		config.put("port", 8000);

		DeploymentOptions options = new DeploymentOptions();
//		options.setIsolationGroup("A");
		options.setConfig(config);

		vertx.deployVerticle("java-hk2:" + JerseyVerticle.class.getCanonicalName(), options);
		System.in.read();
		config.put("resources", new JsonArray().add("com.gentics.vertx.cailun.starter.resources2"));
		
		vertx.deployVerticle("java-hk2:" + JerseyVerticle.class.getCanonicalName(), options);
		System.in.read();
	}

	private static void deployArtifact() {
		JsonObject config = new JsonObject();
		config.put("resources", new JsonArray().add("com.gentics.resources"));
		config.put("port", 8000);

		DeploymentOptions options = new DeploymentOptions();
		options.setConfig(config);

		vertx.deployVerticle("service:com.gentics.vertx:cailun-rest-page:0.1.0-SNAPSHOT", options);
	}
}
