package com.gentics.mesh.plugin;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.graphql.GraphQLPlugin;

import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchema.Builder;
import io.reactivex.Completable;

public class GraphQLTestPlugin extends AbstractPlugin implements GraphQLPlugin {

	private GraphQLSchema schema;

	public GraphQLTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		Builder schemaBuilder = GraphQLSchema.newSchema();
		schema = schemaBuilder.query(newObject()
			.name("PluginDataType")
			.description("Dummy GraphQL Test")
			.field(newFieldDefinition().name("text")
				.type(GraphQLString)
				.description("Say hello to the world of plugins")
				.dataFetcher(env -> {
					return "hello-world";
				}))
			.build()).build();
		return Completable.complete();
	}

	@Override
	public GraphQLSchema createRootSchema() {
		return schema;
	}

}
