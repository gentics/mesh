package com.gentics.mesh.rest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;

public class MeshRestClient {

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	private HttpClient client;

	public MeshRestClient(String host) {
		this(host, DEFAULT_PORT);
	}

	public MeshRestClient(String host, int port) {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost(host);
		options.setDefaultPort(port);
		client = Vertx.vertx().createHttpClient(options);
	}

	public Future<UserResponse> login(String username, String password) {
		Future<UserResponse> future = Future.future();
		String authStringEnc = username + ":" + password;
		byte[] authEncBytes = Base64.encodeBase64(authStringEnc.getBytes());

		HttpClientRequest request = client.get(BASEURI + "/auth/me", rh -> {
			rh.bodyHandler(bh -> {
				String json = bh.toString();
				try {
					UserResponse response = JsonUtil.readValue(json, UserResponse.class);
					future.complete(response);
				} catch (Exception e) {
					future.fail(e);
				}
			});
		});
		request.headers().add("Authorization", "Basic " + new String(authEncBytes));
		request.headers().add("Accept", "application/json");
		request.end();
		return future;
	}

	public Future<TagResponse> createTag(TagCreateRequest tagCreateRequest) {
		Future<TagResponse> future = Future.future();

		Map<String, String> extraHeaders = new HashMap<>();
		Buffer buffer = Buffer.buffer();
		buffer.appendString(JsonUtil.toJson(tagCreateRequest));
		extraHeaders.put("content-length", String.valueOf(buffer.length()));
		extraHeaders.put("content-type", "application/json");

		HttpClientRequest request = client.post(BASEURI + "/project/tags", rh -> {
			rh.bodyHandler(bh -> {
				if (rh.statusCode() == 200) {
					String json = bh.toString();
					try {
						TagResponse tagResponse = JsonUtil.readValue(json, TagResponse.class);
						future.complete(tagResponse);
					} catch (Exception e) {
						future.fail(e);
					}
				} else {
					future.fail("Could not fetch tag:" + rh.statusCode());
				}
			});
		});

		return future;
	}

	public Future<TagResponse> findTag(String uuid) {
		Future<TagResponse> future = Future.future();
		HttpClientRequest request = client.get(BASEURI + "/tags", rh -> {
			rh.bodyHandler(bh -> {

			});
			System.out.println("Received response with status code " + rh.statusCode());
		});

		request.exceptionHandler(e -> {
			System.out.println("Received exception: " + e.getMessage());
			e.printStackTrace();
		});
		return future;
	}
}
