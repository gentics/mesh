package com.gentics.mesh.graphql;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractProjectEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.context.impl.GraphQLContextImpl;
import com.gentics.mesh.rest.EndpointRoute;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.StaticHandler;

@Singleton
public class GraphQLEndpoint extends AbstractProjectEndpoint {

	private static final Logger log = LoggerFactory.getLogger(GraphQLEndpoint.class);

	private GraphQLHandler queryHandler;

	public GraphQLEndpoint() {
		super("graphql", null, null);
	}

	@Inject
	public GraphQLEndpoint(BootstrapInitializer boot, RouterStorage routerStorage, GraphQLHandler queryHandler) {
		super("graphql", boot, routerStorage);
		this.queryHandler = queryHandler;
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		withBodyHandler();

		EndpointRoute queryEndpoint = createEndpoint();
		queryEndpoint.method(POST);
		queryEndpoint.exampleRequest(graphqlExamples.createQueryRequest());
		queryEndpoint.exampleResponse(OK, graphqlExamples.createResponse(), "Basic GraphQL response.");
		queryEndpoint.description("Endpoint which accepts GraphQL queries.");
		queryEndpoint.path("/");
		queryEndpoint.blockingHandler(rc -> {
			GraphQLContext gc = new GraphQLContextImpl(rc);
			String body = gc.getBodyAsString();
			queryHandler.handleQuery(gc, body);
		}, false);

		log.info("GraphiQL is owned and developed by Facebook, Inc. - Copyright (c) 2015, Facebook, Inc. All rights reserved.");
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
