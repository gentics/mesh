package com.gentics.mesh.plugin;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import graphql.schema.GraphQLObjectType;

public class GraphQLTestPlugin extends AbstractPlugin implements GraphQLPlugin {

	public GraphQLTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public GraphQLObjectType createType() {
		return newObject()
			.name("PluginDataType")
			.description("Dummy GraphQL Test")
			.field(newFieldDefinition().name("text")
				.type(GraphQLString)
				.description("Say hello to the world of plugins")
				.dataFetcher(env -> {
					return "hello-world";
				}))
			.build();
	}
}
