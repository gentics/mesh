package com.gentics.mesh.plugin.pf4j;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import com.gentics.mesh.RestAPIVersion;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;

import dagger.Lazy;
import io.vertx.core.Vertx;
import okhttp3.OkHttpClient;

/**
 * @see PluginEnvironment
 */
public class PluginEnvironmentImpl implements PluginEnvironment {

	private final Database db;

	private final Lazy<Vertx> vertx;

	private final AbstractMeshOptions options;

	private Lazy<MeshJWTAuthProvider> authProvider;

	private Lazy<BootstrapInitializer> boot;

	private final OkHttpClient pluginOkHttpClient;

	private static final String WILDCARD_IP = "0.0.0.0";

	private static final String LOOPBACK_IP = "127.0.0.1";

	@Inject
	public PluginEnvironmentImpl(Lazy<BootstrapInitializer> boot, Database db, Lazy<MeshJWTAuthProvider> authProvider, Lazy<Vertx> vertx,
		AbstractMeshOptions options, @Named("pluginClient") OkHttpClient pluginOkHttpClient) {
		this.boot = boot;
		this.db = db;
		this.authProvider = authProvider;
		this.vertx = vertx;
		this.options = options;
		this.pluginOkHttpClient = pluginOkHttpClient;
	}

	@Override
	public String adminToken() {
		return db.tx(tx -> {
			HibUser admin = tx.userDao().findByUsername("admin");
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
		return createAdminClient(RestAPIVersion.V1);
	}

	@Override
	public MeshRestClient createAdminClient(RestAPIVersion version) {
		Objects.requireNonNull(version);
		int port = options.getHttpServerOptions().getPort();
		String host = determineHostString(options);
		MeshRestClient client = MeshRestClient.create(MeshRestClientConfig.newConfig()
			.setPort(port)
			.setHost(host)
			.setBasePath(version.getBasePath())
			.build(), pluginOkHttpClient);

		client.setAPIKey(adminToken());
		return client;
	}

	@Override
	public MeshRestClient createClient(String token) {
		int port = options.getHttpServerOptions().getPort();
		String host = options.getHttpServerOptions().getHost();

		MeshRestClient client = MeshRestClient.create(new MeshRestClientConfig.Builder()
			.setHost(host)
			.setPort(port)
			.setSsl(false)
			.build(), pluginOkHttpClient);

		// Set the token to the client if it was specified.
		if (token != null) {
			client.setAPIKey(token);
		}
		return client;
	}

	@Override
	public AbstractMeshOptions options() {
		return options;
	}

	private String determineHostString(AbstractMeshOptions options) {
		String host = options.getHttpServerOptions().getHost();
		return WILDCARD_IP.equals(host) ? LOOPBACK_IP : host;
	}

}
