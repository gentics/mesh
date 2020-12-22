package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.node.NodeResponse;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import okhttp3.Response;

import java.util.function.Supplier;

import static com.gentics.mesh.http.MeshHeaders.WEBROOT_RESPONSE_TYPE;
import static com.gentics.mesh.http.MeshHeaders.WEBROOT_NODE_UUID;
import static com.gentics.mesh.rest.client.impl.Util.lazily;

/**
 * OkHttp specific webroot response implementation.
 * 
 * @see MeshWebrootResponse
 */
public class OkHttpWebrootResponse implements MeshWebrootResponse {

	private final Response response;

	private final Supplier<MeshBinaryResponse> binaryResponse;
	private final Supplier<NodeResponse> nodeResponse;

	public OkHttpWebrootResponse(Response response) {
		this.response = response;
		binaryResponse = lazily(() -> new OkHttpBinaryResponse(response));
		nodeResponse = lazily(() -> JsonUtil.readValue(response.body().string(), NodeResponse.class));
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
	public NodeResponse getNodeResponse() {
		return isBinary()
			? null
			: nodeResponse.get();
	}
}
