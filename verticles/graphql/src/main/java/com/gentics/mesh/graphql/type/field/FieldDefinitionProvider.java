package com.gentics.mesh.graphql.type.field;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import com.gentics.mesh.graphql.type.MicronodeFieldTypeProvider;
import com.gentics.mesh.util.DateUtils;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class FieldDefinitionProvider extends AbstractTypeProvider {

	@Inject
	public MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	@Inject
	public FieldDefinitionProvider() {
	}

	public GraphQLObjectType getBinaryFieldType() {
		Builder type = newObject().name("BinaryField")
				.description("Binary field");

		type.field(newFieldDefinition().name("fileName")
				.description("Filename of the uploaded file")
				.type(GraphQLString)
				.build());

		type.field(newFieldDefinition().name("width")
				.description("Image width")
				.type(GraphQLInt)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof BinaryGraphField) {
						return ((BinaryGraphField) source).getImageWidth();
					}
					return null;
				})
				.build());

		type.field(newFieldDefinition().name("height")
				.description("Image height")
				.type(GraphQLInt)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof BinaryGraphField) {
						return ((BinaryGraphField) source).getImageHeight();
					}
					return null;
				})
				.build());

		type.field(newFieldDefinition().name("sha512sum")
				.description("SHA512 checksum of the data")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof BinaryGraphField) {
						return ((BinaryGraphField) source).getSHA512Sum();
					}
					return null;
				})
				.build());

		type.field(newFieldDefinition().name("fileSize")
				.description("Size of the binary data in bytes")
				.type(GraphQLLong)
				.build());

		type.field(newFieldDefinition().name("mimeType")
				.description("Mimetype of the binary data")
				.type(GraphQLString)
				.build());

		type.field(newFieldDefinition().name("dominantColor")
				.description("Computed image dominant color")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof BinaryGraphField) {
						return ((BinaryGraphField) source).getImageDominantColor();
					}
					return null;
				})
				.build());

		return type.build();
	}

	public GraphQLFieldDefinition getBinaryDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(getBinaryFieldType())
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getBinary(schema.getName());
					}
					return null;
				})
				.build();

	}

	public GraphQLFieldDefinition getBooleanDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(GraphQLBoolean)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getBoolean(schema.getName())
								.getBoolean();
					}
					return null;
				})
				.build();
	}

	public GraphQLFieldDefinition getNumberDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(GraphQLBigDecimal)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getNumber(schema.getName())
								.getNumber();
					}
					return null;
				})
				.build();
	}

	public GraphQLFieldDefinition getHtmlDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getHtml(schema.getName())
								.getHTML();
					}
					return null;
				})
				.build();
	}

	public GraphQLFieldDefinition getStringDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof GraphFieldContainer) {
						GraphFieldContainer nodeContainer = (GraphFieldContainer) source;
						return nodeContainer.getString(schema.getName())
								.getString();
					}
					return null;
				})
				.build();
	}

	public GraphQLFieldDefinition getDateDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return DateUtils.toISO8601(nodeContainer.getDate(schema.getName())
								.getDate(), 0);
					}
					return null;
				})
				.build();
	}

	public GraphQLFieldDefinition getListDef(ListFieldSchema schema) {
		GraphQLType type = getElementTypeOfList(schema);
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(new GraphQLList(type))
				.argument(getPagingArgs())
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;

						switch (schema.getListType()) {
						case "boolean":
							return nodeContainer.getBooleanList(schema.getName())
									.getList()
									.stream()
									.map(item -> item.getBoolean())
									.collect(Collectors.toList());
						case "html":
							return nodeContainer.getHTMLList(schema.getName())
									.getList()
									.stream()
									.map(item -> item.getHTML())
									.collect(Collectors.toList());
						case "string":
							return nodeContainer.getStringList(schema.getName())
									.getList()
									.stream()
									.map(item -> item.getString())
									.collect(Collectors.toList());
						case "number":
							return nodeContainer.getNumberList(schema.getName())
									.getList()
									.stream()
									.map(item -> item.getNumber())
									.collect(Collectors.toList());
						case "date":
							return nodeContainer.getDateList(schema.getName())
									.getList()
									.stream()
									.map(item -> DateUtils.toISO8601(item.getDate(), 0))
									.collect(Collectors.toList());
						case "node":
							return nodeContainer.getNodeList(schema.getName())
									.getList()
									.stream()
									.map(item -> item.getNode())
									.collect(Collectors.toList());
						case "micronode":
							return null;
						default:
							return null;
						}

					}
					return null;
				})
				.build();
	}

	private GraphQLType getElementTypeOfList(ListFieldSchema schema) {
		switch (schema.getListType()) {
		case "boolean":
			return GraphQLBoolean;
		case "html":
			return GraphQLString;
		case "string":
			return GraphQLString;
		case "number":
			return GraphQLBigDecimal;
		case "date":
			return GraphQLString;
		case "node":
			return new GraphQLTypeReference("Node");
		case "micronode":
			return new GraphQLTypeReference("Micronode");
		default:
			return null;
		}
	}

	public GraphQLFieldDefinition getMicronodeDef(FieldSchema schema, Project project) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(micronodeFieldTypeProvider.getMicroschemaFieldsType(project))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getMicronode(schema.getName()).getMicronode();
					}
					return null;
				})
				.build();
	}

	public GraphQLFieldDefinition getNodeDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName())
				.description(schema.getLabel())
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer nodeContainer = (NodeGraphFieldContainer) source;
						return nodeContainer.getNode(schema.getName());
					}
					return null;
				})
				.build();
	}

}
