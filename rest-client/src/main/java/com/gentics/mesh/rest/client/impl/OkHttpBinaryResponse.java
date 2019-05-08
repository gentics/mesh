package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.MeshBinaryResponse;
import okhttp3.Response;

import java.io.InputStream;

public class OkHttpBinaryResponse implements MeshBinaryResponse {
	private final Response response;

	public OkHttpBinaryResponse(Response response) {
		this.response = response;
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
