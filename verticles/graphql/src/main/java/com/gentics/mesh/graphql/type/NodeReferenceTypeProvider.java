package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

/**
 * GraphQL type provider for node references.
 */
@Singleton
public class NodeReferenceTypeProvider extends AbstractTypeProvider {

	public static final String NODE_REFERENCE_TYPE_NAME = "NodeReference";

	public static final String NODE_REFERENCE_PAGE_TYPE_NAME = "NodeReferencePage";

	@Inject
	public NodeReferenceTypeProvider(MeshOptions options) {
		super(options);
	}

	public GraphQLObjectType createType() {
		GraphQLObjectType.Builder nodeType = newObject();
		nodeType.name(NODE_REFERENCE_TYPE_NAME);
		nodeType.description("A node reference is a link from one node to another.");

		nodeType.field(newFieldDefinition()
			.name("node")
			.description("Load the node that references this node.")
			.type(nonNull(new GraphQLTypeReference(NODE_TYPE_NAME)))
			.dataFetcher(env -> {
				NodeReferenceIn reference = env.getSource();
				return reference.getNode();
			}));

		nodeType.field(newFieldDefinition()
			.name("fieldName")
			.description("The field name in which this node was referenced.")
			.type(nonNull(GraphQLString))
			.dataFetcher(env -> {
				NodeReferenceIn reference = env.getSource();
				return reference.getFieldName();
			}));

		nodeType.field(newFieldDefinition()
			.name("micronodeFieldName")
			.description("The micronode field name in which this node was referenced." +
				"Null if the reference did not originate from a micronode.")
			.type(GraphQLString)
			.dataFetcher(env -> {
				NodeReferenceIn reference = env.getSource();
				return reference.getMicronodeFieldName();
			}));

		return nodeType.build();
	}

}
