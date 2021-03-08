package com.gentics.mesh.rest.client.impl;

import static com.gentics.mesh.http.MeshHeaders.WEBROOT_NODE_UUID;
import static com.gentics.mesh.http.MeshHeaders.WEBROOT_RESPONSE_TYPE;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static com.gentics.mesh.http.HttpConstants.TEXT_PLAIN_UTF8;
import static com.gentics.mesh.http.HttpConstants.TEXT_HTML_UTF8;
import static com.gentics.mesh.rest.client.impl.Util.lazily;

import java.util.function.Supplier;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;

import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * {@link OkHttpClient} implementation of {@link MeshWebrootFieldResponse}.
 * 
 * @author plyhun
 *
 */
public class OkHttpWebrootFieldResponse implements MeshWebrootFieldResponse {

	private final Response response;

	private final Supplier<MeshBinaryResponse> binaryResponse;
	private final Supplier<String> jsonStringResponse;
	
	public OkHttpWebrootFieldResponse(Response response) {
		this.response = response;
		binaryResponse = lazily(() -> new OkHttpBinaryResponse(response));
		jsonStringResponse = lazily(() -> response.body().string());
	}

	@Override
	public boolean isBinary() {
		return "binary".equals(response.header(WEBROOT_RESPONSE_TYPE));
	}

	@Override
	public String getNodeUuid() {
		return response.header(WEBROOT_NODE_UUID);
	}

	@Override
	public MeshBinaryResponse getBinaryResponse() {
		return isBinary()
			? binaryResponse.get()
			: null;
	}

	@Override
	public String getFieldType() {
		return response.header(WEBROOT_RESPONSE_TYPE);
	}

	@Override
	public String getResponseAsJsonString() {
		if (isBinary()) {
			return null;
		}
		if (isPlainText()) {
			return JsonUtil.toJson(jsonStringResponse.get());
		}
		return jsonStringResponse.get();
	}

	@Override
	public boolean isPlainText() {
		return TEXT_PLAIN_UTF8.equals(response.header(CONTENT_TYPE.toString())) || TEXT_HTML_UTF8.equals(response.header(CONTENT_TYPE.toString()));
	}

	@Override
	public String getResponseAsPlainText() {
		if (isBinary() || !isPlainText()) {
			return null;
		}
		return jsonStringResponse.get();
	}	
}
