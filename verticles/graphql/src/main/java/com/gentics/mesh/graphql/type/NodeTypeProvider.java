package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.graphql.filter.NodeReferenceFilter.nodeReferenceFilter;
import static com.gentics.mesh.graphql.type.NodeReferenceTypeProvider.NODE_REFERENCE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.UserTypeProvider.USER_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.model.NodeReferenceIn;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;
import com.gentics.mesh.handler.Versioned;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.search.index.node.NodeSearchHandler;

import graphql.GraphQLException;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;

/**
 * Type provider for the node type. Internally this will map partially to {@link Node} and {@link NodeGraphFieldContainer} vertices.
 */
@Singleton
public class NodeTypeProvider extends AbstractTypeProvider {

	public static final String NODE_TYPE_NAME = "Node";

	public static final String NODE_PAGE_TYPE_NAME = "NodesPage";

	public static final String NODE_CONTENT_VERSION_TYPE_NAME = "ContentVersion";

	public static final String NODE_FIELDS_TYPE_NAME = "Fields";

	@Inject
	public NodeSearchHandler nodeSearchHandler;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider tagTypeProvider;

	@Inject
	public BootstrapInitializer boot;

	@Inject
	public FieldDefinitionProvider fields;

	@Inject
	public NodeTypeProvider(MeshOptions options) {
		super(options);
	}

	/**
	 * Fetcher for the parent node reference of a node.
	 * 
	 * @param env
	 * @return
	 */
	public Object parentNodeFetcher(DataFetchingEnvironment env) {
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		GraphQLContext gc = env.getContext();
		String uuid = gc.getBranch().getUuid();
		Node parentNode = content.getNode().getParentNode(uuid);
		// The project root node can have no parent. Lets check this and exit early.
		if (parentNode == null) {
			return null;
		}
		gc.requiresPerm(parentNode, READ_PERM, READ_PUBLISHED_PERM);

		List<String> languageTags = getLanguageArgument(env, content);
		return new NodeContent(parentNode, parentNode.findVersion(gc, languageTags), languageTags);
	}

	public Object nodeLanguageFetcher(DataFetchingEnvironment env) {
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		List<String> languageTags = getLanguageArgument(env);
		GraphQLContext gc = env.getContext();

		Node node = content.getNode();
		Branch branch = gc.getBranch();
		NodeGraphFieldContainer container = node.findVersion(gc, languageTags);
		// There might not be a container for the selected language (incl. fallback language)
		if (container == null) {
			return null;
		}

		// Check whether the user is allowed to read the published container
		boolean isPublished = container.isPublished(branch.getUuid());
		if (isPublished) {
			gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
		} else {
			// Otherwise the container is a draft and we need to use the regular read permission
			gc.requiresPerm(node, READ_PERM);
		}
		return new NodeContent(node, container, languageTags);
	}

	public Object breadcrumbFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}

		return content.getNode().getBreadcrumbNodes(gc).stream().map(node -> {
			List<String> languageTags = getLanguageArgument(env, content);
			return new NodeContent(node, node.findVersion(gc, languageTags), languageTags);
		}).collect(Collectors.toList());
	}

	public Object languagesFetcher(DataFetchingEnvironment env) {
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		GraphQLContext gc = env.getContext();
		Branch branch = gc.getBranch();
		ContainerType type = ContainerType.forVersion(gc.getVersioningParameters().getVersion());

		Stream<? extends NodeGraphFieldContainer> stream = StreamSupport
			.stream(content.getNode().getGraphFieldContainers(branch, type).spliterator(), false);
		return stream.map(item -> {
			return new NodeContent(content.getNode(), item, content.getLanguageFallback());
		}).collect(Collectors.toList());
	}

	public Versioned<GraphQLType> createType(GraphQLContext context) {
		return Versioned.<GraphQLType>
		since(1, () -> {
			GraphQLObjectType.Builder nodeType = newObject();
			nodeType.name(NODE_TYPE_NAME);
			nodeType.description(
				"A Node is the basic building block for contents. Nodes can contain multiple language specific contents. These contents contain the fields with the actual content.");
			interfaceTypeProvider.addCommonFields(nodeType, true);
			nodeType.fields(createNodeInterfaceFields(context).forVersion(context));
			return nodeType.build();
		}).since(2, () -> {
			GraphQLInterfaceType.Builder nodeType = newInterface();

			nodeType.name(NODE_TYPE_NAME);
			nodeType.description(
				"A Node is the basic building block for contents. Nodes can contain multiple language specific contents. These contents contain the fields with the actual content.");
			interfaceTypeProvider.addCommonFields(nodeType, true);

			nodeType.typeResolver(env -> {
				NodeContent content = env.getObject();
				String schemaName = content.getNode().getSchemaContainer().getName();
				return env.getSchema().getObjectType(schemaName);
			});

			nodeType.fields(createNodeInterfaceFields(context).forVersion(context));

			return nodeType.build();
		}).build();
	}

	public Versioned<List<GraphQLFieldDefinition>> createNodeInterfaceFields(GraphQLContext context) {
		List<GraphQLFieldDefinition> baseFields = Arrays.asList(
			// .project
			newFieldDefinition().name("project").description("Project of the node").type(new GraphQLTypeReference("Project")).dataFetcher((
				env) -> {
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				Project projectOfNode = content.getNode().getProject();
				return gc.requiresPerm(projectOfNode, READ_PERM);
			}).build(),

			// .breadcrumb
			newFieldDefinition().name("breadcrumb").description("Breadcrumb of the node").type(new GraphQLList(new GraphQLTypeReference(
				NODE_TYPE_NAME))).dataFetcher(this::breadcrumbFetcher).build(),

			// .availableLanguages
			newFieldDefinition().name("availableLanguages").description("List all available languages for the node").type(new GraphQLList(
				GraphQLString)).dataFetcher(env -> {
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				// TODO handle branch!
				return content.getNode().getAvailableLanguageNames();
			}).build(),

			// .languages
			newFieldDefinition().name("languages").description("Load all languages of the node").type(new GraphQLList(
				new GraphQLTypeReference(NODE_TYPE_NAME))).dataFetcher(this::languagesFetcher).build(),

			// .child
			newFieldDefinition().name("child").description("Resolve a webroot path to a specific child node.").argument(createPathArg())
				.type(new GraphQLTypeReference(NODE_TYPE_NAME)).dataFetcher(env -> {
				String nodePath = env.getArgument("path");
				if (nodePath != null) {
					GraphQLContext gc = env.getContext();

					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					Node node = content.getNode();
					// Resolve the given path and return the found container
					Branch branch = gc.getBranch();
					String branchUuid = branch.getUuid();
					ContainerType type = ContainerType.forVersion(gc.getVersioningParameters().getVersion());
					Stack<String> pathStack = new Stack<>();
					pathStack.add(nodePath);
					Path path = new Path();
					try {
						node.resolvePath(branchUuid, type, path, pathStack);
					} catch (GenericRestException e) {
						// Check whether the path could not be resolved
						if (e.getStatus() == NOT_FOUND) {
							return null;
						} else {
							throw e;
						}
					}
					// Check whether the path could not be resolved. In those cases the segments is empty
					if (path.getSegments().isEmpty()) {
						return null;
					}
					// Otherwise return the last segment.
					PathSegment lastSegment = path.getSegments().get(path.getSegments().size() - 1);
					NodeGraphFieldContainer container = lastSegment.getContainer();
					return new NodeContent(null, container, Arrays.asList(container.getLanguageTag()));
				}
				return null;
			}).build(),

			// .children
			newPagingFieldWithFetcherBuilder("children", "Load child nodes of the node.", (env) -> {
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}

				List<String> languageTags = getLanguageArgument(env, content);

				Stream<NodeContent> nodes = content.getNode().getChildrenStream(gc)
					.map(item -> new NodeContent(item, item.findVersion(gc, languageTags), languageTags))
					.filter(nodeContentFilter.forVersion(gc));

				return applyNodeFilter(env, nodes);
			}, NODE_PAGE_TYPE_NAME)
				.argument(createLanguageTagArg(false))
				.argument(NodeFilter.filter(context).createFilterArgument()).build(),

			// .parent

			newFieldDefinition()
				.name("parent")
				.description("Parent node")
				.type(new GraphQLTypeReference(NODE_TYPE_NAME))
				.argument(createLanguageTagArg(false))
				.dataFetcher(this::parentNodeFetcher).build(),

			// .tags
			newFieldDefinition().name("tags").argument(createPagingArgs()).type(new GraphQLTypeReference(TAG_PAGE_TYPE_NAME)).dataFetcher((
				env) -> {
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				Node node = content.getNode();
				return node.getTags(gc.getUser(), getPagingInfo(env), gc.getBranch());
			}).build(),

			// TODO Fix name confusion and check what version of schema should be used to determine this type
			// .isContainer
			newFieldDefinition().name("isContainer").description("Check whether the node can have subnodes via children").type(
				GraphQLBoolean).dataFetcher((env) -> {
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				Node node = content.getNode();
				return node.getSchemaContainer().getLatestVersion().getSchema().getContainer();
			}).build(),

			// .referencedBy
			newFieldDefinition()
				.name("referencedBy")
				.description("Loads nodes that reference this node.")
				.argument(nodeReferenceFilter(context).createFilterArgument())
				.type(new GraphQLTypeReference(NODE_REFERENCE_PAGE_TYPE_NAME))
				.dataFetcher(env -> {
					NodeContent content = env.getSource();

					Stream<NodeReferenceIn> stream = NodeReferenceIn.fromContent(context, content);
					Map<String, ?> filterInput = env.getArgument("filter");
					if (filterInput != null) {
						stream = stream.filter(nodeReferenceFilter(context).createPredicate(filterInput));
					}

					return new DynamicStreamPageImpl<>(stream, getPagingInfo(env));
				}).build(),

			// Content specific fields

			// .node

			newFieldDefinition()
				.name("node")
				.description("Load the node with a different language.")
				.argument(createLanguageTagArg(false))
				.dataFetcher(this::nodeLanguageFetcher)
				.type(new GraphQLTypeReference(NODE_TYPE_NAME))
				.build(),

			// .path
			newFieldDefinition().name("path").description("Webroot path of the content.").type(GraphQLString).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				return content.getContainer().map(container -> {
					ContainerType containerType = ContainerType.forVersion(gc.getVersioningParameters().getVersion());
					String branchUuid = gc.getBranch().getUuid();
					String languageTag = container.getLanguageTag();
					return container.getParentNode().getPath(gc, branchUuid, containerType, languageTag);
				}).orElse(null);
			}).build(),

			// .edited
			newFieldDefinition().name("edited").description("ISO8601 formatted edit timestamp.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer()
					.map(EditorTrackingVertex::getLastEditedDate)
					.orElse(null);
			}).build(),

			// .editor
			newFieldDefinition().name("editor").description("Editor of the element").type(new GraphQLTypeReference(USER_TYPE_NAME))
				.dataFetcher(this::editorFetcher).build(),

			// .schema
			newFieldDefinition().name("schema").description("Schema of the node").type(new GraphQLTypeReference(SCHEMA_TYPE_NAME))
				.dataFetcher(env -> {
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					return content.getContainer()
						.map(NodeGraphFieldContainer::getSchemaContainerVersion)
						.orElse(null);
				}).build(),

			// .isPublished
			newFieldDefinition().name("isPublished").description("Check whether the content is published.").type(GraphQLBoolean)
				.dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					return content.getContainer()
						.map(container -> container.isPublished(gc.getBranch().getUuid()))
						.orElse(null);
				}).build(),

			// .isDraft
			newFieldDefinition().name("isDraft").description("Check whether the content is a draft.").type(GraphQLBoolean).dataFetcher(
				env -> {
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					return content.getContainer()
						.map(container -> container.isDraft(gc.getBranch().getUuid()))
						.orElse(null);
				}).build(),

			// .version
			newFieldDefinition().name("version").description("Version of the content.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer()
					.map(container -> container.getVersion().getFullVersion())
					.orElse(null);
			}).build(),

			// .versions
			newFieldDefinition().name("versions").description("List of versions of the node.")
				.argument(createSingleLanguageTagArg(true))
				.type(GraphQLList.list(GraphQLTypeReference.typeRef(NODE_CONTENT_VERSION_TYPE_NAME))).dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					String languageTag = getSingleLanguageArgument(env);
					NodeContent content = env.getSource();
					Node node = content.getNode();
					if (node == null) {
						return null;
					}
					return node.getGraphFieldContainers(gc.getBranch(), DRAFT).stream().filter(c -> {
						String lang = c.getLanguageTag();
						return lang.equals(languageTag);
					}).findFirst().map(NodeGraphFieldContainer::versions).orElse(null);
			}).build(),

			// .language
			newFieldDefinition().name("language").description("The language of this content.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer()
					.map(BasicFieldContainer::getLanguageTag)
					.orElse(null);
			}).build(),

			// .displayName
			newFieldDefinition().name("displayName").description("The value of the display field.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer()
					.map(NodeGraphFieldContainer::getDisplayFieldValue)
					.orElse(null);
			}).build()
		);

		Supplier<List<GraphQLFieldDefinition>> withNodeFieldsSupplier = () -> {
			List<GraphQLFieldDefinition> withNodeFields = new ArrayList<>(baseFields);

			withNodeFields.add(newFieldDefinition()
				.name("fields")
				.description("Contains the fields of the content.")
				.type(createFieldsUnionType(context).forVersion(context))
				.deprecate("Usage of fields has changed in /api/v2. See https://github.com/gentics/mesh/issues/428")
				.dataFetcher(env -> {
				// The fields can be accessed via the container so we can directly pass it along.
				NodeContent content = env.getSource();
				return content.getContainer().orElse(null);
			}).build());

			return withNodeFields;
		};

		return Versioned
			.since(1, withNodeFieldsSupplier)
			.since(2, baseFields)
			.build();
	}

	public GraphQLObjectType createVersionInfoType() {
		GraphQLObjectType.Builder builder = newObject();
		builder.name(NODE_CONTENT_VERSION_TYPE_NAME);
		builder.description("Content version information");

		// .version
		builder.field(newFieldDefinition().name("version").description("Version of the content").type(GraphQLString).dataFetcher((env) -> {
			NodeGraphFieldContainer version = env.getSource();
			return version.getVersion();
		}));

		// .draft
		builder.field(newFieldDefinition().name("draft").description("Flag that indicates whether the version is used as a draft.")
			.type(GraphQLBoolean).dataFetcher((env) -> {
				GraphQLContext gc = env.getContext();
				String branchUuid = gc.getBranch().getUuid();
				NodeGraphFieldContainer version = env.getSource();
				return version.isDraft(branchUuid);
			}));

		// .published
		builder.field(newFieldDefinition().name("published").description("Flag that indicates whether the version is used as a published version.")
			.type(GraphQLBoolean).dataFetcher((env) -> {
				GraphQLContext gc = env.getContext();
				String branchUuid = gc.getBranch().getUuid();
				NodeGraphFieldContainer version = env.getSource();
				return version.isPublished(branchUuid);
			}));

		// .branchRoot
		builder.field(newFieldDefinition().name("branchRoot")
			.description("Flag that indicates whether the version is used as a branch root version for a branch.").type(GraphQLBoolean)
			.dataFetcher((env) -> {
				NodeGraphFieldContainer version = env.getSource();
				return version.isInitial();
			}));

		// .created
		builder.field(
			newFieldDefinition().name("created").description("ISO8601 formatted created date string").type(GraphQLString).dataFetcher(env -> {
				NodeGraphFieldContainer source = env.getSource();
				return source.getLastEditedDate();
			}));

		// .creator
		builder.field(
			newFieldDefinition().name("creator").description("Creator of the version").type(new GraphQLTypeReference("User")).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				NodeGraphFieldContainer source = env.getSource();
				return gc.requiresPerm(source.getEditor(), READ_PERM);
			}));

		return builder.build();
	}

	public Object editorFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		return content.getContainer()
			.map(container -> gc.requiresPerm(container.getEditor(), READ_PERM))
			.orElse(null);
	}

	/**
	 * Invoke a elasticsearch using the provided query and return a page of found containers.
	 *
	 * @param gc
	 * @param query
	 * @param pagingInfo
	 * @return
	 */
	public Page<? extends NodeContent> handleContentSearch(GraphQLContext gc, String query, PagingParameters pagingInfo) {
		try {
			return nodeSearchHandler.handleContainerSearch(gc, query, pagingInfo, READ_PERM, READ_PUBLISHED_PERM);
		} catch (MeshConfigurationException | InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Invoke the given query and return a page of nodes.
	 *
	 * @param gc
	 * @param query
	 *            Elasticsearch query
	 * @param pagingInfo
	 * @return
	 */
	public Page<? extends Node> handleSearch(GraphQLContext gc, String query, PagingParameters pagingInfo) {
		try {
			return nodeSearchHandler.query(gc, query, pagingInfo, READ_PERM, READ_PUBLISHED_PERM);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Versioned<GraphQLUnionType> createFieldsUnionType(GraphQLContext context) {
		GraphQLObjectType[] typeArray = generateSchemaFieldTypes(context).forVersion(context).toArray(new GraphQLObjectType[0]);

		GraphQLUnionType fieldType = newUnionType().name(NODE_FIELDS_TYPE_NAME).possibleTypes(typeArray).description("Fields of the node.")
			.typeResolver(env -> {
				Object object = env.getObject();
				if (object instanceof NodeGraphFieldContainer) {
					NodeGraphFieldContainer fieldContainer = (NodeGraphFieldContainer) object;
					String schemaName = fieldContainer.getSchemaContainerVersion().getName();
					GraphQLObjectType foundType = env.getSchema().getObjectType(schemaName);
					if (foundType == null) {
						throw new GraphQLException("The type for the schema with name {" + schemaName
							+ "} could not be found. Maybe the schema is not linked to the project.");
					}
					return foundType;
				}
				return null;
			}).build();

		return Versioned.forVersion(1, fieldType).build();
	}

	/**
	 * Generate a map of all schema types which correspond to schemas which are part of the project/branch.
	 *
	 * @param context
	 * @return
	 */
	public Versioned<List<GraphQLObjectType>> generateSchemaFieldTypes(GraphQLContext context) {
		return Versioned
			.forVersion(1, () -> generateSchemaFieldTypesV1(context))
			.since(2, () -> generateSchemaFieldTypesV2(context))
			.build();
	}

	private List<GraphQLObjectType> generateSchemaFieldTypesV1(GraphQLContext context) {
		Project project = context.getProject();
		List<GraphQLObjectType> schemaTypes = new ArrayList<>();
		for (SchemaContainer container : project.getSchemaContainerRoot().findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			GraphQLObjectType.Builder root = newObject();
			// TODO remove this workaround
			root.name(schema.getName().replaceAll("-", "_"));
			root.description(schema.getDescription());

			// TODO add link resolving argument / code
			for (FieldSchema fieldSchema : schema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
					case STRING:
						root.field(fields.createStringDef(fieldSchema));
						break;
					case HTML:
						root.field(fields.createHtmlDef(fieldSchema));
						break;
					case NUMBER:
						root.field(fields.createNumberDef(fieldSchema));
						break;
					case DATE:
						root.field(fields.createDateDef(fieldSchema));
						break;
					case BOOLEAN:
						root.field(fields.createBooleanDef(fieldSchema));
						break;
					case NODE:
						root.field(fields.createNodeDef(fieldSchema));
						break;
					case BINARY:
						root.field(fields.createBinaryDef(fieldSchema));
						break;
					case LIST:
						ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
						root.field(fields.createListDef(context, listFieldSchema));
						break;
					case MICRONODE:
						root.field(fields.createMicronodeDef(fieldSchema, project));
						break;
				}
			}
			GraphQLObjectType type = root.build();
			schemaTypes.add(type);
		}
		return schemaTypes;
	}


	private List<GraphQLObjectType> generateSchemaFieldTypesV2(GraphQLContext context) {
		Project project = context.getProject();

		return project.getSchemaContainerRoot().findAll().stream().map(container -> {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			GraphQLObjectType.Builder root = newObject();
			root.withInterface(GraphQLTypeReference.typeRef(NODE_TYPE_NAME));
			root.name(schema.getName());
			root.description(schema.getDescription());

			GraphQLFieldDefinition.Builder fieldsField = GraphQLFieldDefinition.newFieldDefinition();
			GraphQLObjectType.Builder fieldsType = newObject();
			fieldsType.name(nodeTypeName(schema.getName()));
			fieldsField.dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer().orElse(null);
			});

			// TODO add link resolving argument / code
			for (FieldSchema fieldSchema : schema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
					case STRING:
						fieldsType.field(fields.createStringDef(fieldSchema));
						break;
					case HTML:
						fieldsType.field(fields.createHtmlDef(fieldSchema));
						break;
					case NUMBER:
						fieldsType.field(fields.createNumberDef(fieldSchema));
						break;
					case DATE:
						fieldsType.field(fields.createDateDef(fieldSchema));
						break;
					case BOOLEAN:
						fieldsType.field(fields.createBooleanDef(fieldSchema));
						break;
					case NODE:
						fieldsType.field(fields.createNodeDef(fieldSchema));
						break;
					case BINARY:
						fieldsType.field(fields.createBinaryDef(fieldSchema));
						break;
					case LIST:
						ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
						fieldsType.field(fields.createListDef(context, listFieldSchema));
						break;
					case MICRONODE:
						fieldsType.field(fields.createMicronodeDef(fieldSchema, project));
						break;
				}
			}
			fieldsField.name("fields").type(fieldsType);
			root.field(fieldsField);
			root.fields(createNodeInterfaceFields(context).forVersion(context));
			interfaceTypeProvider.addCommonFields(root, true);
			return root.build();
		}).collect(Collectors.toList());
	}

	public static String nodeTypeName(String schemaName) {
		return schemaName + "Fields";
	}

}
