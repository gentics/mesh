package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.UserTypeProvider.USER_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.type.field.NodeFieldTypeProvider;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.search.index.node.NodeSearchHandler;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

/**
 * Type provider for the node type. Internally this will map partially to {@link Node} and {@link NodeGraphFieldContainer} vertices.
 */
@Singleton
public class NodeTypeProvider extends AbstractTypeProvider {

	public static final String NODE_TYPE_NAME = "Node";

	public static final String NODE_PAGE_TYPE_NAME = "NodesPage";

	@Inject
	public NodeSearchHandler nodeSearchHandler;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider tagTypeProvider;

	@Inject
	public BootstrapInitializer boot;

	@Inject
	public NodeFieldTypeProvider nodeFieldTypeProvider;

	@Inject
	public NodeTypeProvider() {
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

		List<String> languageTags =  getLanguageArgument(env, content);
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
			List<String> languageTags =  getLanguageArgument(env, content);
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
			.stream(content.getNode().getGraphFieldContainersIt(branch, type).spliterator(), false);
		return stream.map(item -> {
			return new NodeContent(content.getNode(), item, content.getLanguageFallback());
		}).collect(Collectors.toList());
	}

	public GraphQLObjectType createType(GraphQLContext context) {
		Project project = context.getProject();
		Builder nodeType = newObject();
		nodeType.name(NODE_TYPE_NAME);
		nodeType.description(
			"A Node is the basic building block for contents. Nodes can contain multiple language specific contents. These contents contain the fields with the actual content.");
		interfaceTypeProvider.addCommonFields(nodeType, true);

		// .project
		nodeType.field(newFieldDefinition().name("project").description("Project of the node").type(new GraphQLTypeReference("Project")).dataFetcher((
			env) -> {
			GraphQLContext gc = env.getContext();
			NodeContent content = env.getSource();
			if (content == null) {
				return null;
			}
			Project projectOfNode = content.getNode().getProject();
			return gc.requiresPerm(projectOfNode, READ_PERM);
		}));

		// .breadcrumb
		nodeType.field(newFieldDefinition().name("breadcrumb").description("Breadcrumb of the node").type(new GraphQLList(new GraphQLTypeReference(
			NODE_TYPE_NAME))).dataFetcher(this::breadcrumbFetcher));

		// .availableLanguages
		nodeType.field(newFieldDefinition().name("availableLanguages").description("List all available languages for the node").type(new GraphQLList(
			GraphQLString)).dataFetcher(env -> {
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				// TODO handle branch!
				return content.getNode().getAvailableLanguageNames();
			}));

		// .languages
		nodeType.field(newFieldDefinition().name("languages").description("Load all languages of the node").type(new GraphQLList(
			new GraphQLTypeReference(NODE_TYPE_NAME))).dataFetcher(this::languagesFetcher));

		// .child
		nodeType.field(newFieldDefinition().name("child").description("Resolve a webroot path to a specific child node.").argument(createPathArg())
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
					return new NodeContent(null, container, Arrays.asList(container.getLanguage().getLanguageTag()));
				}
				return null;
			}));

		// .children
		nodeType.field(newPagingFieldWithFetcherBuilder("children", "Load child nodes of the node.", (env) -> {
			GraphQLContext gc = env.getContext();
			NodeContent content = env.getSource();
			if (content == null) {
				return null;
			}

			List<String> languageTags = getLanguageArgument(env, content);

			Stream<NodeContent> nodes = content.getNode().getChildrenStream(gc)
				.map(item -> new NodeContent(item, item.findVersion(gc, languageTags), languageTags))
				.filter(item -> item.getContainer() != null);

			return applyNodeFilter(env, nodes);
		}, NODE_PAGE_TYPE_NAME)
			.argument(createLanguageTagArg(false))
			.argument(NodeFilter.filter(context).createFilterArgument()));

		// .parent
		nodeType.field(
			newFieldDefinition()
			.name("parent")
			.description("Parent node")
			.type(new GraphQLTypeReference(NODE_TYPE_NAME))
			.argument(createLanguageTagArg(false))
			.dataFetcher(this::parentNodeFetcher));

		// .tags
		nodeType.field(newFieldDefinition().name("tags").argument(createPagingArgs()).type(new GraphQLTypeReference(TAG_PAGE_TYPE_NAME)).dataFetcher((
			env) -> {
			GraphQLContext gc = env.getContext();
			NodeContent content = env.getSource();
			if (content == null) {
				return null;
			}
			Node node = content.getNode();
			return node.getTags(gc.getUser(), getPagingInfo(env), gc.getBranch());
		}));

		// TODO Fix name confusion and check what version of schema should be used to determine this type
		// .isContainer
		nodeType.field(newFieldDefinition().name("isContainer").description("Check whether the node can have subnodes via children").type(
			GraphQLBoolean).dataFetcher((env) -> {
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				Node node = content.getNode();
				return node.getSchemaContainer().getLatestVersion().getSchema().isContainer();
			}));

		// Content specific fields

		// .node
		nodeType.field(
			newFieldDefinition()
				.name("node")
				.description("Load the node with a different language.")
				.argument(createLanguageTagArg(false))
				.dataFetcher(this::nodeLanguageFetcher)
				.type(new GraphQLTypeReference(NODE_TYPE_NAME))
				.build());

		// .path
		nodeType.field(newFieldDefinition().name("path").description("Webroot path of the content.").type(GraphQLString).dataFetcher(env -> {
			GraphQLContext gc = env.getContext();
			NodeContent content = env.getSource();
			if (content == null) {
				return null;
			}
			NodeGraphFieldContainer container = content.getContainer();
			if (container == null) {
				return null;
			}
			ContainerType containerType = ContainerType.forVersion(gc.getVersioningParameters().getVersion());
			String branchUuid = gc.getBranch().getUuid();
			String languageTag = container.getLanguage().getLanguageTag();
			return container.getParentNode().getPath(gc, branchUuid, containerType, languageTag);
		}));

		// .edited
		nodeType.field(newFieldDefinition().name("edited").description("ISO8601 formatted edit timestamp.").type(GraphQLString).dataFetcher(env -> {
			NodeContent content = env.getSource();
			NodeGraphFieldContainer container = content.getContainer();
			if (container == null) {
				return null;
			}
			return container.getLastEditedDate();
		}));

		// .editor
		nodeType.field(newFieldDefinition().name("editor").description("Editor of the element").type(new GraphQLTypeReference(USER_TYPE_NAME))
			.dataFetcher(this::editorFetcher));

		// .schema
		nodeType.field(newFieldDefinition().name("schema").description("Schema of the node").type(new GraphQLTypeReference(SCHEMA_TYPE_NAME))
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
			}));

		// .isPublished
		nodeType.field(newFieldDefinition().name("isPublished").description("Check whether the content is published.").type(GraphQLBoolean)
			.dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				if (content == null) {
					return null;
				}
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return container.isPublished(gc.getBranch().getUuid());
			}));

		// .isDraft
		nodeType.field(newFieldDefinition().name("isDraft").description("Check whether the content is a draft.").type(GraphQLBoolean).dataFetcher(
			env -> {
				GraphQLContext gc = env.getContext();
				NodeContent content = env.getSource();
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return container.isDraft(gc.getBranch().getUuid());
			}));

		// .version
		nodeType.field(newFieldDefinition().name("version").description("Version of the content.").type(GraphQLString).dataFetcher(env -> {
			NodeContent content = env.getSource();
			NodeGraphFieldContainer container = content.getContainer();
			if (container == null) {
				return null;
			}
			return container.getVersion().getFullVersion();
		}));

		// .fields
		nodeType.field(newFieldDefinition().name("fields").description("Contains the fields of the content.").type(nodeFieldTypeProvider
			.getSchemaFieldsType(context)).dataFetcher(env -> {
				// The fields can be accessed via the container so we can directly pass it along.
				NodeContent content = env.getSource();
				return content.getContainer();
			}));

		// .language
		nodeType.field(newFieldDefinition().name("language").description("The language of this content.").type(GraphQLString).dataFetcher(env -> {
			NodeContent content = env.getSource();
			NodeGraphFieldContainer container = content.getContainer();
			if (container == null) {
				return null;
			}
			return container.getLanguage().getLanguageTag();
		}));

		nodeType
			.field(newFieldDefinition().name("displayName").description("The value of the display field.").type(GraphQLString).dataFetcher(env -> {
				NodeContent content = env.getSource();
				NodeGraphFieldContainer container = content.getContainer();
				if (container == null) {
					return null;
				}
				return container.getDisplayFieldValue();
			}));

		return nodeType.build();
	}

	public Object editorFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		User user = content.getContainer().getEditor();
		return gc.requiresPerm(user, READ_PERM);
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

}
