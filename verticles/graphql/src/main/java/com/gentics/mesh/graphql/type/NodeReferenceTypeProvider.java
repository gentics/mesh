package com.gentics.mesh.graphql.type;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.graphql.context.GraphQLContext;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

import javax.inject.Singleton;
import java.util.List;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

@Singleton
public class NodeReferenceTypeProvider extends AbstractTypeProvider {

    public static final String NODE_REFERENCE_TYPE_NAME = "NodeReference";

    public static final String NODE_REFERENCE_PAGE_TYPE_NAME = "NodeReferencePage";

    public GraphQLObjectType createType() {
        GraphQLObjectType.Builder nodeType = newObject();
        nodeType.name(NODE_REFERENCE_TYPE_NAME);
        nodeType.description("A node reference is a link from one node to another.");

        nodeType.field(
            newFieldDefinition()
            .name("node")
            .description("Load the node that references this node.")
            .type(new GraphQLTypeReference(NODE_TYPE_NAME))
            .dataFetcher(env -> {
                Node node = env.getSource();
                GraphQLContext gc = env.getContext();
                List<String> languageTags = getLanguageArgument(env);

                NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(gc, languageTags);
                return new NodeContent(node, container);
            })
        );

        nodeType.field(
            newFieldDefinition()
            .name("fieldName")
            .description("The field name in which this node was referenced.")
            .type(GraphQLString)
            .dataFetcher(env -> {
                NodeReference reference = env.getSource();
                return reference.fieldName;
            })
        );

        return nodeType.build();
    }

    public static final class NodeReference {
        public Node node;
        public String fieldName;

        public NodeReference(Node node, String fieldName) {
            this.node = node;
            this.fieldName = fieldName;
        }
    }
}
