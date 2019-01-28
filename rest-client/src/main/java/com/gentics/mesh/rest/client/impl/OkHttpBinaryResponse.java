package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.MeshBinaryResponse;
import io.reactivex.Flowable;
import okhttp3.Response;

import java.io.InputStream;
import java.util.function.Supplier;

import static com.gentics.mesh.rest.client.impl.Util.lazily;

public class OkHttpBinaryResponse implements MeshBinaryResponse {
	private final Response response;
	private final Supplier<byte[]> bodyBytes;

	public OkHttpBinaryResponse(Response response) {
		this.response = response;

		bodyBytes = lazily(() -> response.body().bytes());
	}

	@Override
	public Flowable<byte[]> getFlowable() {
		// TODO IMPLEMENT non-blockingly
		throw new RuntimeException("Not implemented");
	}

	@Override
	public InputStream getStream() {
		return response.body().byteStream();
	}

	@Override
	public String getFilename() {
		String disposition = response.header("Content-Disposition");
		// TODO Use proper parser
		return disposition.substring(disposition.indexOf("=") + 1);
	}

	@Override
	public String getContentType() {
		return response.body().contentType().toString();
	}

	@Override
	public void close() {
		response.close();
	}
}
