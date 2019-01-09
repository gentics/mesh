package com.gentics.mesh.test.remote;

import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestServer;

import io.vertx.core.Vertx;

public class MeshRemoteServer extends TestWatcher implements MeshTestServer {

	private MeshRestClient client;

	private String hostname;

	private int port;

	private Vertx vertx;

	public MeshRemoteServer(Vertx vertx, String hostname, int port) {
		this.vertx = vertx;
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	protected void starting(Description description) {
		client = MeshRestClient.create(getHostname(), getPort(), false, vertx);
		client.setLogin("admin", "admin");
		client.login().blockingGet();
	}

	@Override
	protected void finished(Description description) {
		client.close();
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public String getHostname() {
		return hostname;
	}

	@Override
	public MeshRestClient client() {
		return client;
	}

}
