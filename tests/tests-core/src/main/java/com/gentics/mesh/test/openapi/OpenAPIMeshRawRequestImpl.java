package com.gentics.mesh.test.openapi;

import com.gentics.mesh.rest.client.impl.AbstractMeshOkHttpRequest;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OpenAPIMeshRawRequestImpl<T, R> extends AbstractMeshOkHttpRequest<R> {

	private final UpgradedCall call;

	public OpenAPIMeshRawRequestImpl(OkHttpClient client, UpgradedCall call, Class<? extends R> targetType) {
		super(client, targetType);
		this.call = call;
	}

	@Override
	public void setHeader(String name, String value) {
		call.getBuilder().header(name, value);
	}

	@Override
	protected Request createRequest() {
		return call.build();
	}

	@Override
	protected Call createCall() {
		return call;
	}
}
