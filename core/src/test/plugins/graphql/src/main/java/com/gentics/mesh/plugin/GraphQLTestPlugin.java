package com.gentics.mesh.plugin;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import graphql.schema.GraphQLObjectType;
import io.reactivex.Completable;

public class GraphQLTestPlugin extends AbstractPlugin implements GraphQLPlugin {

	private GraphQLObjectType type;

	public GraphQLTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		type = newObject()
			.name("PluginDataType")
			.description("Dummy GraphQL Test")
			.field(newFieldDefinition().name("text")
				.type(GraphQLString)
				.description("Say hello to the world of plugins")
				.dataFetcher(env -> {
					return "hello-world";
				}))
			.build();
		type = prefixType(type);
		return Completable.complete();
	}

	private GraphQLObjectType prefixType(GraphQLObjectType type) {
		return type.transform(b -> {
			b.name(id() + "_" + type.getName());
		});
	}

	@Override
	public GraphQLObjectType createRootType() {
		return type;
	}

}
