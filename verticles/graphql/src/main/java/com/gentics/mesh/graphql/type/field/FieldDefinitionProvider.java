package com.gentics.mesh.graphql.type.field;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.image.FocalPoint;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.util.DateUtils;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.field.MicronodeFieldTypeProvider.MICRONODE_TYPE_NAME;
import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

@Singleton
public class FieldDefinitionProvider extends AbstractTypeProvider {

	public static final String BINARY_FIELD_TYPE_NAME = "BinaryField";

	@Inject
	public MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	@Inject
	public WebRootLinkReplacer linkReplacer;

	@Inject
	public FieldDefinitionProvider(MeshOptions options) {
		super(options);
	}

	public GraphQLObjectType createBinaryFieldType() {
		Builder type = newObject().name(BINARY_FIELD_TYPE_NAME).description("Binary field");

		// .binaryUuid
		type.field(newFieldDefinition().name("binaryUuid").description("UUID of the binary data.").type(GraphQLString).dataFetcher(fetcher -> {
			BinaryGraphField field = fetcher.getSource();
			Binary binary = field.getBinary();
			return binary == null ? 0 : binary.getUuid();
		}));

		// .fileName
		type.field(newFieldDefinition().name("fileName").description("Filename of the uploaded file.").type(GraphQLString));

		// .width
		type.field(newFieldDefinition().name("width").description("Image width in pixel.").type(GraphQLInt).dataFetcher(fetcher -> {
			BinaryGraphField field = fetcher.getSource();
			Binary binary = field.getBinary();
			return binary == null ? 0 : binary.getImageWidth();
		}));

		// .height
		type.field(newFieldDefinition().name("height").description("Image height in pixel.").type(GraphQLInt).dataFetcher(fetcher -> {
			BinaryGraphField field = fetcher.getSource();
			Binary binary = field.getBinary();
			return binary == null ? 0 : binary.getImageHeight();
		}));

		// .sha512sum
		type.field(
			newFieldDefinition().name("sha512sum").description("SHA512 checksum of the binary data.").type(GraphQLString).dataFetcher(fetcher -> {
				BinaryGraphField field = fetcher.getSource();
				return field.getBinary().getSHA512Sum();
			}));

		// .fileSize
		type.field(newFieldDefinition().name("fileSize").description("Size of the binary data in bytes").type(GraphQLLong).dataFetcher(fetcher -> {
			BinaryGraphField field = fetcher.getSource();
			return field.getBinary().getSize();
		}));

		// .mimeType
		type.field(newFieldDefinition().name("mimeType").description("Mimetype of the binary data").type(GraphQLString));

		// .dominantColor
		type.field(
			newFieldDefinition().name("dominantColor").description("Computed image dominant color").type(GraphQLString).dataFetcher(fetcher -> {
				BinaryGraphField field = fetcher.getSource();
				return field.getImageDominantColor();
			}));

		// .focalPoint
		type.field(
			newFieldDefinition().name("focalPoint").description("Focal point of the image.").type(createFocalPointType()).dataFetcher(fetcher -> {
				BinaryGraphField field = fetcher.getSource();
				return field.getImageFocalPoint();
			}));

		// .plainText
		type.field(newFieldDefinition().name("plainText").description("Extracted plain text of the uploaded document.").type(GraphQLString));

		return type.build();
	}

	public GraphQLObjectType createFocalPointType() {
		Builder type = newObject().name("FocalPoint").description("Focal point");

		// .x
		type.field(newFieldDefinition().name("x").description("X-axis factor of the focal point. Left is 0 and middle is 0.5.").type(GraphQLFloat)
			.dataFetcher(fetcher -> {
				FocalPoint point = fetcher.getSource();
				return point.getX();
			}));

		// .y
		type.field(newFieldDefinition().name("y").description("Y-axis factor of the focal point. Top is 0 and middle is 0.5.").type(GraphQLFloat)
			.dataFetcher(fetcher -> {
				FocalPoint point = fetcher.getSource();
				return point.getY();
			}));

		return type.build();
	}

	public GraphQLFieldDefinition createBinaryDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(new GraphQLTypeReference(BINARY_FIELD_TYPE_NAME))
			.dataFetcher(env -> {
				GraphFieldContainer container = env.getSource();
				return container.getBinary(schema.getName());
			}).build();

	}

	public GraphQLFieldDefinition createBooleanDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLBoolean).dataFetcher(env -> {
			GraphFieldContainer container = env.getSource();
			BooleanGraphField booleanField = container.getBoolean(schema.getName());
			if (booleanField != null) {
				return booleanField.getBoolean();
			}
			return null;
		}).build();
	}

	public GraphQLFieldDefinition createNumberDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLBigDecimal).dataFetcher(env -> {
			GraphFieldContainer container = env.getSource();
			NumberGraphField numberField = container.getNumber(schema.getName());
			if (numberField != null) {
				return numberField.getNumber();
			}
			return null;
		}).build();
	}

	public GraphQLFieldDefinition createHtmlDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLString).argument(createLinkTypeArg())
			.dataFetcher(env -> {
				GraphFieldContainer container = env.getSource();
				HtmlGraphField htmlField = container.getHtml(schema.getName());
				if (htmlField != null) {
					GraphQLContext gc = env.getContext();
					LinkType type = getLinkType(env);
					String content = htmlField.getHTML();
					return linkReplacer.replace(gc, gc.getBranch()
						.getUuid(), null, content, type, gc.getProject().getName(), Arrays.asList(container.getLanguageTag()));
				}
				return null;
			}).build();
	}

	public GraphQLFieldDefinition createStringDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLString).argument(createLinkTypeArg())
			.dataFetcher(env -> {
				GraphFieldContainer container = env.getSource();
				StringGraphField field = container.getString(schema.getName());
				if (field != null) {
					GraphQLContext gc = env.getContext();
					LinkType type = getLinkType(env);
					String content = field.getString();
					return linkReplacer.replace(gc, gc.getBranch()
						.getUuid(), null, content, type, gc.getProject().getName(), Arrays.asList(container.getLanguageTag()));
				}
				return null;
			}).build();
	}

	public GraphQLFieldDefinition createDateDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLString).dataFetcher(env -> {
			GraphFieldContainer container = env.getSource();
			DateGraphField dateField = container.getDate(schema.getName());
			if (dateField != null) {
				return DateUtils.toISO8601(dateField.getDate(), 0);
			}
			return null;
		}).build();

	}

	/**
	 * Create the GraphQL field definition for the given list field schema.
	 * 
	 * @param schema
	 * @return
	 */
	public GraphQLFieldDefinition createListDef(GraphQLContext context, ListFieldSchema schema) {
		GraphQLType type = getElementTypeOfList(schema);
		graphql.schema.GraphQLFieldDefinition.Builder fieldType = newFieldDefinition().name(schema.getName()).description(schema.getLabel())
			.type(new GraphQLList(type)).argument(createPagingArgs());
		NodeFilter nodeFilter = NodeFilter.filter(context);

		// Add link resolving arg to html and string lists
		switch (schema.getListType()) {
		case "html":
		case "string":
			fieldType.argument(createLinkTypeArg());
			break;
		case "node":
			fieldType.argument(nodeFilter.createFilterArgument());
			break;
		}

		return fieldType.dataFetcher(env -> {
			GraphFieldContainer container = env.getSource();
			GraphQLContext gc = env.getContext();

			switch (schema.getListType()) {
			case "boolean":
				BooleanGraphFieldList booleanList = container.getBooleanList(schema.getName());
				if (booleanList == null) {
					return null;
				}
				return booleanList.getList().stream().map(item -> item.getBoolean()).collect(Collectors.toList());
			case "html":
				HtmlGraphFieldList htmlList = container.getHTMLList(schema.getName());
				if (htmlList == null) {
					return null;
				}
				return htmlList.getList().stream().map(item -> {
					String content = item.getHTML();
					LinkType linkType = getLinkType(env);
					return linkReplacer.replace(gc, null, null, content, linkType, gc.getProject().getName(), Arrays.asList(container.getLanguageTag()));
				}).collect(Collectors.toList());
			case "string":
				StringGraphFieldList stringList = container.getStringList(schema.getName());
				if (stringList == null) {
					return null;
				}
				return stringList.getList().stream().map(item -> {
					String content = item.getString();
					LinkType linkType = getLinkType(env);
					return linkReplacer.replace(gc, null, null, content, linkType, gc.getProject().getName(), Arrays.asList(container.getLanguageTag()));
				}).collect(Collectors.toList());
			case "number":
				NumberGraphFieldList numberList = container.getNumberList(schema.getName());
				if (numberList == null) {
					return null;
				}
				return numberList.getList().stream().map(item -> item.getNumber()).collect(Collectors.toList());
			case "date":
				DateGraphFieldList dateList = container.getDateList(schema.getName());
				if (dateList == null) {
					return null;
				}
				return dateList.getList().stream().map(item -> DateUtils.toISO8601(item.getDate(), 0)).collect(Collectors.toList());
			case "node":
				NodeGraphFieldList nodeList = container.getNodeList(schema.getName());
				if (nodeList == null) {
					return null;
				}
				Map<String, ?> filterArgument = env.getArgument("filter");
				Stream<NodeContent> nodes = nodeList.getList().stream().map(item -> {
					Node node = item.getNode();
					List<String> languageTags;
					if (container instanceof NodeGraphFieldContainer) {
						languageTags = Arrays.asList(container.getLanguageTag());
					} else if (container instanceof Micronode) {
						Micronode micronode = (Micronode)container;
						languageTags = Arrays.asList(micronode.getContainer().getLanguageTag());
					} else {
						throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "container can only be NodeGraphFieldContainer or Micronode");
					}
					// TODO we need to add more assertions and check what happens if the itemContainer is null
					NodeGraphFieldContainer itemContainer = node.findVersion(gc, languageTags);
					return new NodeContent(node, itemContainer, languageTags);
				});
				if (filterArgument != null) {
					nodes = nodes.filter(nodeFilter.createPredicate(filterArgument));
				}
				return nodes.collect(Collectors.toList());
			case "micronode":
				MicronodeGraphFieldList micronodeList = container.getMicronodeList(schema.getName());
				if (micronodeList == null) {
					return null;
				}
				return micronodeList.getList().stream().map(item -> item.getMicronode()).collect(Collectors.toList());
			default:
				return null;
			}
		}).build();
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

	/**
	 * Create the GraphQL micronode field definition for the given schema.
	 * 
	 * @param schema
	 * @param project
	 * @return Created field definition
	 */
	public GraphQLFieldDefinition createMicronodeDef(FieldSchema schema, Project project) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(new GraphQLTypeReference(MICRONODE_TYPE_NAME))
			.dataFetcher(env -> {
				GraphFieldContainer container = env.getSource();
				MicronodeGraphField micronodeField = container.getMicronode(schema.getName());
				if (micronodeField != null) {
					return micronodeField.getMicronode();
				}
				return null;
			}).build();
	}

	/**
	 * Generate a new node field definition using the provided field schema.
	 * 
	 * @param schema
	 * @return
	 */
	public GraphQLFieldDefinition createNodeDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).argument(createLanguageTagArg(false)).description(schema.getLabel())
			.type(new GraphQLTypeReference(NODE_TYPE_NAME)).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				GraphFieldContainer source = env.getSource();
				// TODO decide whether we want to reference the default content by default
				NodeGraphField nodeField = source.getNode(schema.getName());
				if (nodeField != null) {
					Node node = nodeField.getNode();
					if (node != null) {
						//Note that we would need to check for micronodes which are not language specific!
						List<String> languageTags = getLanguageArgument(env, source);
						// Check permissions for the linked node
						gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
						NodeGraphFieldContainer container = node.findVersion(gc, languageTags);
						return new NodeContent(node, container, languageTags);
					}
				}
				return null;
			}).build();
	}

}
