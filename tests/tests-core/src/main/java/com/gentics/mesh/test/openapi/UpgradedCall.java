package com.gentics.mesh.test.openapi;

import java.io.IOException;

import kotlin.jvm.functions.Function0;
import kotlin.reflect.KClass;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okio.Timeout;

/**
 * Lazily build {@link Call}
 */
public class UpgradedCall implements Call {

	private final Request.Builder builder;
	private final OkHttpClient client;
	private Call call = null;
	
	public UpgradedCall(Builder builder, OkHttpClient client) {
		this.builder = builder;
		this.client = client;
	}

	@Override
	public void cancel() {
		lazy().cancel();
	}

	@Override
	public void enqueue(Callback arg0) {
		lazy().enqueue(arg0);
	}

	@Override
	public Response execute() throws IOException {
		return lazy().execute();
	}

	@Override
	public boolean isCanceled() {
		return lazy().isCanceled();
	}

	@Override
	public boolean isExecuted() {
		return lazy().isExecuted();
	}

	@Override
	public Request request() {
		return lazy().request();
	}

	@Override
	public <T> T tag(KClass<T> arg0) {
		return lazy().tag(arg0);
	}

	@Override
	public <T> T tag(Class<? extends T> arg0) {
		return lazy().tag(arg0);
	}

	@Override
	public <T> T tag(KClass<T> arg0, Function0<? extends T> arg1) {
		return lazy().tag(arg0, arg1);
	}

	@Override
	public <T> T tag(Class<T> arg0, Function0<? extends T> arg1) {
		return lazy().tag(arg0, arg1);
	}

	@Override
	public Timeout timeout() {
		return lazy().timeout();
	}

	@Override
	public UpgradedCall clone() {
		return new UpgradedCall(builder, client);
	}

	public Request.Builder getBuilder() {
		return builder;
	}

	public Request build() {
		return lazy().request();
	}

	private Call lazy() {
		if (call == null) {
			call = client.newCall(builder.build());
		} 
		return call;
	}
}
