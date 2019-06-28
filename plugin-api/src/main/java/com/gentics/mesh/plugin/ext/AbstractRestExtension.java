package com.gentics.mesh.plugin.ext;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractRestExtension implements RestExtension {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestExtension.class);

	private MeshRestClient adminClient;

	/**
	 * Return a wrapped routing context.
	 * 
	 * @param rc
	 *            Vert.x routing context
	 * @return Wrapped context
	 */
	public PluginContext wrap(RoutingContext rc) {
		return new PluginContext(rc);
	}

	@Override
	public MeshRestClient adminClient() {
		String token = manager.adminToken();
		adminClient.setAPIKey(token);
		return adminClient;
	}

	/**
	 * Return a wrapped routing context handler
	 * 
	 * @param handler
	 *            Handler to be wrapped
	 * @return Wrapped handler
	 */
	public Handler<RoutingContext> wrapHandler(Handler<PluginContext> handler) {
		return rc -> handler.handle(wrap(rc));
	}

	protected void createAdminClient() {
		MeshOptions options = Mesh.mesh().getOptions();
		int port = options.getHttpServerOptions().getPort();
		String host = options.getHttpServerOptions().getHost();
		adminClient = MeshRestClient.create(host, port, false);
	}

	@Override
	public void start() {
		log.info("Starting REST extension {" + getName() + "}");
	}

	@Override
	public void stop() {
		log.info("Stoppign REST extension {" + getName() + "}");

	}

}
