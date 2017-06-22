package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.UserTypeProvider.USER_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.field.NodeFieldTypeProvider;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

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
	public NodeIndexHandler nodeIndexHandler;

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
		String uuid = gc.getRelease().getUuid();
		Node parentNode = content.getNode().getParentNode(uuid);
		// The project root node can have no parent. Lets check this and exit early.
		if (parentNode == null) {
			return null;
		}
		gc.requiresPerm(parentNode, READ_PERM, READ_PUBLISHED_PERM);
		return handleLanguageFallback(gc, parentNode, content);
	}

	public Object nodeLanguageFetcher(DataFetchingEnvironment env) {
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}
		List<String> languageTags = getLanguageArgument(env);
		GraphQLContext gc = env.getContext();

		Node node = content.getNode();
		Release release = gc.getRelease();
		NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(gc, languageTags);
		// There might not be a container for the selected language (incl. fallback language)
		if (container == null) {
			return null;
		}

		// Check whether the user is allowed to read the published container
		boolean isPublished = container.isPublished(release.getUuid());
		if (isPublished) {
			gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
		} else {
			// Otherwise the container is a draft and we need to use the regular read permission
			gc.requiresPerm(node, READ_PERM);
		}
		return new NodeContent(node, container);
	}

	public Object breadcrumbFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		NodeContent content = env.getSource();
		if (content == null) {
			return null;
		}

		return content.getNode().getBreadcrumbNodes(gc).stream().map(node -> {
			return handleLanguageFallback(gc, node, content);
		}).collect(Collectors.toList());
	}

	/**
	 * Handle the language fallback within graphql queries when dealing with nodes. This method loads the container which best matches the current query
	 * situation. A list of languages is constructed in order to apply the fallback and load the matching container from the given node.
	 * <ul>
	 * <li>Check whether the given content has a container. Use the container language to load the container from the node</li>
	 * <li>If the content does not provide a container the default mesh language is used to load the container.
	 * </ul>
	 * 
	 * @param gc
	 * @param node
	 *            Node from which the container will be loaded
	 * @param content
	 *            Content which may contain a container from which the language information will be used to load the container
	 * @return Located container or null if no container could be found
	 */
	private NodeContent handleLanguageFallback(GraphQLContext gc, Node node, NodeContent content) {
		List<String> languageTags = new ArrayList<>();
		if (content.getContainer() != null) {
			languageTags.add(content.getContainer().getLanguage().getLanguageTag());
		} else {
			languageTags.add(Mesh.mesh().getOptions().getDefaultLanguage());
		}
		return new NodeContent(node, node.findNextMatchingFieldContainer(gc, languageTags));
	}

	public GraphQLObjectType createType(Project project) {
		Builder nodeType = newObject();
		nodeType.name(NODE_TYPE_NAME);
		nodeType.description(
				"A Node is the basic building block for contents. Nodes can contain multiple language specific contents. These contents contain the fields with the actual content.");
		interfaceTypeProvider.addCommonFields(nodeType, true);

		// .project
		nodeType.field(newFieldDefinition().name("project").description("Project of the node").type(new GraphQLTypeReference("Project"))
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					Project projectOfNode = content.getNode().getProject();
					return gc.requiresPerm(projectOfNode, READ_PERM);
				}));

		// .breadcrumb
		nodeType.field(newFieldDefinition().name("breadcrumb").description("Breadcrumb of the node")
				.type(new GraphQLList(new GraphQLTypeReference(NODE_TYPE_NAME))).dataFetcher(this::breadcrumbFetcher));

		// .availableLanguages
		nodeType.field(newFieldDefinition().name("availableLanguages").description("List all available languages for the node")
				.type(new GraphQLList(GraphQLString)).dataFetcher((env) -> {
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					// TODO handle release!
					return content.getNode().getAvailableLanguageNames();
				}));

		// .child
		nodeType.field(newFieldDefinition().name("child").description("Resolve a webroot path to a specific child node.").argument(createPathArg())
				.type(new GraphQLTypeReference(NODE_TYPE_NAME)).dataFetcher((env) -> {
					String nodePath = env.getArgument("path");
					if (nodePath != null) {
						GraphQLContext gc = env.getContext();

						NodeContent content = env.getSource();
						if (content == null) {
							return null;
						}
						Node node = content.getNode();
						// Resolve the given path and return the found container
						Release release = gc.getRelease();
						String releaseUuid = release.getUuid();
						ContainerType type = ContainerType.forVersion(gc.getVersioningParameters().getVersion());
						Stack<String> pathStack = new Stack<>();
						pathStack.add(nodePath);
						Path path = new Path();
						try {
							node.resolvePath(releaseUuid, type, path, pathStack);
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
						Node nodeOfContainer = null;
						return new NodeContent(nodeOfContainer, container);
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

			ContainerType selectedType = ContainerType.forVersion(gc.getVersioningParameters().getVersion());
			Node node = content.getNode();
			List<String> languageTags = getLanguageArgument(env);

			TransformablePage<? extends Node> page = node.getChildren(gc.getUser(), languageTags, gc.getRelease().getUuid(), selectedType,
					getPagingInfo(env));

			// Transform the found nodes into contents
			List<NodeContent> contents = page.getWrappedList().stream().map(item -> {
				NodeGraphFieldContainer container = item.findNextMatchingFieldContainer(gc, languageTags);
				return new NodeContent(item, container);
			}).collect(Collectors.toList());
			return new PageImpl<NodeContent>(contents, page);
		}, NODE_PAGE_TYPE_NAME).argument(createLanguageTagArg()));

		// .parent
		nodeType.field(newFieldDefinition().name("parent").description("Parent node").type(new GraphQLTypeReference(NODE_TYPE_NAME))
				.dataFetcher(this::parentNodeFetcher));

		// .tags
		nodeType.field(
				newFieldDefinition().name("tags").argument(createPagingArgs()).type(new GraphQLTypeReference(TAG_PAGE_TYPE_NAME)).dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					Node node = content.getNode();
					return node.getTags(gc.getUser(), getPagingParameters(env), gc.getRelease());
				}));

		// TODO Fix name confusion and check what version of schema should be used to determine this type
		// .isContainer
		nodeType.field(newFieldDefinition().name("isContainer").description("Check whether the node can have subnodes via children")
				.type(GraphQLBoolean).dataFetcher((env) -> {
					NodeContent content = env.getSource();
					if (content == null) {
						return null;
					}
					Node node = content.getNode();
					return node.getSchemaContainer().getLatestVersion().getSchema().isContainer();
				}));

		// Content specific fields

		// .node
		nodeType.field(newFieldDefinition().name("node").description("Load the node with a different language.").argument(createLanguageTagArg())
				.argument(createLanguageTagArg()).dataFetcher(this::nodeLanguageFetcher).type(new GraphQLTypeReference(NODE_TYPE_NAME)).build());

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
			String releaseUuid = gc.getRelease().getUuid();
			String languageTag = container.getLanguage().getLanguageTag();
			return container.getParentNode().getPath(releaseUuid, containerType, languageTag);
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
					return container.isPublished(gc.getRelease().getUuid());
				}));

		// .isDraft
		nodeType.field(
				newFieldDefinition().name("isDraft").description("Check whether the content is a draft.").type(GraphQLBoolean).dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					NodeContent content = env.getSource();
					NodeGraphFieldContainer container = content.getContainer();
					if (container == null) {
						return null;
					}
					return container.isDraft(gc.getRelease().getUuid());
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
		nodeType.field(newFieldDefinition().name("fields").description("Contains the fields of the content.")
				.type(nodeFieldTypeProvider.getSchemaFieldsType(project)).dataFetcher(env -> {
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
			return nodeIndexHandler.handleContainerSearch(gc, query, pagingInfo, READ_PERM, READ_PUBLISHED_PERM);
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
			return nodeIndexHandler.query(gc, query, pagingInfo, READ_PERM, READ_PUBLISHED_PERM);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
