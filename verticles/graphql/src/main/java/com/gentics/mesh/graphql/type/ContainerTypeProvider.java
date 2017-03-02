package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class ContainerTypeProvider extends AbstractTypeProvider {

	@Inject
	public NodeFieldTypeProvider nodeFieldTypeProvider;

	@Inject
	public ContainerTypeProvider() {

	}

	public GraphQLObjectType getContainerType(Project project) {

		Builder type = newObject().name("Container")
				.description("Language specific node container which contains the node fields");

		// .node
		type.field(newFieldDefinition().name("node")
				.description("Node to which the container belongs")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer container = (NodeGraphFieldContainer) source;
						return container.getParentNode();
					}
					return null;
				})
				.build());

		type.field(newFieldDefinition().name("edited")
				.description("ISO8601 formatted edit timestamp")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof EditorTrackingVertex) {
						return ((EditorTrackingVertex) source).getLastEditedDate();
					}
					return null;
				})
				.build());

//		type.field(newFieldDefinition().name("created")
//				.description("ISO8601 formatted created date string")
//				.type(GraphQLString)
//				.dataFetcher(fetcher -> {
//					Object source = fetcher.getSource();
//					if (source instanceof CreatorTrackingVertex) {
//						return ((CreatorTrackingVertex) source).getCreationDate();
//					}
//					return null;
//				})
//				.build());
//
//		type.field(newFieldDefinition().name("creator")
//				.description("Creator of the element")
//				.type(new GraphQLTypeReference("User"))
//				.dataFetcher(fetcher -> {
//					Object source = fetcher.getSource();
//					if (source instanceof CreatorTrackingVertex) {
//						return ((CreatorTrackingVertex) source).getCreator();
//					}
//					return null;
//				})
//				.build());

		// .editor
		type.field(newFieldDefinition().name("editor")
				.description("Editor of the element")
				.type(new GraphQLTypeReference("User"))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof EditorTrackingVertex) {
						return ((EditorTrackingVertex) source).getEditor();
					}
					return null;
				})
				.build());

		// .version
		type.field(newFieldDefinition().name("version")
				.description("Version of the container")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer container = (NodeGraphFieldContainer) source;
						return container.getVersion()
								.getFullVersion();
					}
					return null;
				})
				.build());

		// .fields
		type.field(newFieldDefinition().name("fields")
				.type(nodeFieldTypeProvider.getSchemaFieldsType(project))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						return (NodeGraphFieldContainer) source;
					}
					return null;
				})
				.build());

		//.language
		type.field(newFieldDefinition().name("language")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						// TODO implement correct language handling
						return ((NodeGraphFieldContainer) source).getLanguage()
								.getLanguageTag();
					}
					return null;
				})
				.build());

		return type.build();
	}
}
