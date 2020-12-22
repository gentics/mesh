package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
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
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.model.NodeReferenceIn;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;
import com.gentics.mesh.handler.Versioned;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathImpl;
import com.gentics.mesh.path.impl.PathSegmentImpl;
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
	public NodeTypeProvider(AbstractMeshOptions options) {
		super(options);
	}

	/**
	 * Fetcher for the parent node reference of a node.
	 *
	 * @param env
	 * @return
	 */
	public Object parentNodeFetcher(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		NodeDaoWrapper nodeDao = tx.nodeDao();
		ContentDaoWrapper contentDao = tx.contentDao();

		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		GraphQLContext gc = env.getContext();
		String uuid = tx.getBranch(gc).getUuid();
		HibNode parentNode = nodeDao.getParentNode(content.getNode(), uuid);
		// The project root node can have no parent. Lets check this and exit early.
		if (parentNode == null) {
			return null;
		}
		gc.requiresPerm(parentNode, READ_PERM, READ_PUBLISHED_PERM);

		List<String> languageTags = getLanguageArgument(env, content);
		ContainerType type = getNodeVersion(env);

		NodeGraphFieldContainer container = contentDao.findVersion(parentNode, gc, languageTags, type);
		container = gc.requiresReadPermSoft(container, env);

		return new NodeContent(parentNode, container, languageTags, type);
	}

	public Object nodeLanguageFetcher(DataFetchingEnvironment env) {
		ContentDaoWrapper contentDao = Tx.get().contentDao();
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		List<String> languageTags = getLanguageArgument(env);
		GraphQLContext gc = env.getContext();

		HibNode node = content.getNode();
		ContainerType type = getNodeVersion(env);
		NodeGraphFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
		container = gc.requiresReadPermSoft(container, env);
		return new NodeContent(node, container, languageTags, type);
	}

	public Object breadcrumbFetcher(DataFetchingEnvironment env) {
		ContentDaoWrapper contentDao = Tx.get().contentDao();
		NodeDaoWrapper nodeDao = Tx.get().nodeDao();
		GraphQLContext gc = env.getContext();
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}

		ContainerType type = getNodeVersion(env);
		return nodeDao.getBreadcrumbNodes(content.getNode(), gc).stream().map(node -> {
			List<String> languageTags = getLanguageArgument(env, content);
			NodeGraphFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
			return new NodeContent(node, container, languageTags, type);
		})
			.filter(item -> item.getContainer() != null)
			.filter(gc::hasReadPerm)
			.collect(Collectors.toList());
	}

	public Object languagesFetcher(DataFetchingEnvironment env) {
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		Tx tx = Tx.get();
		ContentDaoWrapper contentDao = tx.contentDao();
		GraphQLContext gc = env.getContext();
		HibBranch branch = tx.getBranch(gc);

		ContainerType type = getNodeVersion(env);
		Stream<? extends NodeGraphFieldContainer> stream = StreamSupport
			.stream(contentDao.getGraphFieldContainers(content.getNode(), branch, type).spliterator(), false);
		return stream
			.filter(gc::hasReadPerm)
			.map(container -> {
				return new NodeContent(content.getNode(), container, content.getLanguageFallback(), type);
			}).collect(Collectors.toList());
	}

	public Versioned<GraphQLType> createType(GraphQLContext context) {
		return Versioned.<GraphQLType>since(1, () -> {
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
				HibProject projectOfNode = content.getNode().getProject();
				return gc.requiresPerm(projectOfNode, READ_PERM);
			}).build(),

			// .breadcrumb
			newFieldDefinition().name("breadcrumb")
				.description("Breadcrumb of the node")
				.type(new GraphQLList(new GraphQLTypeReference(NODE_TYPE_NAME)))
				.argument(createNodeVersionArg())
				.argument(createLanguageTagArg(false))
				.dataFetcher(this::breadcrumbFetcher).build(),

			// .availableLanguages
			newFieldDefinition().name("availableLanguages").description("List all available languages for the node").type(new GraphQLList(
				GraphQLString)).dataFetcher(env -> {
					NodeDaoWrapper nodeDao = Tx.get().nodeDao();
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					// TODO handle branch!
					return nodeDao.getAvailableLanguageNames(content.getNode());
				}).build(),

			// .languages
			newFieldDefinition()
				.name("languages")
				.description("Load all languages of the node")
				.argument(createNodeVersionArg())
				.type(new GraphQLList(new GraphQLTypeReference(NODE_TYPE_NAME)))
				.dataFetcher(this::languagesFetcher)
				.build(),

			// .child
			newFieldDefinition()
				.name("child")
				.description("Resolve a webroot path to a specific child node.")
				.argument(createPathArg())
				.argument(createNodeVersionArg())
				.type(new GraphQLTypeReference(NODE_TYPE_NAME))
				.dataFetcher(env -> {
					Tx tx = Tx.get();
					String nodePath = env.getArgument("path");
					if (nodePath != null) {
						GraphQLContext gc = env.getContext();

						NodeContent content = env.getSource();
						if (content == null) {
							return null;
						}
						HibNode node = content.getNode();
						// Resolve the given path and return the found container
						HibBranch branch = tx.getBranch(gc);
						String branchUuid = branch.getUuid();
						ContainerType type = getNodeVersion(env);
						Stack<String> pathStack = new Stack<>();
						pathStack.add(nodePath);
						Path path = new PathImpl();
						try {
							NodeDaoWrapper nodeDao = Tx.get().nodeDao();
							nodeDao.resolvePath(node, branchUuid, type, path, pathStack);
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
						NodeGraphFieldContainer containerFromPath = ((PathSegmentImpl)lastSegment).getContainer();
						HibNode nodeFromPath = null;
						if (containerFromPath != null) {
							nodeFromPath = boot.contentDao().getNode(containerFromPath);
							gc.requiresPerm(nodeFromPath, READ_PERM, READ_PUBLISHED_PERM);
							containerFromPath = gc.requiresReadPermSoft(containerFromPath, env);
						} else {
							return null;
						}
						List<String> langs = new ArrayList<>();
						if (containerFromPath != null) {
							langs = Arrays.asList(containerFromPath.getLanguageTag());
						}
						return new NodeContent(nodeFromPath, containerFromPath, langs, type);
					}
					return null;
				}).build(),

			// .children
			newPagingFieldWithFetcherBuilder("children", "Load child nodes of the node.", (env) -> {
				ContentDaoWrapper contentDao = Tx.get().contentDao();
				NodeDaoWrapper nodeDao = Tx.get().nodeDao();
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}

				List<String> languageTags = getLanguageArgument(env, content);
				ContainerType type = getNodeVersion(env);

				Stream<NodeContent> nodes = nodeDao.getChildrenStream(content.getNode(), gc)
					.map(item -> new NodeContent(item, contentDao.findVersion(item, gc, languageTags, type), languageTags, type))
					.filter(item -> item.getContainer() != null)
					.filter(item -> gc.hasReadPerm(item.getContainer()));

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
				.argument(createNodeVersionArg())
				.dataFetcher(this::parentNodeFetcher).build(),

			// .tags
			newFieldDefinition().name("tags")
				.argument(createPagingArgs()).type(new GraphQLTypeReference(TAG_PAGE_TYPE_NAME))
				.dataFetcher(env -> {
					Tx tx = Tx.get();
					TagDaoWrapper tagDao = tx.tagDao();

					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					HibNode node = content.getNode();
					return tagDao.getTags(node, gc.getUser(), getPagingInfo(env), tx.getBranch(gc));
				}).build(),

			// TODO Fix name confusion and check what version of schema should be used to determine this type
			// .isContainer
			newFieldDefinition().name("isContainer").description("Check whether the node can have subnodes via children").type(
				GraphQLBoolean).dataFetcher((env) -> {
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					HibNode node = content.getNode();
					return node.getSchemaContainer().getLatestVersion().getSchema().getContainer();
				}).build(),

			// .referencedBy
			newFieldDefinition()
				.name("referencedBy")
				.description("Loads nodes that reference this node.")
				.argument(createPagingArgs())
				.argument(nodeReferenceFilter(context).createFilterArgument())
				.argument(createNodeVersionArg())
				.type(new GraphQLTypeReference(NODE_REFERENCE_PAGE_TYPE_NAME))
				.dataFetcher(env -> {
					NodeContent content = env.getSource();
					ContainerType type = getNodeVersion(env);
					Stream<NodeReferenceIn> stream = NodeReferenceIn.fromContent(context, content, type);
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
				.argument(createNodeVersionArg())
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
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				ContainerType containerType = getNodeVersion(env);
				String branchUuid = Tx.get().getBranch(gc).getUuid();
				String languageTag = container.getLanguageTag();
				return boot.nodeDao().getPath(boot.contentDao().getNode(container), gc, branchUuid, containerType, languageTag);
			}).build(),

			// .edited
			newFieldDefinition().name("edited").description("ISO8601 formatted edit timestamp.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return container.getLastEditedDate();
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
					NodeGraphFieldContainer container = content.getContainer();
					if (container == null) {
						return null;
					}
					return container.getSchemaContainerVersion();
				}).build(),

			// .isPublished
			newFieldDefinition().name("isPublished").description("Check whether the content is published.").type(GraphQLBoolean)
				.dataFetcher(env -> {
					Tx tx = Tx.get();
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					NodeGraphFieldContainer container = content.getContainer();
					if (container == null) {
						return null;
					}
					return container.isPublished(tx.getBranch(gc).getUuid());
				}).build(),

			// .isDraft
			newFieldDefinition().name("isDraft").description("Check whether the content is a draft.").type(GraphQLBoolean).dataFetcher(
				env -> {
					Tx tx = Tx.get();
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					NodeGraphFieldContainer container = content.getContainer();
					if (container == null) {
						return null;
					}
					return container.isDraft(tx.getBranch(gc).getUuid());
				}).build(),

			// .version
			newFieldDefinition().name("version").description("Version of the content.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return container.getVersion().getFullVersion();
			}).build(),

			// .versions
			newFieldDefinition().name("versions").description("List of versions of the node.")
				.argument(createSingleLanguageTagArg(true))
				.type(GraphQLList.list(GraphQLTypeReference.typeRef(NODE_CONTENT_VERSION_TYPE_NAME))).dataFetcher(env -> {
					Tx tx = Tx.get();
					ContentDaoWrapper contentDao = tx.contentDao();
					GraphQLContext gc = env.getContext();
					String languageTag = getSingleLanguageArgument(env);
					NodeContent content = env.getSource();
					HibNode node = content.getNode();
					if (node == null) {
						return null;
					}
					// TODO this is hardcoding the draft versions. We maybe want both or check which type of nodecontent is currently loaded.
					// This would not have been a problem if contents would be reflected as a type in graphql
					return contentDao.getGraphFieldContainers(node, tx.getBranch(gc), DRAFT).stream().filter(c -> {
						String lang = c.getLanguageTag();
						return lang.equals(languageTag);
					}).findFirst().map(NodeGraphFieldContainer::versions).orElse(null);
				}).build(),

			// .language
			newFieldDefinition().name("language").description("The language of this content.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return container.getLanguageTag();
			}).build(),

			// .displayName
			newFieldDefinition().name("displayName").description("The value of the display field.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return boot.contentDao().getDisplayFieldValue(container);
			}).build());

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
					return content.getContainer();
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
				Tx tx = Tx.get();
				GraphQLContext gc = env.getContext();
				String branchUuid = tx.getBranch(gc).getUuid();
				NodeGraphFieldContainer version = env.getSource();
				return version.isDraft(branchUuid);
			}));

		// .published
		builder.field(newFieldDefinition().name("published").description("Flag that indicates whether the version is used as a published version.")
			.type(GraphQLBoolean).dataFetcher(env -> {
				Tx tx = Tx.get();
				GraphQLContext gc = env.getContext();
				String branchUuid = tx.getBranch(gc).getUuid();
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
		HibUser user = content.getContainer().getEditor();
		return gc.requiresPerm(user, READ_PERM);
	}

	/**
	 * Invoke a elasticsearch using the provided query and return a page of found containers.
	 *
	 * @param gc
	 * @param query
	 * @param pagingInfo
	 * @param type
	 * @return
	 */
	public Page<? extends NodeContent> handleContentSearch(GraphQLContext gc, String query, PagingParameters pagingInfo, ContainerType type) {
		try {
			return nodeSearchHandler.handleContainerSearch(gc, query, pagingInfo, type, READ_PERM, READ_PUBLISHED_PERM);
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
	public Page<? extends HibNode> handleSearch(GraphQLContext gc, String query, PagingParameters pagingInfo) {
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
		Tx tx = Tx.get();
		SchemaDaoWrapper schemaDao = tx.schemaDao();

		HibProject project = tx.getProject(context);
		List<GraphQLObjectType> schemaTypes = new ArrayList<>();
		for (HibSchema container : schemaDao.findAll(project)) {
			HibSchemaVersion version = container.getLatestVersion();
			SchemaModel schema = version.getSchema();
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
		Tx tx = Tx.get();
		HibProject project = tx.getProject(context);

		SchemaDaoWrapper schemaDao = Tx.get().schemaDao();
		return schemaDao.findAll(project).stream().map(container -> {
			HibSchemaVersion version = container.getLatestVersion();
			SchemaModel schema = version.getSchema();
			GraphQLObjectType.Builder root = newObject();
			root.withInterface(GraphQLTypeReference.typeRef(NODE_TYPE_NAME));
			root.name(schema.getName());
			root.description(schema.getDescription());

			GraphQLFieldDefinition.Builder fieldsField = GraphQLFieldDefinition.newFieldDefinition();
			GraphQLObjectType.Builder fieldsType = newObject();
			fieldsType.name(nodeTypeName(schema.getName()));
			fieldsField.dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer();
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
