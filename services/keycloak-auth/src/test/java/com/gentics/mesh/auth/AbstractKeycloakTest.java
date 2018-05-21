package com.gentics.mesh.auth;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

public abstract class AbstractKeycloakTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractKeycloakTest.class);

	protected OkHttpClient client() {
		Builder builder = new OkHttpClient.Builder();
		return builder.build();
	}

	protected JsonObject get(String path, JsonObject token) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + token.getString("access_token"))
			.url("http://localhost:8080" + path)
			.build();

		Response response = client().newCall(request).execute();
		return new JsonObject(response.body().string());
	}

	protected String get(String path) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url("http://localhost:8080" + path)
			.build();

		Response response = client().newCall(request).execute();
		System.out.println("Response: " + response.code());
		return response.body().string();
	}

	protected JsonObject loadJson(String path) throws IOException {
		return new JsonObject(IOUtils.toString(getClass().getResource(path), Charset.defaultCharset()));
	}
}
