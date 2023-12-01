package com.gentics.mesh.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.endpoint.admin.AdminEndpoint;
import com.gentics.mesh.core.endpoint.admin.HealthEndpoint;
import com.gentics.mesh.core.endpoint.admin.RestInfoEndpoint;
import com.gentics.mesh.core.endpoint.auth.AuthenticationEndpoint;
import com.gentics.mesh.core.endpoint.branch.BranchEndpoint;
import com.gentics.mesh.core.endpoint.eventbus.EventbusEndpoint;
import com.gentics.mesh.core.endpoint.group.GroupEndpoint;
import com.gentics.mesh.core.endpoint.microschema.MicroschemaEndpoint;
import com.gentics.mesh.core.endpoint.microschema.ProjectMicroschemaEndpoint;
import com.gentics.mesh.core.endpoint.navroot.NavRootEndpoint;
import com.gentics.mesh.core.endpoint.node.NodeEndpoint;
import com.gentics.mesh.core.endpoint.project.LanguageEndpoint;
import com.gentics.mesh.core.endpoint.project.ProjectEndpoint;
import com.gentics.mesh.core.endpoint.project.ProjectInfoEndpoint;
import com.gentics.mesh.core.endpoint.role.RoleEndpoint;
import com.gentics.mesh.core.endpoint.schema.ProjectSchemaEndpoint;
import com.gentics.mesh.core.endpoint.schema.SchemaEndpoint;
import com.gentics.mesh.core.endpoint.tagfamily.TagFamilyEndpoint;
import com.gentics.mesh.core.endpoint.user.UserEndpoint;
import com.gentics.mesh.core.endpoint.utility.UtilityEndpoint;
import com.gentics.mesh.core.endpoint.webroot.WebRootEndpoint;
import com.gentics.mesh.core.endpoint.webrootfield.WebRootFieldEndpoint;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.GraphQLEndpoint;
import com.gentics.mesh.router.RouterStorageImpl;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.search.ProjectRawSearchEndpointImpl;
import com.gentics.mesh.search.ProjectSearchEndpointImpl;
import com.gentics.mesh.search.RawSearchEndpointImpl;
import com.gentics.mesh.search.SearchEndpointImpl;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.Router;
import io.vertx.reactivex.core.http.HttpServer;

/**
 * Central REST API Verticle which will provide all core REST API Endpoints
 */
public class RestAPIVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractInternalEndpoint.class);

	private HttpServer httpsServer;

	private HttpServer httpServer;

	protected final Provider<RouterStorageImpl> routerStorage;

	protected final Provider<UserEndpoint> userEndpoint;

	protected final Provider<LanguageEndpoint> languageEndpoint;

	protected final Provider<RoleEndpoint> roleEndpoint;

	protected final Provider<GroupEndpoint> groupEndpoint;

	protected final Provider<ProjectEndpoint> projectEndpoint;

	protected final Provider<NodeEndpoint> nodeEndpoint;

	protected final Provider<TagFamilyEndpoint> tagFamilyEndpoint;

	protected final Provider<BranchEndpoint> branchEndpoint;

	protected final Provider<SchemaEndpoint> schemaEndpoint;

	protected final Provider<ProjectSearchEndpointImpl> projectSearchEndpoint;

	protected final Provider<ProjectRawSearchEndpointImpl> projectRawSearchEndpoint;

	protected final Provider<ProjectSchemaEndpoint> projectSchemaEndpoint;

	protected final Provider<ProjectInfoEndpoint> projectInfoEndpoint;

	protected final Provider<ProjectMicroschemaEndpoint> projectMicroschemaEndpoint;

	protected final Provider<WebRootEndpoint> webrootEndpoint;

	protected final Provider<WebRootFieldEndpoint> webrootFieldEndpoint;

	protected final Provider<RestInfoEndpoint> restInfoEndpoint;

	protected final Provider<UtilityEndpoint> utilityEndpoint;

	protected final Provider<MicroschemaEndpoint> microschemaEndpoint;

	protected final Provider<EventbusEndpoint> eventbusEndpoint;

	protected final Provider<NavRootEndpoint> navrootEndpoint;

	protected final Provider<AuthenticationEndpoint> authenticationEndpoint;

	protected final Provider<SearchEndpointImpl> searchEndpoint;

	protected final Provider<RawSearchEndpointImpl> rawSearchEndpoint;

	protected final Provider<GraphQLEndpoint> graphqlEndpoint;

	protected final Provider<AdminEndpoint> adminEndpoint;

	protected final Provider<HealthEndpoint> healthEndpoint;

	protected final RouterStorageRegistryImpl routerStorageRegistry;

	protected final io.vertx.reactivex.core.Vertx rxVertx;

	protected final Vertx vertx;

	protected final MeshOptions meshOptions;

	@Inject
	public RestAPIVerticle(Provider<RouterStorageImpl> routerStorage, Provider<UserEndpoint> userEndpoint,
			Provider<RoleEndpoint> roleEndpoint, Provider<GroupEndpoint> groupEndpoint, Provider<LanguageEndpoint> languageEndpoint,
			Provider<ProjectEndpoint> projectEndpoint, Provider<NodeEndpoint> nodeEndpoint,
			Provider<TagFamilyEndpoint> tagFamilyEndpoint, Provider<BranchEndpoint> branchEndpoint,
			Provider<SchemaEndpoint> schemaEndpoint, Provider<ProjectSearchEndpointImpl> projectSearchEndpoint,
			Provider<ProjectRawSearchEndpointImpl> projectRawSearchEndpoint,
			Provider<ProjectSchemaEndpoint> projectSchemaEndpoint, Provider<ProjectInfoEndpoint> projectInfoEndpoint,
			Provider<ProjectMicroschemaEndpoint> projectMicroschemaEndpoint, Provider<WebRootEndpoint> webrootEndpoint,
			Provider<WebRootFieldEndpoint> webrootFieldEndpoint, Provider<RestInfoEndpoint> restInfoEndpoint,
			Provider<UtilityEndpoint> utilityEndpoint, Provider<MicroschemaEndpoint> microschemaEndpoint,
			Provider<EventbusEndpoint> eventbusEndpoint, Provider<NavRootEndpoint> navrootEndpoint,
			Provider<AuthenticationEndpoint> authenticationEndpoint, Provider<SearchEndpointImpl> searchEndpoint,
			Provider<RawSearchEndpointImpl> rawSearchEndpoint, Provider<GraphQLEndpoint> graphqlEndpoint,
			Provider<AdminEndpoint> adminEndpoint, Provider<HealthEndpoint> healthEndpoint,
			RouterStorageRegistryImpl routerStorageRegistry, io.vertx.reactivex.core.Vertx rxVertx, Vertx vertx,
			MeshOptions meshOptions) {
		this.routerStorage = routerStorage;
		this.languageEndpoint = languageEndpoint;
		this.userEndpoint = userEndpoint;
		this.roleEndpoint = roleEndpoint;
		this.groupEndpoint = groupEndpoint;
		this.projectEndpoint = projectEndpoint;
		this.nodeEndpoint = nodeEndpoint;
		this.tagFamilyEndpoint = tagFamilyEndpoint;
		this.branchEndpoint = branchEndpoint;
		this.schemaEndpoint = schemaEndpoint;
		this.projectSearchEndpoint = projectSearchEndpoint;
		this.projectRawSearchEndpoint = projectRawSearchEndpoint;
		this.projectSchemaEndpoint = projectSchemaEndpoint;
		this.projectInfoEndpoint = projectInfoEndpoint;
		this.projectMicroschemaEndpoint = projectMicroschemaEndpoint;
		this.webrootEndpoint = webrootEndpoint;
		this.webrootFieldEndpoint = webrootFieldEndpoint;
		this.restInfoEndpoint = restInfoEndpoint;
		this.utilityEndpoint = utilityEndpoint;
		this.microschemaEndpoint = microschemaEndpoint;
		this.eventbusEndpoint = eventbusEndpoint;
		this.navrootEndpoint = navrootEndpoint;
		this.authenticationEndpoint = authenticationEndpoint;
		this.searchEndpoint = searchEndpoint;
		this.rawSearchEndpoint = rawSearchEndpoint;
		this.graphqlEndpoint = graphqlEndpoint;
		this.adminEndpoint = adminEndpoint;
		this.healthEndpoint = healthEndpoint;
		this.routerStorageRegistry = routerStorageRegistry;
		this.rxVertx = rxVertx;
		this.vertx = vertx;
		this.meshOptions = meshOptions;
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		JsonArray initialProjects = config().getJsonArray("initialProjects");
		HttpServerConfig meshServerOptions = meshOptions.getHttpServerOptions();

		if (meshServerOptions.isHttp()) {
			HttpServerOptions httpOptions = new HttpServerOptions();
			if (log.isDebugEnabled()) {
				log.debug("Setting http server options..");
			}
			applyCommonSettings(httpOptions, meshServerOptions);
			httpOptions.setPort(meshServerOptions.getPort());
			httpOptions.setSsl(false);

			log.info("Starting http server in verticle {" + getClass().getName() + "} on port {" + httpOptions.getPort() + "}");
			httpServer = rxVertx.createHttpServer(httpOptions);
		}

		if (meshServerOptions.isSsl()) {
			HttpServerOptions httpsOptions = new HttpServerOptions();
			if (log.isDebugEnabled()) {
				log.debug("Setting ssl server options..");
			}
			applyCommonSettings(httpsOptions, meshServerOptions);
			httpsOptions.setPort(meshServerOptions.getSslPort());
			httpsOptions.setSsl(true);
			PemKeyCertOptions keyOptions = new PemKeyCertOptions();

			httpsOptions.setClientAuth(meshServerOptions.getClientAuthMode());

			keyOptions.setKeyPath(meshServerOptions.getKeyPath());
			keyOptions.setCertPath(meshServerOptions.getCertPath());
			httpsOptions.setPemKeyCertOptions(keyOptions);

			PemTrustOptions pemTrustOptions = new PemTrustOptions();
			for (String path : meshServerOptions.getTrustedCertPaths()) {
				pemTrustOptions.addCertPath(path);
			}
			// Only set the trust options when those have been specified.
			// Internally Vert.x will only create a trust manager when options have been specified
			// Otherwise the default JVM trust manager will be used.
			if (!pemTrustOptions.getCertPaths().isEmpty()) {
				httpsOptions.setPemTrustOptions(pemTrustOptions);
			}

			log.info("Starting https server in verticle {" + getClass().getName() + "} on port {" + httpsOptions.getPort() + "}");
			httpsServer = rxVertx.createHttpServer(httpsOptions);
		}

		RouterStorageImpl storage = routerStorage.get();
		Router rootRouter = storage.root().getRouter();
		registerEndPoints(storage);

		if (initialProjects != null) {
			for (Object project : initialProjects) {
				routerStorageRegistry.addProject((String) project);
			}
		}

		io.vertx.reactivex.ext.web.Router rxRootRouter = io.vertx.reactivex.ext.web.Router.newInstance(rootRouter);
		// Now listen to requests from all created servers
		List<Single<HttpServer>> serverListens = Arrays.asList(httpServer, httpsServer).stream()
			.filter(Objects::nonNull)
			.map(s -> {
				return s.requestHandler(rxRootRouter);
			})
			.map(s -> s.rxListen())
			.collect(Collectors.toList());

		Single.merge(serverListens).ignoreElements().subscribe(() -> {
			promise.complete();
		}, err -> {
			promise.fail(err);
		});

	}

	private void applyCommonSettings(HttpServerOptions options, HttpServerConfig serverConfig) {
		String host = config().getString("host");
		options.setHost(host);
		options.setCompressionSupported(true);
		options.setHandle100ContinueAutomatically(true);
		options.setUseAlpn(serverConfig.isUseAlpn());
		// options.setLogActivity(true);

		// TCP options
		options.setTcpFastOpen(true)
			.setTcpNoDelay(true)
			.setTcpQuickAck(true);

		options.setMaxFormAttributeSize(serverConfig.getMaxFormAttributeSize());
	}

	@Override
	public void stop(Promise<Void> promise) throws Exception {
		List<Completable> serverClose = Arrays.asList(httpServer, httpsServer).stream()
			.filter(Objects::nonNull)
			.map(HttpServer::rxClose)
			.collect(Collectors.toList());

		Completable.merge(serverClose)
			.subscribe(promise::complete, promise::fail);
	}

	/**
	 * Register the API endpoints and bind them to the given router.
	 *
	 * @param storage
	 * @throws Exception
	 */
	private void registerEndPoints(RouterStorageImpl storage) throws Exception {

		List<AbstractInternalEndpoint> endpoints = new ArrayList<>();
		endpoints.add(restInfoEndpoint.get());
		// verticles.add(projectInfoVerticle());
		endpoints.add(languageEndpoint.get());

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
		endpoints.add(branchEndpoint.get());
		endpoints.add(graphqlEndpoint.get());

		// Global verticles
		endpoints.add(webrootEndpoint.get());
		endpoints.add(webrootFieldEndpoint.get());
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
		endpoints.add(healthEndpoint.get());

		for (AbstractInternalEndpoint endpoint : endpoints) {
			endpoint.init(vertx, storage);
			endpoint.registerEndPoints();
		}
	}

}
