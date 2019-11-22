package com.gentics.mesh.plugin.pf4j;

import javax.inject.Inject;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.Lazy;
import io.vertx.core.Vertx;

public class PluginEnvironmentImpl implements PluginEnvironment {

	private final Database db;

	private final Lazy<Vertx> vertx;

	private final MeshOptions options;

	private Lazy<MeshJWTAuthProvider> authProvider;

	private Lazy<BootstrapInitializer> boot;

	private Lazy<MeshLocalClientImpl> localClient;

	private static final String WILDCARD_IP = "0.0.0.0";

	private static final String LOOPBACK_IP = "127.0.0.1";

	@Inject
	public PluginEnvironmentImpl(Lazy<BootstrapInitializer> boot, Database db, Lazy<MeshJWTAuthProvider> authProvider, Lazy<Vertx> vertx, MeshOptions options, Lazy<MeshLocalClientImpl> localClient) {
		this.boot = boot;
		this.db = db;
		this.authProvider = authProvider;
		this.vertx = vertx;
		this.options = options;
		this.localClient = localClient;
	}

	@Override
	public String adminToken() {
		return db.tx(() -> {
			User admin = boot.get().userRoot().findByUsername("admin");
			// TODO: Use dedicated tokenCode - See https://github.com/gentics/mesh/issues/412
			return authProvider.get().generateAPIToken(admin, null, null);
		});
	}

	@Override
	public Vertx vertx() {
		return vertx.get();
	}

	@Override
	public MeshRestClient createAdminClient() {
		int port = options.getHttpServerOptions().getPort();
		String host = determineHostString(options);
		MeshRestClient client = MeshRestClient.create(host, port, false);
		client.setAPIKey(adminToken());
		return client;
	}

	@Override
	public MeshRestClient createLocalClient() {
		MeshLocalClientImpl client = localClient.get();
		// TODO add null check
		MeshAuthUser admin = db.tx(() -> boot.get().userRoot().findByUsername("admin").toAuthUser());
		client.setUser(admin);
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
