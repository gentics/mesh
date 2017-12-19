package com.gentics.mesh.rest;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.core.verticle.admin.AdminEndpoint;
import com.gentics.mesh.core.verticle.admin.RestInfoEndpoint;
import com.gentics.mesh.core.verticle.auth.AuthenticationEndpoint;
import com.gentics.mesh.core.verticle.eventbus.EventbusEndpoint;
import com.gentics.mesh.core.verticle.group.GroupEndpoint;
import com.gentics.mesh.core.verticle.microschema.MicroschemaEndpoint;
import com.gentics.mesh.core.verticle.microschema.ProjectMicroschemaEndpoint;
import com.gentics.mesh.core.verticle.navroot.NavRootEndpoint;
import com.gentics.mesh.core.verticle.node.NodeEndpoint;
import com.gentics.mesh.core.verticle.project.ProjectEndpoint;
import com.gentics.mesh.core.verticle.project.ProjectInfoEndpoint;
import com.gentics.mesh.core.verticle.release.ReleaseEndpoint;
import com.gentics.mesh.core.verticle.role.RoleEndpoint;
import com.gentics.mesh.core.verticle.schema.ProjectSchemaEndpoint;
import com.gentics.mesh.core.verticle.schema.SchemaEndpoint;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyEndpoint;
import com.gentics.mesh.core.verticle.user.UserEndpoint;
import com.gentics.mesh.core.verticle.utility.UtilityEndpoint;
import com.gentics.mesh.core.verticle.webroot.WebRootEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphql.GraphQLEndpoint;
import com.gentics.mesh.search.ProjectRawSearchEndpointImpl;
import com.gentics.mesh.search.ProjectSearchEndpointImpl;
import com.gentics.mesh.search.RawSearchEndpointImpl;
import com.gentics.mesh.search.SearchEndpointImpl;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

/**
 * Central REST API Verticle which will provide all core REST API Endpoints
 */
@Singleton
public class RestAPIVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractEndpoint.class);

	protected HttpServer server;

	@Inject
	public Provider<RouterStorage> routerStorage;

	@Inject
	public Provider<UserEndpoint> userEndpoint;

	@Inject
	public Provider<RoleEndpoint> roleEndpoint;

	@Inject
	public Provider<GroupEndpoint> groupEndpoint;

	@Inject
	public Provider<ProjectEndpoint> projectEndpoint;

	@Inject
	public Provider<NodeEndpoint> nodeEndpoint;

	@Inject
	public Provider<TagFamilyEndpoint> tagFamilyEndpoint;

	@Inject
	public Provider<ReleaseEndpoint> releaseEndpoint;

	@Inject
	public Provider<SchemaEndpoint> schemaEndpoint;

	@Inject
	public Provider<ProjectSearchEndpointImpl> projectSearchEndpoint;

	@Inject
	public Provider<ProjectRawSearchEndpointImpl> projectRawSearchEndpoint;

	@Inject
	public Provider<ProjectSchemaEndpoint> projectSchemaEndpoint;

	@Inject
	public Provider<ProjectInfoEndpoint> projectInfoEndpoint;

	@Inject
	public Provider<ProjectMicroschemaEndpoint> projectMicroschemaEndpoint;

	@Inject
	public Provider<WebRootEndpoint> webrootEndpoint;

	@Inject
	public Provider<RestInfoEndpoint> restInfoEndpoint;

	@Inject
	public Provider<UtilityEndpoint> utilityEndpoint;

	@Inject
	public Provider<MicroschemaEndpoint> microschemaEndpoint;

	@Inject
	public Provider<EventbusEndpoint> eventbusEndpoint;

	@Inject
	public Provider<NavRootEndpoint> navrootEndpoint;

	@Inject
	public Provider<AuthenticationEndpoint> authenticationEndpoint;

	@Inject
	public Provider<SearchEndpointImpl> searchEndpoint;

	@Inject
	public Provider<RawSearchEndpointImpl> rawSearchEndpoint;

	@Inject
	public Provider<GraphQLEndpoint> graphqlEndpoint;

	@Inject
	public Provider<AdminEndpoint> adminEndpoint;

	@Inject
	public RestAPIVerticle() {
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		// this.localRouter = setupLocalRouter();
		// if (localRouter == null) {
		// throw new MeshConfigurationException("The local router was not setup correctly. Startup failed.");
		// }
		int port = config().getInteger("port");
		String host = config().getString("host");
		if (log.isInfoEnabled()) {
			log.info("Starting http server on {" + host + ":" + port + "}..");
		}
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(port);
		options.setHost(host);
		options.setCompressionSupported(true);
		// options.setLogActivity(true);
		// MeshOptions meshOptions = Mesh.mesh().getOptions();
		// HttpServerConfig httpServerOptions = meshOptions.getHttpServerOptions();
		// if (httpServerOptions.isSsl()) {
		// if (log.isErrorEnabled()) {
		// log.debug("Setting ssl server options");
		// }
		// options.setSsl(true);
		// PemKeyCertOptions keyOptions = new PemKeyCertOptions();
		// if (isEmpty(httpServerOptions.getCertPath()) || isEmpty(httpServerOptions.getKeyPath())) {
		// throw new MeshConfigurationException("SSL is enabled but either the server key or the cert path was not specified.");
		// }
		// keyOptions.setKeyPath(httpServerOptions.getKeyPath());
		// keyOptions.setCertPath(httpServerOptions.getCertPath());
		// options.setPemKeyCertOptions(keyOptions);
		// }

		log.info("Starting http server in verticle {" + getClass().getName() + "} on port {" + options.getPort() + "}");
		server = vertx.createHttpServer(options);
		RouterStorage storage = routerStorage.get();
		Router router = storage.getRootRouter();
		server.requestHandler(router::accept);

		server.listen(rh -> {
			if (rh.failed()) {
				startFuture.fail(rh.cause());
			} else {
				if (log.isInfoEnabled()) {
					log.info("Started http server.. Port: " + config().getInteger("port"));
				}
				try {
					registerEndPoints(storage);
					startFuture.complete();
				} catch (Exception e) {
					e.printStackTrace();
					startFuture.fail(e);
				}
			}
		});

	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		server.close(rh -> {
			if (rh.failed()) {
				stopFuture.fail(rh.cause());
			} else {
				stopFuture.complete();
			}
		});
	}

	/**
	 * Register the API endpoints and bind them to the given router.
	 * 
	 * @param router
	 * @throws Exception
	 */
	private void registerEndPoints(RouterStorage storage) throws Exception {

		List<AbstractEndpoint> endpoints = new ArrayList<>();
		endpoints.add(restInfoEndpoint.get());
		// verticles.add(projectInfoVerticle());

		// User Group Role verticles
		endpoints.add(userEndpoint.get());
		endpoints.add(groupEndpoint.get());
		endpoints.add(roleEndpoint.get());

		// Project specific verticles
		endpoints.add(nodeEndpoint.get());
		endpoints.add(tagFamilyEndpoint.get());
		endpoints.add(projectSchemaEndpoint.get());
		endpoints.add(projectMicroschemaEndpoint.get());
		endpoints.add(projectSearchEndpoint.get());
		endpoints.add(projectRawSearchEndpoint.get());
		endpoints.add(releaseEndpoint.get());
		endpoints.add(graphqlEndpoint.get());

		// Global verticles
		endpoints.add(webrootEndpoint.get());
		endpoints.add(navrootEndpoint.get());
		endpoints.add(projectEndpoint.get());
		endpoints.add(schemaEndpoint.get());
		endpoints.add(microschemaEndpoint.get());
		endpoints.add(searchEndpoint.get());
		endpoints.add(rawSearchEndpoint.get());
		endpoints.add(authenticationEndpoint.get());
		endpoints.add(adminEndpoint.get());
		endpoints.add(eventbusEndpoint.get());
		endpoints.add(utilityEndpoint.get());
		endpoints.add(projectInfoEndpoint.get());

		for (AbstractEndpoint endpoint : endpoints) {
			endpoint.init(storage);
			endpoint.registerEndPoints();
		}
	}

	public HttpServer getServer() {
		return server;
	}

}
