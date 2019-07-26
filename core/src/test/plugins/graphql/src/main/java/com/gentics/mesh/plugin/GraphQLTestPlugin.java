package com.gentics.mesh.plugin;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphQLTestPlugin extends AbstractPlugin implements GraphQLPlugin {

	private static final Logger log = LoggerFactory.getLogger(GraphQLTestPlugin.class);

	public GraphQLTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public void start() {
		log.info("Starting GraphQL plugin");
	}

	@Override

	public GraphQLObjectType createType() {
		Builder root = newObject();
		root.name("PluginDataType");
		root.description("Dummy GraphQL Test");
		root.field(newFieldDefinition().name("text").type(GraphQLString).description("The text field returns info text").dataFetcher(env -> {
			return "hello-world";
		}));
		return root.build();
	}
}
