package com.gentics.mesh.graphql;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

import graphql.GraphQL;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnionType;
import graphql.schema.TypeResolver;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.StaticHandler;

@Singleton
public class GraphQLVerticle extends AbstractWebVerticle {

	public GraphQLVerticle() {
		super("graphql", null);
	}

	@Inject
	public GraphQLVerticle(RouterStorage routerStorage) {
		super("graphql", routerStorage);
	}

	@Override
	public void registerEndPoints() throws Exception {

		GraphQLSchema schema = getSchema();
		Endpoint queryEndpoint = createEndpoint();
		queryEndpoint.method(POST);
		queryEndpoint.description("Endpoint which accepts GraphQL queries.");
		queryEndpoint.path("/");
		queryEndpoint.handler(rh -> {
			String body = rh.getBodyAsString();
			TestNode context = new TestNode();
			context.setUuid(UUIDUtil.randomUUID());
			context.getFields().add(new DateTestField("dateFieldName", "todayValue"));
			context.getFields().add(new StringTestField("stringFieldName", true));
			JsonObject queryJson = new JsonObject(body);
			String query = queryJson.getString("query");
			System.out.println(query);
			Map<String, Object> result = (Map<String, Object>) new GraphQL(schema).execute(query, context).getData();
			if (result == null) {
				rh.response().setStatusCode(400);
				rh.response().end("Query could not be executed");
			} else {
				rh.response().putHeader("Content-Type", "application/json");
				JsonObject response = new JsonObject();
				response.put("data", new JsonObject(JsonUtil.toJson(result)));
				rh.response().end(response.toString());
			}
			System.out.println(result);
			// Prints: {hello=world}
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

	private GraphQLSchema getSchema() {
		// Builder obj = newObject().name("helloWorldQuery");
		// obj.field(newFieldDefinition().type(GraphQLString).name("hello").staticValue("world").build());
		// obj.field(newFieldDefinition().type(GraphQLString).name("mop").dataFetcher(env -> {
		// return "mopValue";
		// }).build());
		// GraphQLObjectType queryType = obj.build();
		GraphQLObjectType dateFieldType = newObject().name("date").field(newFieldDefinition().name("name").type(GraphQLString).build())
				.field(newFieldDefinition().name("value").type(GraphQLString).build()).build();

		GraphQLObjectType stringFieldType = newObject().name("string").field(newFieldDefinition().name("name").type(GraphQLString).build())
				.field(newFieldDefinition().name("encoded").type(GraphQLBoolean).build()).build();

		// Builder userBuilder = newObject().name("User").description("The user");
		// userBuilder.field(newFieldDefinition().type(GraphQLString).name("name").staticValue("someName").build());
		// GraphQLObjectType userType = userBuilder.build();

		GraphQLInterfaceType comicCharacter = newInterface()
			    .name("ComicCharacter")
			    .description("A abstract comic character.")
			    .field(newFieldDefinition()
			            .name("name")
			            .description("The name of the character.")
			            .type(GraphQLString).build())
			    .typeResolver(r -> {
			    	return null;
			    })
			    .build();
		
		GraphQLUnionType fieldType = newUnionType().name("Fields").possibleType(dateFieldType).possibleType(stringFieldType)
				.typeResolver(new TypeResolver() {
					@Override
					public GraphQLObjectType getType(Object object) {
						if(object instanceof StringTestField) {
							return stringFieldType;
						}
						 if(object instanceof DateTestField) {
							 return dateFieldType;
						 }
						return stringFieldType;
					}
				}).build();

		GraphQLObjectType nodeType = newObject().name("Node").description("A Node")
				.field(newFieldDefinition().name("uuid").description("The uuid of node.").type(GraphQLString).build())
				.field(newFieldDefinition().name("fields").type(new GraphQLList(fieldType)).build()).build();
		GraphQLSchema schema = GraphQLSchema.newSchema().query(nodeType).build();
		return schema;

	}

	@Override
	public String getDescription() {
		return "Graph QL endpoint";
	}

}
