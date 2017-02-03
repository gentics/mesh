package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;

@Singleton
public class NodeTypeProvider {

	private NodeFieldTypeProvider nodeFieldTypeProvider;

	@Inject
	public NodeTypeProvider(NodeFieldTypeProvider nodeFieldTypeProvider) {
		this.nodeFieldTypeProvider = nodeFieldTypeProvider;
	}

	public GraphQLObjectType getNodeType(Project project) {
		GraphQLObjectType nodeType = newObject().name("Node").description("A Node")
				.field(newFieldDefinition().name("uuid").description("The uuid of node.").type(GraphQLString).build())
				.field(newFieldDefinition().name("fields").type(nodeFieldTypeProvider.getFieldsType(project)).dataFetcher(fetcher -> {
					if(fetcher.getSource() instanceof Node)  {
						Node node = (Node)fetcher.getSource();
						return node.getGraphFieldContainer("en");
					}
					return null;
				}).build()).build();
		return nodeType;
	}
}
