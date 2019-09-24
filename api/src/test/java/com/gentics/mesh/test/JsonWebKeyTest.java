package com.gentics.mesh.test;

import org.junit.Test;

import com.gentics.mesh.etc.config.auth.JsonWebKey;

import io.vertx.core.json.JsonObject;

public class JsonWebKeyTest {

	@Test
	public void testKeyHandling() {
		JsonWebKey key = new JsonWebKey();
		key.setAlgorithm("RS256").setType("RSA").setPublicKeyUse("sig");

		JsonObject json = key.toJson();

		System.out.println(json.encodePrettily());
	}
}
