package com.gentics.mesh.graphql;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.context.impl.GraphQLContextImpl;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.StaticHandler;

public class GraphQLEndpoint extends AbstractProjectEndpoint {

	private static final Logger log = LoggerFactory.getLogger(GraphQLEndpoint.class);

	private GraphQLHandler queryHandler;

	public GraphQLEndpoint() {
		super("graphql", null, null, null, null, null);
	}

	@Inject
	public GraphQLEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, GraphQLHandler queryHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("graphql", chain, boot, localConfigApi, db, options);
		this.queryHandler = queryHandler;
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		InternalEndpointRoute queryEndpoint = createRoute();
		queryEndpoint.method(POST);
		queryEndpoint.exampleRequest(graphqlExamples.createQueryRequest());
		queryEndpoint.addQueryParameters(SearchParametersImpl.class);
		queryEndpoint.exampleResponse(OK, graphqlExamples.createResponse(), "Basic GraphQL response.");
		queryEndpoint.description("Endpoint which accepts GraphQL queries.");
		queryEndpoint.path("/");
		// TODO Change this when mutations are implemented
		queryEndpoint.setMutating(false);
		queryEndpoint.blockingHandler(rc -> {
			GraphQLContext gc = new GraphQLContextImpl(rc, boot.mesh().getOptions().getHttpServerOptions());
			String body = gc.getBodyAsString();
			queryHandler.handleQuery(gc, body);
		}, false);

		StaticHandler staticHandler = StaticHandler.create("graphiql");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");

		// Redirect handler
		route("/browser").method(GET).handler(rc -> {
			if (rc.request().path().endsWith("/browser")) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", rc.request().path() + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});

		route("/browser/*").method(GET).handler(staticHandler);

	}

	@Override
	public String getDescription() {
		return "GraphQL endpoint";
	}

}
