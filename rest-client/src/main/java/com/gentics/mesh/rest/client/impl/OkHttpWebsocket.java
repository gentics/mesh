package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.EventbusEvent;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebsocket;
import io.reactivex.Observable;
import okhttp3.OkHttpClient;

public class OkHttpWebsocket implements MeshWebsocket {
	private final OkHttpClient client;
	private final MeshRestClientConfig config;

	public OkHttpWebsocket(OkHttpClient client, MeshRestClientConfig config) {
		this.client = client;
		this.config = config;
	}

	@Override
	public void close() {

	}

	@Override
	public void publishEvent(String eventName, Object body) {

	}

	@Override
	public void registerEvents(String... eventNames) {

	}

	@Override
	public void unregisterEvents(String... eventNames) {

	}

	@Override
	public Observable<EventbusEvent> events() {
		return null;
	}

	@Override
	public Observable<Object> connections() {
		return null;
	}

	@Override
	public Observable<Throwable> errors() {
		return null;
	}
}
