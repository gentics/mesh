package com.gentics.mesh.plugin.mustread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.json.JsonObject;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient.Builder;

public interface MustReadPluginTestHelper {

	MeshRestClient meshClient();

	public static final String API_NAME = "mustread";

	String serverURL();

	default OkHttpClient client() {
		Builder builder = new Builder();
		CookieJar cookieJar = new CookieJar() {
			private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

			@Override
			public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

			}

			@Override
			public List<Cookie> loadForRequest(HttpUrl url) {
				ArrayList<Cookie> cookies = new ArrayList<>();
				cookies.add(new Cookie.Builder()
					.domain(url.host())
					.path("/")
					.name("mesh.token")
					.value(meshClient().getAuthentication().getToken())
					.httpOnly()
					.secure()
					.build());
				return cookies;

			}
		};
		builder.cookieJar(cookieJar);
		return builder.build();
	}

	default  String getResponseTextGetRequest(String path) throws IOException {
		return getResponseGetRequest(path).body().string();
	}

	default  int getResponseCodeGetRequest(String path) throws IOException {
		return getResponseGetRequest(path).code();
	}

	default  Response getResponseGetRequest(String path) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(serverURL() + path)
			.build();

		Response response = client().newCall(request).execute();
		return response;
	}

	default  String getResponseTextPostRequest(String path, JsonObject body) throws IOException {
		return getResponsePostRequest(path, body).body().string();
	}

	default  int getResponseCodePostRequest(String path, JsonObject body) throws IOException {
		return getResponsePostRequest(path, body).code();
	}

	default  Response getResponsePostRequest(String path, JsonObject body) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url(serverURL() + path)
			.post(RequestBody.create(MediaType.get("application/json"), body.toString()))
			.build();

		Response response = client().newCall(request).execute();
		return response;
	}

	default  JsonObject getResponseJsonGetRequest(String path) throws IOException {
		String jsonString = getResponseTextGetRequest(path);
		return new JsonObject(jsonString);
	}
}
