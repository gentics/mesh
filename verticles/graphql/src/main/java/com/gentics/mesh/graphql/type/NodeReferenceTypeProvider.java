package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;

import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class NodeReferenceTypeProvider extends AbstractTypeProvider {

	public static final String NODE_REFERENCE_TYPE_NAME = "NodeReference";

	public static final String NODE_REFERENCE_PAGE_TYPE_NAME = "NodeReferencePage";

	@Inject
	public NodeReferenceTypeProvider() {
	}

	public GraphQLObjectType createType() {
		GraphQLObjectType.Builder nodeType = newObject();
		nodeType.name(NODE_REFERENCE_TYPE_NAME);
		nodeType.description("A node reference is a link from one node to another.");

		nodeType.field(
			newFieldDefinition()
				.name("node")
				.description("Load the node that references this node.")
				.type(nonNull(new GraphQLTypeReference(NODE_TYPE_NAME)))
				.dataFetcher(env -> {
					NodeReference reference = env.getSource();
					return reference.getNode();
				})
		);

		nodeType.field(
			newFieldDefinition()
				.name("fieldName")
				.description("The field name in which this node was referenced.")
				.type(nonNull(GraphQLString))
				.dataFetcher(env -> {
					NodeReference reference = env.getSource();
					return reference.getFieldName();
				})
		);

		nodeType.field(
			newFieldDefinition()
				.name("micronodeFieldName")
				.description("The micronode field name in which this node was referenced." +
					"Null if the reference did not originate from a micronode.")
				.type(GraphQLString)
				.dataFetcher(env -> {
					NodeReference reference = env.getSource();
					return reference.getMicronodeFieldName();
				})
		);

		return nodeType.build();
	}

	public static final class NodeReference {
		private final NodeContent node;
		private final Lazy<String> fieldName;
		private final Lazy<String> micronodeFieldName;

		public NodeReference(NodeContent node, NodeGraphField nodeGraphField) {
			this.node = node;
			this.fieldName = new Lazy<>(nodeGraphField::getFieldName);
			this.micronodeFieldName = new Lazy<>(() -> nodeGraphField.getMicronodeFieldName().orElse(null));
		}

		public NodeContent getNode() {
			return node;
		}

		public String getFieldName() {
			return fieldName.get();
		}

		public String getMicronodeFieldName() {
			return micronodeFieldName.get();
		}
	}
}
