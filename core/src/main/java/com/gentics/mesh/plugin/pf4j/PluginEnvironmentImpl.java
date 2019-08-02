package com.gentics.mesh.plugin.pf4j;

import javax.inject.Inject;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.Lazy;
import io.vertx.core.Vertx;

public class PluginEnvironmentImpl implements PluginEnvironment {

	private final Lazy<Vertx> vertx;

	private final MeshOptions options;

	private static final String WILDCARD_IP = "0.0.0.0";

	private static final String LOOPBACK_IP = "127.0.0.1";

	@Inject
	public PluginEnvironmentImpl(Lazy<Vertx> vertx, MeshOptions options) {
		this.vertx = vertx;
		this.options = options;
	}

	@Override
	public String adminToken() {
		MeshComponent mesh = MeshInternal.get();
		MeshJWTAuthProvider authProvider = mesh.authProvider();
		Database db = mesh.database();

		return db.tx(() -> {
			User admin = mesh.boot().userRoot().findByUsername("admin");
			// TODO: Use dedicated tokenCode - See https://github.com/gentics/mesh/issues/412
			return authProvider.generateAPIToken(admin, null, null);
		});
	}

	@Override
	public Vertx vertx() {
		return vertx.get();
	}

	@Override
	public MeshRestClient createAdminClient() {
		MeshOptions options = Mesh.mesh().getOptions();
		int port = options.getHttpServerOptions().getPort();
		String host = determineHostString(options);
		MeshRestClient client = MeshRestClient.create(host, port, false);
		client.setAPIKey(adminToken());
		return client;
	}

	@Override
	public MeshOptions options() {
		return options;
	}

	private String determineHostString(MeshOptions options) {
		String host = options.getHttpServerOptions().getHost();
		return WILDCARD_IP.equals(host) ? LOOPBACK_IP : host;
	}

}
