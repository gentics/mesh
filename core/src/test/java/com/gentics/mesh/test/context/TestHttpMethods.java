package com.gentics.mesh.test.context;

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
public interface TestHttpMethods extends TestHelperMethods {

	OkHttpClient httpClient();
	
	default HttpUrl prepareUrl(@NotNull String path, ParameterProvider... params) {
		HttpUrl.Builder url = new HttpUrl.Builder();
		url.scheme("http");
		url.host("localhost");
		url.port(port());
		url.encodedPath(path);
		
		if (params != null) {
			for (ParameterProvider param : params) {
				for (Entry<String, String> entry : param.getParameters()) {
					url.addQueryParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		
		return url.build();
	}
	
	default Call httpGet(@NotNull String path, ParameterProvider... params) {
		HttpUrl url = this.prepareUrl(path, params);
		
		Request.Builder b = new Request.Builder();
		b.url(url);
		b.method("GET", null);
		
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
