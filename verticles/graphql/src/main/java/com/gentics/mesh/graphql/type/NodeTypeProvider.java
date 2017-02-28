package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.graphql.type.argument.ArgumentsProvider;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class NodeTypeProvider {

	@Inject
	public NodeFieldTypeProvider nodeFieldTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ArgumentsProvider argumentsProvider;

	@Inject
	public TagTypeProvider tagTypeProvider;

	@Inject
	public NodeTypeProvider() {
	}

	public GraphQLObjectType getNodeType(Project project) {
		Builder nodeType = newObject();
		nodeType.name("Node");
		nodeType.description("A Node");
		interfaceTypeProvider.addCommonFields(nodeType);
		nodeType.field(newFieldDefinition().name("project")
				.description("Project of the node")
				.type(new GraphQLTypeReference("Project"))
				.build());
		nodeType.field(newFieldDefinition().name("children")
				.argument(argumentsProvider.getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.build());
		nodeType.field(newFieldDefinition().name("parent")
				.type(new GraphQLTypeReference("Node"))
				.build());
		nodeType.field(newFieldDefinition().name("tags")
				.argument(argumentsProvider.getPagingArgs())
				.type(tagTypeProvider.getTagType()));
		nodeType.field(newFieldDefinition().name("fields")
				.type(nodeFieldTypeProvider.getFieldsType(project))
				.dataFetcher(fetcher -> {
					if (fetcher.getSource() instanceof Node) {
						Node node = (Node) fetcher.getSource();
						NodeGraphFieldContainer nodeContainer = node.getGraphFieldContainer("en");
						Map<String, Object> context = (Map<String, Object>) fetcher.getContext();
						context.put("nodeContainer", nodeContainer);
						return nodeContainer;
					}
					return null;
				})
				.build())
				.build();
		return nodeType.build();
	}
}
