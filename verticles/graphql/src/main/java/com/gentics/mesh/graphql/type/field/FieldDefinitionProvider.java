package com.gentics.mesh.graphql.type.field;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacerImpl;
import com.gentics.mesh.core.rest.common.ContainerType;
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

@Singleton
public class FieldDefinitionProvider extends AbstractTypeProvider {

	public static final String BINARY_FIELD_TYPE_NAME = "BinaryField";
	public static final String S3_BINARY_FIELD_TYPE_NAME = "S3BinaryField";

	@Inject
	public MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	@Inject
	public WebRootLinkReplacerImpl linkReplacer;

	@Inject
	public FieldDefinitionProvider(MeshOptions options) {
		super(options);
	}

	public GraphQLObjectType createBinaryFieldType() {
		Builder type = newObject().name(BINARY_FIELD_TYPE_NAME).description("Binary field");

		// .binaryUuid
		type.field(newFieldDefinition().name("binaryUuid").description("UUID of the binary data.").type(GraphQLString).dataFetcher(fetcher -> {
			HibBinaryField field = fetcher.getSource();
			HibBinary binary = field.getBinary();
			return binary == null ? 0 : binary.getUuid();
		}));

		// .fileName
		type.field(newFieldDefinition().name("fileName").description("Filename of the uploaded file.").type(GraphQLString));

		// .width
		type.field(newFieldDefinition().name("width").description("Image width in pixel.").type(GraphQLInt).dataFetcher(fetcher -> {
			HibBinaryField field = fetcher.getSource();
			HibBinary binary = field.getBinary();
			return binary == null ? 0 : binary.getImageWidth();
		}));

		// .height
		type.field(newFieldDefinition().name("height").description("Image height in pixel.").type(GraphQLInt).dataFetcher(fetcher -> {
			HibBinaryField field = fetcher.getSource();
			HibBinary binary = field.getBinary();
			return binary == null ? 0 : binary.getImageHeight();
		}));

		// .sha512sum
		type.field(
			newFieldDefinition().name("sha512sum").description("SHA512 checksum of the binary data.").type(GraphQLString).dataFetcher(fetcher -> {
				HibBinaryField field = fetcher.getSource();
				return field.getBinary().getSHA512Sum();
			}));

		// .fileSize
		type.field(newFieldDefinition().name("fileSize").description("Size of the binary data in bytes").type(GraphQLLong).dataFetcher(fetcher -> {
			HibBinaryField field = fetcher.getSource();
			return field.getBinary().getSize();
		}));

		// .mimeType
		type.field(newFieldDefinition().name("mimeType").description("Mimetype of the binary data").type(GraphQLString));

		// .dominantColor
		type.field(
			newFieldDefinition().name("dominantColor").description("Computed image dominant color").type(GraphQLString).dataFetcher(fetcher -> {
				HibBinaryField field = fetcher.getSource();
				return field.getImageDominantColor();
			}));

		// .focalPoint
		type.field(
			newFieldDefinition().name("focalPoint").description("Focal point of the image.").type(createFocalPointType("FocalPoint")).dataFetcher(fetcher -> {
				HibBinaryField field = fetcher.getSource();
				return field.getImageFocalPoint();
			}));

		// .plainText
		type.field(newFieldDefinition().name("plainText").description("Extracted plain text of the uploaded document.").type(GraphQLString));

		return type.build();
	}

	public GraphQLObjectType createS3BinaryFieldType() {
		Builder type = newObject().name(S3_BINARY_FIELD_TYPE_NAME).description("S3 Binary field");

		// .s3binaryUuid
		type.field(newFieldDefinition().name("s3binaryUuid").description("UUID of the s3 binary data.").type(GraphQLString).dataFetcher(fetcher -> {
			S3HibBinaryField field = fetcher.getSource();
			S3HibBinary s3Binary = field.getBinary();
			return s3Binary == null ? 0 : s3Binary.getUuid();
		}));

		// .fileName
		type.field(newFieldDefinition().name("fileName").description("Filename of the uploaded file.").type(GraphQLString));

		// .width
		type.field(newFieldDefinition().name("width").description("Image width in pixel.").type(GraphQLInt).dataFetcher(fetcher -> {
			S3HibBinaryField field = fetcher.getSource();
			S3HibBinary s3Binary = field.getBinary();
			return s3Binary == null ? 0 : s3Binary.getImageWidth();
		}));

		// .height
		type.field(newFieldDefinition().name("height").description("Image height in pixel.").type(GraphQLInt).dataFetcher(fetcher -> {
			S3HibBinaryField field = fetcher.getSource();
			S3HibBinary s3Binary = field.getBinary();
			return s3Binary == null ? 0 : s3Binary.getImageHeight();
		}));

		// .fileSize
		type.field(newFieldDefinition().name("fileSize").description("Size of the s3 binary data in bytes").type(GraphQLLong).dataFetcher(fetcher -> {
			S3HibBinaryField field = fetcher.getSource();
			S3HibBinary s3Binary = field.getBinary();
			return s3Binary.getSize();
		}));

		// .mimeType
		type.field(newFieldDefinition().name("mimeType").description("Mimetype of the binary data").type(GraphQLString));

		// .dominantColor
		type.field(
				newFieldDefinition().name("dominantColor").description("Computed image dominant color").type(GraphQLString).dataFetcher(fetcher -> {
					S3HibBinaryField field = fetcher.getSource();
					return field.getImageDominantColor();
				}));

		// .focalPoint
		type.field(
				newFieldDefinition().name("focalPoint").description("Focal point of the image.").type(createFocalPointType("S3FocalPoint")).dataFetcher(fetcher -> {
					S3HibBinaryField field = fetcher.getSource();
					return field.getImageFocalPoint();
				}));

		// .plainText
		type.field(newFieldDefinition().name("plainText").description("Extracted plain text of the uploaded document.").type(GraphQLString));

		// .s3ObjectKey
		type.field(newFieldDefinition().name("s3ObjectKey").description("S3 object key. Serves as reference to AWS.").type(GraphQLString));

		return type.build();
	}

	public GraphQLObjectType createFocalPointType(String name) {
		Builder type = newObject().name(name).description("Focal point");

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
				HibFieldContainer container = env.getSource();
				return container.getBinary(schema.getName());
			}).build();

	}

	public GraphQLFieldDefinition createS3BinaryDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(new GraphQLTypeReference(S3_BINARY_FIELD_TYPE_NAME))
				.dataFetcher(env -> {
					HibFieldContainer container = env.getSource();
					return container.getS3Binary(schema.getName());
				}).build();
	}

	public GraphQLFieldDefinition createBooleanDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLBoolean).dataFetcher(env -> {
			HibFieldContainer container = env.getSource();
			HibBooleanField booleanField = container.getBoolean(schema.getName());
			if (booleanField != null) {
				return booleanField.getBoolean();
			}
			return null;
		}).build();
	}

	public GraphQLFieldDefinition createNumberDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLBigDecimal).dataFetcher(env -> {
			HibFieldContainer container = env.getSource();
			HibNumberField numberField = container.getNumber(schema.getName());
			if (numberField != null) {
				return numberField.getNumber();
			}
			return null;
		}).build();
	}

	public GraphQLFieldDefinition createHtmlDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLString).argument(createLinkTypeArg())
			.dataFetcher(env -> {
				HibFieldContainer container = env.getSource();
				HibHtmlField htmlField = container.getHtml(schema.getName());
				if (htmlField != null) {
					Tx tx = Tx.get();
					GraphQLContext gc = env.getContext();
					LinkType type = getLinkType(env);
					String content = htmlField.getHTML();
					return linkReplacer.replace(gc, tx.getBranch(gc)
						.getUuid(), null, content, type, tx.getProject(gc).getName(), Arrays.asList(container.getLanguageTag()));
				}
				return null;
			}).build();
	}

	public GraphQLFieldDefinition createStringDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLString).argument(createLinkTypeArg())
			.dataFetcher(env -> {
				Tx tx = Tx.get();
				HibFieldContainer container = env.getSource();
				HibStringField field = container.getString(schema.getName());
				if (field != null) {
					GraphQLContext gc = env.getContext();
					LinkType type = getLinkType(env);
					String content = field.getString();
					return linkReplacer.replace(gc, tx.getBranch(gc)
						.getUuid(), null, content, type, tx.getProject(gc).getName(), Arrays.asList(container.getLanguageTag()));
				}
				return null;
			}).build();
	}

	public GraphQLFieldDefinition createDateDef(FieldSchema schema) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(GraphQLString).dataFetcher(env -> {
			HibFieldContainer container = env.getSource();
			HibDateField dateField = container.getDate(schema.getName());
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
		graphql.schema.GraphQLFieldDefinition.Builder fieldType = newFieldDefinition()
			.name(schema.getName())
			.description(schema.getLabel())
			.type(new GraphQLList(type))
			.argument(createPagingArgs());
		NodeFilter nodeFilter = NodeFilter.filter(context);

		// Add link resolving arg to html and string lists
		switch (schema.getListType()) {
		case "html":
		case "string":
			fieldType.argument(createLinkTypeArg());
			break;
		case "node":
			fieldType.argument(createNodeVersionArg());
			fieldType.argument(nodeFilter.createFilterArgument());
			break;
		}

		return fieldType.dataFetcher(env -> {
			Tx tx = Tx.get();
			ContentDao contentDao = tx.contentDao();
			HibFieldContainer container = env.getSource();
			GraphQLContext gc = env.getContext();

			switch (schema.getListType()) {
			case "boolean":
				HibBooleanFieldList booleanList = container.getBooleanList(schema.getName());
				if (booleanList == null) {
					return null;
				}
				return booleanList.getList().stream().map(item -> item.getBoolean()).collect(Collectors.toList());
			case "html":
				HibHtmlFieldList htmlList = container.getHTMLList(schema.getName());
				if (htmlList == null) {
					return null;
				}
				return htmlList.getList().stream().map(item -> {
					String content = item.getHTML();
					LinkType linkType = getLinkType(env);
					return linkReplacer.replace(gc, null, null, content, linkType, tx.getProject(gc).getName(),
						Arrays.asList(container.getLanguageTag()));
				}).collect(Collectors.toList());
			case "string":
				HibStringFieldList stringList = container.getStringList(schema.getName());
				if (stringList == null) {
					return null;
				}
				return stringList.getList().stream().map(item -> {
					String content = item.getString();
					LinkType linkType = getLinkType(env);
					return linkReplacer.replace(gc, null, null, content, linkType, tx.getProject(gc).getName(),
						Arrays.asList(container.getLanguageTag()));
				}).collect(Collectors.toList());
			case "number":
				HibNumberFieldList numberList = container.getNumberList(schema.getName());
				if (numberList == null) {
					return null;
				}
				return numberList.getList().stream().map(item -> item.getNumber()).collect(Collectors.toList());
			case "date":
				HibDateFieldList dateList = container.getDateList(schema.getName());
				if (dateList == null) {
					return null;
				}
				return dateList.getList().stream().map(item -> DateUtils.toISO8601(item.getDate(), 0)).collect(Collectors.toList());
			case "node":
				HibNodeFieldList nodeList = container.getNodeList(schema.getName());
				if (nodeList == null) {
					return null;
				}
				Map<String, ?> filterArgument = env.getArgument("filter");
				ContainerType nodeType = getNodeVersion(env);

				Stream<NodeContent> nodes = nodeList.getList().stream().map(item -> {
					HibNode node = item.getNode();
					List<String> languageTags;
					if (container instanceof HibNodeFieldContainer) {
						languageTags = Arrays.asList(container.getLanguageTag());
					} else if (container instanceof HibMicronode) {
						HibMicronode micronode = (HibMicronode) container;
						languageTags = Arrays.asList(micronode.getContainer().getLanguageTag());
					} else {
						throw error(HttpResponseStatus.INTERNAL_SERVER_ERROR, "container can only be NodeGraphFieldContainer or Micronode");
					}
					// TODO we need to add more assertions and check what happens if the itemContainer is null
					HibNodeFieldContainer itemContainer = contentDao.findVersion(node, gc, languageTags, nodeType);
					return new NodeContent(node, itemContainer, languageTags, nodeType);
				});
				if (filterArgument != null) {
					nodes = nodes.filter(nodeFilter.createPredicate(filterArgument));
				}
				return nodes
					.filter(content -> content.getContainer() != null)
					.filter(gc::hasReadPerm)
					.collect(Collectors.toList());
			case "micronode":
				HibMicronodeFieldList micronodeList = container.getMicronodeList(schema.getName());
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
	public GraphQLFieldDefinition createMicronodeDef(FieldSchema schema, HibProject project) {
		return newFieldDefinition().name(schema.getName()).description(schema.getLabel()).type(new GraphQLTypeReference(MICRONODE_TYPE_NAME))
			.dataFetcher(env -> {
				HibFieldContainer container = env.getSource();
				HibMicronodeField micronodeField = container.getMicronode(schema.getName());
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
		return newFieldDefinition()
			.name(schema.getName())
			.argument(createLanguageTagArg(false))
			.argument(createNodeVersionArg())
			.description(schema.getLabel())
			.type(new GraphQLTypeReference(NODE_TYPE_NAME)).dataFetcher(env -> {
				ContentDao contentDao = Tx.get().contentDao();
				GraphQLContext gc = env.getContext();
				HibFieldContainer source = env.getSource();
				ContainerType type = getNodeVersion(env);

				// TODO decide whether we want to reference the default content by default
				HibNodeField nodeField = source.getNode(schema.getName());
				if (nodeField != null) {
					HibNode node = nodeField.getNode();
					if (node != null) {
						//Note that we would need to check for micronodes which are not language specific!
						List<String> languageTags = getLanguageArgument(env, source);
						// Check permissions for the linked node
						gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
						HibNodeFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
						container = gc.requiresReadPermSoft(container, env);
						return new NodeContent(node, container, languageTags, type);
					}
				}
				return null;
			}).build();
	}

}
