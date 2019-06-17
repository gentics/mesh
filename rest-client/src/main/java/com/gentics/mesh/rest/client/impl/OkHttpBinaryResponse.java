package com.gentics.mesh.rest.client.impl;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.gentics.mesh.rest.client.MeshBinaryResponse;

import okhttp3.Response;

public class OkHttpBinaryResponse implements MeshBinaryResponse {
	private final Response response;

	public static final String FILENAME_DISPOSITION_ATTR = "filename*=utf-8''";

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
		try {
			int start = disposition.indexOf(FILENAME_DISPOSITION_ATTR);
			String encodedName = disposition.substring(start + FILENAME_DISPOSITION_ATTR.length());
			return URLDecoder.decode(encodedName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
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
