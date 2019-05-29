package com.gentics.mesh.test.context;

import java.io.IOException;
import java.util.Map.Entry;

import javax.validation.constraints.NotNull;

import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.json.JsonObject;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Interface for HTTP-Methods in Test-Classes to test requests/responses which are not covered/allowed via the API.
 */
public interface TestHttpMethods extends TestHelper {

	OkHttpClient httpClient();

	default HttpUrl prepareUrl(@NotNull String path, ParameterProvider... params) {
		HttpUrl.Builder url = new HttpUrl.Builder();
		url.scheme("http");
		url.host("localhost");
		url.port(port());
		url.encodedPath(path);

		if (params != null) {
			for (ParameterProvider param : params) {
				for (Entry<String, String> entry : param.getParameters().entrySet()) {
					url.addQueryParameter(entry.getKey(), entry.getValue());
				}
			}
		}

		return url.build();
	}

	default String httpGetNow(String path, ParameterProvider... params) throws IOException {
		return httpGet(path, params).execute().body().string();
	}

	default String httpGetNow(String path, String token, ParameterProvider... params) throws IOException {
		return httpGet(path, token, params).execute().body().string();
	}

	default JsonObject httpGetNowJson(String path, String token, ParameterProvider... params) throws IOException {
		return new JsonObject(httpGetNow(path, token, params));
	}

	default Call httpGet(@NotNull String path, ParameterProvider... params) {
		return httpGet(path, null, params);
	}

	default Call httpGet(@NotNull String path, String token, ParameterProvider... params) {
		HttpUrl url = this.prepareUrl(path, params);

		Request.Builder b = new Request.Builder();
		b.url(url);
		b.method("GET", null);
		if (token != null) {
			b.addHeader("Cookie", "mesh.token=" + token);
		}

		return this.httpClient().newCall(b.build());
	}

	default Call httpPost(@NotNull String path, RequestBody body, ParameterProvider... params) {
		HttpUrl url = this.prepareUrl(path, params);

		Request.Builder b = new Request.Builder();
		b.url(url);
		b.method("POST", body);

		return this.httpClient().newCall(b.build());
	}

	default Call httpPost(@NotNull String path, String body, ParameterProvider... params) {
		return this.httpPost(path, RequestBody.create(MediaType.parse("application/json"), body), params);
	}

	default Call httpPost(@NotNull String path, JsonObject json, ParameterProvider... params) {
		return this.httpPost(path, RequestBody.create(MediaType.parse("application/json"), json.encode()), params);
	}

	default Call httpDelete(@NotNull String path, ParameterProvider... params) {
		HttpUrl url = this.prepareUrl(path, params);

		Request.Builder b = new Request.Builder();
		b.url(url);
		b.method("DELETE", null);

		return this.httpClient().newCall(b.build());
	}
}
