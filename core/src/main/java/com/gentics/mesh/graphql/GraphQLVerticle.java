package com.gentics.mesh.graphql;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.ext.web.handler.StaticHandler;

@Singleton
public class GraphQLVerticle extends AbstractWebVerticle {

	private GraphQLHandler queryHandler;

	public GraphQLVerticle() {
		super("graphql", null);
	}

	@Inject
	public GraphQLVerticle(RouterStorage routerStorage, GraphQLHandler queryHandler) {
		super("graphql", routerStorage);
		this.queryHandler = queryHandler;
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		Endpoint queryEndpoint = createEndpoint();
		queryEndpoint.method(POST);
		queryEndpoint.description("Endpoint which accepts GraphQL queries.");
		queryEndpoint.path("/");
		queryEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String body = ac.getBodyAsString();
			queryHandler.handleQuery(ac, body);
		});

		StaticHandler staticHandler = StaticHandler.create("graphiql");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		route("/browser/*").method(GET).handler(staticHandler);

		// Redirect handler
		route("/browser").method(GET).handler(rc -> {
			if ("/browser".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", rc.request().path() + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});

	}

	@Override
	public String getDescription() {
		return "Graph QL endpoint";
	}

}
