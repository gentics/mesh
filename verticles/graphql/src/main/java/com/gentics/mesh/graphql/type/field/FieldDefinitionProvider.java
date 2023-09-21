package com.gentics.mesh.graphql.type.field;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.field.MicronodeFieldTypeProvider.MICRONODE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.scalars.java.JavaPrimitives.GraphQLBigDecimal;
import static graphql.scalars.java.JavaPrimitives.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;

import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
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
import com.gentics.mesh.graphql.dataloader.NodeDataLoader;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import com.gentics.mesh.graphql.type.NodeTypeProvider;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.image.CropMode;
import com.gentics.mesh.util.DateUtils;
import com.google.common.base.Functions;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Promise;

@Singleton
public class FieldDefinitionProvider extends AbstractTypeProvider {

	public static final String BINARY_FIELD_TYPE_NAME = "BinaryField";
	public static final String S3_BINARY_FIELD_TYPE_NAME = "S3BinaryField";

	/**
	 * Key for the data loader, which efficiently replaces links in contents
	 */
	public static final String LINK_REPLACER_DATA_LOADER_KEY = "linkReplaceLoader";

	/**
	 * Key for the data loader for boolean list field values
	 */
	public static final String BOOLEAN_LIST_VALUES_DATA_LOADER_KEY = "booleanListLoader";

	/**
	 * Key for the data loader for date list field values
	 */
	public static final String DATE_LIST_VALUES_DATA_LOADER_KEY = "dateListLoader";

	/**
	 * Key for the data loader for number list field values
	 */
	public static final String NUMBER_LIST_VALUES_DATA_LOADER_KEY = "numberListLoader";

	/**
	 * Key for the data loader for html list field values
	 */
	public static final String HTML_LIST_VALUES_DATA_LOADER_KEY = "htmlListLoader";

	/**
	 * Key for the data loader for string list field values
	 */
	public static final String STRING_LIST_VALUES_DATA_LOADER_KEY = "stringListLoader";

	protected final MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	protected final WebRootLinkReplacerImpl linkReplacer;

	/**
	 * Partition the keys by context and call the consumer for each part
	 * @param <T> result type
	 * @param keyContexts map of keys to contexts
	 * @param consumer consumer
	 */
	private static void partitioningByLinkTypeAndText(List<DataLoaderKey> keys, BiConsumer<Pair<LinkType, String>, Set<String>> consumer) {
		Map<Pair<LinkType, String>, Set<String>> partitionedKeys = keys.stream()
			.map(key -> Pair.of(Pair.of(key.linkType, key.languageTag), key.content))
			.collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toSet())));

		partitionedKeys.forEach(consumer::accept);
	}

	/**
	 * DataLoader implementation that replaces links in contents
	 */
	public BatchLoaderWithContext<DataLoaderKey, String> LINK_REPLACER_LOADER;

	/**
	 * Generic list field data loader
	 * @param <U> type of the field values
	 * @param <V> type fo the returned (rendered) field values
	 * @param keys list of listUuids for which the field values need to be loaded
	 * @param dataFunction function, that loads the list field values
	 * @param transformer transformer for transforming the loaded values into the returned values (e.g. for date formatting)
	 * @return CompletionStage for the lists of loaded list values
	 */
	private static <U, V> CompletionStage<List<List<V>>> listValueDataLoader(List<String> keys, Function<List<String>, Map<String,List<U>>> dataFunction, Function<List<List<U>>, List<List<V>>> transformer) {
		Promise<List<List<V>>> promise = Promise.promise();

		Map<String,List<U>> listValuesMap = dataFunction.apply(keys);
		List<List<U>> valueLists = keys.stream().map(key -> listValuesMap.getOrDefault(key, Collections.emptyList())).collect(Collectors.toList());

		promise.complete(transformer.apply(valueLists));
		return promise.future().toCompletionStage();
	}

	/**
	 * DataLoader implementation for values of boolean lists
	 */
	public BatchLoaderWithContext<String, List<Boolean>> BOOLEAN_LIST_VALUE_LOADER = (keys, environment) -> {
		ContentDao contentDao = Tx.get().contentDao();
		return listValueDataLoader(keys, contentDao::getBooleanListFieldValues, Functions.identity());
	};

	/**
	 * DataLoader implementation for values of date lists
	 */
	public BatchLoaderWithContext<String, List<String>> DATE_LIST_VALUE_LOADER = (keys, environment) -> {
		ContentDao contentDao = Tx.get().contentDao();

		Function<List<List<Long>>, List<List<String>>> dateFormatter = orig -> {
			return orig.stream().map(origList -> origList.stream().map(date -> DateUtils.toISO8601(date, 0)).collect(Collectors.toList())).collect(Collectors.toList());
		};

		return listValueDataLoader(keys, contentDao::getDateListFieldValues, dateFormatter);
	};

	/**
	 * DataLoader implementation for values of number lists
	 */
	public BatchLoaderWithContext<String, List<Number>> NUMBER_LIST_VALUE_LOADER = (keys, environment) -> {
		ContentDao contentDao = Tx.get().contentDao();
		return listValueDataLoader(keys, contentDao::getNumberListFieldValues, Functions.identity());
	};

	/**
	 * DataLoader implementation for values of string lists
	 */
	public BatchLoaderWithContext<String, List<String>> HTML_LIST_VALUE_LOADER = (keys, environment) -> {
		ContentDao contentDao = Tx.get().contentDao();
		return listValueDataLoader(keys, contentDao::getHtmlListFieldValues, Functions.identity());
	};

	/**
	 * DataLoader implementation for values of string lists
	 */
	public BatchLoaderWithContext<String, List<String>> STRING_LIST_VALUE_LOADER = (keys, environment) -> {
		ContentDao contentDao = Tx.get().contentDao();
		return listValueDataLoader(keys, contentDao::getStringListFieldValues, Functions.identity());
	};

	@Inject
	public FieldDefinitionProvider(MeshOptions options, MicronodeFieldTypeProvider micronodeFieldTypeProvider, WebRootLinkReplacerImpl linkReplacer) {
		super(options);
		this.micronodeFieldTypeProvider = micronodeFieldTypeProvider;
		this.linkReplacer = linkReplacer;
		LINK_REPLACER_LOADER = (keys, environment) -> {
			Promise<List<String>> promise = Promise.promise();
			GraphQLContext gc = environment.getContext();
			String branchUuid = Tx.get().getBranch(gc).getUuid();
			String projectName = Tx.get().getProject(gc).getName();

			Map<DataLoaderKey, String> resolvedByContent = new HashMap<>();

			partitioningByLinkTypeAndText(keys, (pair, contents) -> {
				LinkType type = pair.getLeft();
				String languageTag = pair.getRight();

				Map<String, String> replacedByContent = linkReplacer.replaceMany(gc, branchUuid, null, contents, type, projectName, languageTag);
				for (Entry<String, String> entry : replacedByContent.entrySet()) {
					String content = entry.getKey();
					String replaced = entry.getValue();

					DataLoaderKey key = new DataLoaderKey(content, type, languageTag);
					resolvedByContent.put(key, replaced);
				}
			});

			List<String> resolvedContents = keys.stream().map(key -> resolvedByContent.getOrDefault(key, "")).collect(Collectors.toList());
			promise.complete(resolvedContents);

			return promise.future().toCompletionStage();
		};
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

		// .s3objectKey
		type.field(
			newFieldDefinition().name("s3ObjectKey").description("S3 object key of the S3 storage binary data.").type(GraphQLString).dataFetcher(fetcher -> {
				S3HibBinaryField field = fetcher.getSource();
				return field.getBinary().getS3ObjectKey();
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

		// .variants
		type.field(newFieldDefinition().name("variants").description("Image binary manipulation variants, if applicable").type(new GraphQLList(createImageVariantType("variant"))).dataFetcher(fetcher -> {
			HibBinaryField field = fetcher.getSource();
			return field.getImageVariants().list();
		}));

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

	public GraphQLObjectType createImageVariantType(String name) {
		Builder type = newObject().name(name).description("Image variant");

		// .width
		type.field(newFieldDefinition().name("width").description("Variant width").type(GraphQLInt)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getWidth();
			}));

		// .height
		type.field(newFieldDefinition().name("height").description("Variant height").type(GraphQLInt)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getHeight();
			}));

		// .cropX
		type.field(newFieldDefinition().name("cropX").description("Variant crop X starting point").type(GraphQLInt)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getCropStartX();
			}));

		// .cropY
		type.field(newFieldDefinition().name("cropY").description("Variant crop Y starting point").type(GraphQLInt)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getCropStartY();
			}));

		// .cropWidth
		type.field(newFieldDefinition().name("cropWidth").description("Variant crop width").type(GraphQLInt)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getCropWidth();
			}));

		// .cropHeigth
		type.field(newFieldDefinition().name("cropHeight").description("Variant crop heigth").type(GraphQLInt)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getCropHeight();
			}));

		// .cropMode
		type.field(newFieldDefinition().name("cropMode").description("Variant crop mode").type(GraphQLString)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return String.valueOf(variant.getCropMode());
			}));

		// .resizeMode
		type.field(newFieldDefinition().name("resizeMode").description("Variant resize mode").type(GraphQLString)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return String.valueOf(variant.getResizeMode());
			}));

		// .focalPoint
		type.field(
			newFieldDefinition().name("focalPoint").description("Focal point of the variant.").type(createFocalPointType("ImageVariantFocalPoint")).dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getFocalPoint();
			}));

		// .focalZoom
		type.field(newFieldDefinition().name("zoom").description("Focal zoom factor").type(GraphQLFloat)
			.dataFetcher(fetcher -> {
				HibImageVariant variant = fetcher.getSource();
				return variant.getFocalPointZoom();
			}));

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

					if (type != null && type != LinkType.OFF) {
						DataLoader<DataLoaderKey, String> linkedContentLoader = env.getDataLoader(FieldDefinitionProvider.LINK_REPLACER_DATA_LOADER_KEY);
						return linkedContentLoader.load(new DataLoaderKey(content, type, container.getLanguageTag()));
					} else {
						return content;
					}
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

					if (type != null && type != LinkType.OFF) {
						DataLoader<DataLoaderKey, String> linkedContentLoader = env.getDataLoader(FieldDefinitionProvider.LINK_REPLACER_DATA_LOADER_KEY);
						return linkedContentLoader.load(new DataLoaderKey(content, type, container.getLanguageTag()));
					} else {
						return content;
					}
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
			.argument(createPagingArgs(false));
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

				String booleanListUuid = booleanList.getUuid();
				if (contentDao.supportsPrefetchingListFieldValues() && !StringUtils.isEmpty(booleanListUuid)) {
					DataLoader<String, List<Boolean>> booleanListValueLoader = env.getDataLoader(FieldDefinitionProvider.BOOLEAN_LIST_VALUES_DATA_LOADER_KEY);
					return booleanListValueLoader.load(booleanListUuid);
				} else {
					return booleanList.getList().stream().map(item -> item.getBoolean()).collect(Collectors.toList());
				}
			case "html":
				HibHtmlFieldList htmlList = container.getHTMLList(schema.getName());
				if (htmlList == null) {
					return null;
				}

				String htmlListUuid = htmlList.getUuid();
				if (contentDao.supportsPrefetchingListFieldValues() && !StringUtils.isEmpty(htmlListUuid)) {
					LinkType linkType = getLinkType(env);
					DataLoader<String, List<String>> htmlListValueLoader = env.getDataLoader(FieldDefinitionProvider.HTML_LIST_VALUES_DATA_LOADER_KEY);

					if (linkType != null && linkType != LinkType.OFF) {
						String projectName = tx.getProject(gc).getName();
						List<String> languageTags = Arrays.asList(container.getLanguageTag());
						return htmlListValueLoader.load(htmlListUuid).thenApply(contents -> {
							return contents.stream().map(content -> linkReplacer.replace(gc, null, null, content,
									linkType, projectName, languageTags)).collect(Collectors.toList());
						});
					} else {
						return htmlListValueLoader.load(htmlListUuid);
					}
				} else {
					return htmlList.getList().stream().map(item -> {
						String content = item.getHTML();
						LinkType linkType = getLinkType(env);
						return linkReplacer.replace(gc, null, null, content, linkType, tx.getProject(gc).getName(),
								Arrays.asList(container.getLanguageTag()));
					}).collect(Collectors.toList());
				}
			case "string":
				HibStringFieldList stringList = container.getStringList(schema.getName());
				if (stringList == null) {
					return null;
				}

				String stringListUuid = stringList.getUuid();
				if (contentDao.supportsPrefetchingListFieldValues() && !StringUtils.isEmpty(stringListUuid)) {
					LinkType linkType = getLinkType(env);
					DataLoader<String, List<String>> stringListValueLoader = env.getDataLoader(FieldDefinitionProvider.STRING_LIST_VALUES_DATA_LOADER_KEY);

					if (linkType != null && linkType != LinkType.OFF) {
						String projectName = tx.getProject(gc).getName();
						List<String> languageTags = Arrays.asList(container.getLanguageTag());
						return stringListValueLoader.load(stringListUuid).thenApply(contents -> {
							return contents.stream().map(content -> linkReplacer.replace(gc, null, null, content,
									linkType, projectName, languageTags)).collect(Collectors.toList());
						});
					} else {
						return stringListValueLoader.load(stringListUuid);
					}
				} else {
					return stringList.getList().stream().map(item -> {
						String content = item.getString();
						LinkType linkType = getLinkType(env);
						return linkReplacer.replace(gc, null, null, content, linkType, tx.getProject(gc).getName(),
								Arrays.asList(container.getLanguageTag()));
					}).collect(Collectors.toList());
				}
			case "number":
				HibNumberFieldList numberList = container.getNumberList(schema.getName());
				if (numberList == null) {
					return null;
				}

				String numberListUuid = numberList.getUuid();
				if (contentDao.supportsPrefetchingListFieldValues() && !StringUtils.isEmpty(numberListUuid)) {
					DataLoader<String, List<Number>> numberListValueLoader = env.getDataLoader(FieldDefinitionProvider.NUMBER_LIST_VALUES_DATA_LOADER_KEY);
					return numberListValueLoader.load(numberListUuid);
				} else {
					return numberList.getList().stream().map(item -> item.getNumber()).collect(Collectors.toList());
				}
			case "date":
				HibDateFieldList dateList = container.getDateList(schema.getName());
				if (dateList == null) {
					return null;
				}

				String dateListUuid = dateList.getUuid();
				if (contentDao.supportsPrefetchingListFieldValues() && !StringUtils.isEmpty(dateListUuid)) {
					DataLoader<String, List<String>> dateListValueLoader = env.getDataLoader(FieldDefinitionProvider.DATE_LIST_VALUES_DATA_LOADER_KEY);
					return dateListValueLoader.load(dateListUuid);
				} else {
					return dateList.getList().stream().map(item -> DateUtils.toISO8601(item.getDate(), 0)).collect(Collectors.toList());
				}
			case "node":
				HibNodeFieldList nodeList = container.getNodeList(schema.getName());
				if (nodeList == null) {
					return null;
				}
				Map<String, ?> filterArgument = env.getArgument("filter");
				ContainerType nodeType = getNodeVersion(env);

				Stream<NodeContent> nodes = nodeList.getList().stream().map(item -> {
					HibNode node = item.getNode();
					if (node == null) {
						return null;
					}
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
				}).filter(Objects::nonNull);
				if (filterArgument != null) {
					nodes = nodes.filter(nodeFilter.createPredicate(filterArgument));
				}
				return nodes
					.filter(content -> content.getContainer() != null)
					.filter(content1 -> gc.hasReadPerm(content1, nodeType))
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

						NodeDataLoader.Context context = new NodeDataLoader.Context(type, languageTags, Optional.empty(), getPagingInfo(env));
						DataLoader<HibNode, List<HibNodeFieldContainer>> contentLoader = env.getDataLoader(NodeDataLoader.CONTENT_LOADER_KEY);
						return contentLoader.load(node, context).thenApply((containers) -> {
							HibNodeFieldContainer container = NodeTypeProvider.getContainerWithFallback(languageTags, containers);
							return NodeTypeProvider.createNodeContentWithSoftPermissions(env, gc, node, languageTags, type, container);
						});
					}
				}
				return null;
			}).build();
	}

	/**
	 * Keys for the DataLoader (containing content, link type and language)
	 */
	public static class DataLoaderKey {
		/**
		 * Content
		 */
		protected final String content;

		/**
		 * Link Type
		 */
		protected final LinkType linkType;

		/**
		 * Language
		 */
		protected final String languageTag;

		/**
		 * Create an instance
		 * @param content content
		 * @param linkType link type
		 * @param languageTag language
		 */
		public DataLoaderKey(String content, LinkType linkType, String languageTag) {
			this.content = content;
			this.linkType = linkType;
			this.languageTag = languageTag;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DataLoaderKey) {
				DataLoaderKey other = (DataLoaderKey) obj;
				return other.linkType == linkType && StringUtils.equals(other.languageTag, languageTag) && StringUtils.equals(other.content, content);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(content, linkType, languageTag);
		}
	}
}
