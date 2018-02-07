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
	
	default Request.Builder httpRequest(@NotNull String path, ParameterProvider... params) {
		HttpUrl.Builder url = new HttpUrl.Builder();
		url.scheme("http");
		url.host("localhost");
		url.port(this.port());
		url.encodedPath(path);
		
		if (params != null) {
			for (ParameterProvider param : params) {
				for (Entry<String, String> entry : param.getParameters()) {
					url.addQueryParameter(entry.getKey(), entry.getValue());
				}
			}
		}
		
		Request.Builder b = new Request.Builder();
		b.url(url.build());
		
		return b;
	}
	
	default Call httpGet(@NotNull String path, ParameterProvider... params) {
		Request.Builder b = this.httpRequest(path, params);
		b.method("GET", null);
		
		return this.httpClient().newCall(b.build());
	}
	
	default Call httpPost(@NotNull String path, RequestBody body, ParameterProvider... params) {	
		Request.Builder b = this.httpRequest(path, params);
		b.method("POST", body);
		
		return this.httpClient().newCall(b.build());
	}
	
	default Call httpPost(@NotNull String path, String body) {
		return this.httpPost(path, RequestBody.create(MediaType.parse("application/json"), body));
	}
	
	default Call httpPost(@NotNull String path, JsonObject json) {
		return this.httpPost(path, RequestBody.create(MediaType.parse("application/json"), json.encode()));
	}
	
	default Call httpDelete(@NotNull String path, ParameterProvider... params) {
		Request.Builder b = this.httpRequest(path, params);
		b.method("DELETE", null);
		
		return this.httpClient().newCall(b.build());
	}
}
