package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.List;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class NodeTypeProvider extends AbstractTypeProvider {

	@Inject
	public NodeIndexHandler nodeIndexHandler;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider tagTypeProvider;

	@Inject
	public ContentTypeProvider contentTypeProvider;

	@Inject
	public BootstrapInitializer boot;

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
		Node node = env.getSource();
		GraphQLContext gc = env.getContext();
		String uuid = gc.getRelease()
				.getUuid();
		Node parentNode = node.getParentNode(uuid);
		// The project root node can have no parent. Lets check this and exit early. 
		if (parentNode == null) {
			return null;
		}
		return gc.requiresPerm(parentNode, READ_PERM, READ_PUBLISHED_PERM);
	}

	public Object contentFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		String languageTag = env.getArgument("language");
		if (languageTag != null) {
			GraphQLContext gc = env.getContext();
			Release release = gc.getRelease();
			NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag);
			// There might not be a container for the selected language
			if (container == null) {
				return null;
			}

			// Check whether the user is allowed to read the published container
			boolean isPublished = container.isPublished(release.getUuid());
			if (isPublished) {
				gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
				return container;
			} else {
				// Otherwise the container is a draft and we need to use the regular read permission
				gc.requiresPerm(node, READ_PERM);
				return container;
			}
		}
		return null;
	}

	public Object breadcrumbFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Node node = env.getSource();
		return node.getBreadcrumbNodes(gc);
	}

	public GraphQLObjectType createNodeType(Project project) {
		Builder nodeType = newObject();
		nodeType.name("Node");
		nodeType.description(
				"A Node is the basic building block for contents. Nodes can contain multiple language specific contents. These contents contain the fields with the actual content.");
		interfaceTypeProvider.addCommonFields(nodeType, true);

		// .project
		nodeType.field(newFieldDefinition().name("project")
				.description("Project of the node")
				.type(new GraphQLTypeReference("Project"))
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					Node node = env.getSource();
					Project projectOfNode = node.getProject();
					return gc.requiresPerm(projectOfNode, READ_PERM);
				}));

		// .breadcrumb
		nodeType.field(newFieldDefinition().name("breadcrumb")
				.description("Breadcrumb of the node")
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.dataFetcher(this::breadcrumbFetcher));

		// .availableLanguages
		nodeType.field(newFieldDefinition().name("availableLanguages")
				.description("List all available languages for the node")
				.type(new GraphQLList(GraphQLString))
				.dataFetcher((env) -> {
					Node node = env.getSource();
					//TODO handle release!
					return node.getAvailableLanguageNames();
				}));

		// .child
		nodeType.field(newFieldDefinition().name("child")
				.description("Resolve a webroot path to a specific child content.")
				.argument(createPathArg())
				.type(new GraphQLTypeReference("Content"))
				.dataFetcher((env) -> {
					String nodePath = env.getArgument("path");
					if (nodePath != null) {
						GraphQLContext gc = env.getContext();

						// Resolve the given path and return the found container
						Node node = env.getSource();
						Release release = gc.getRelease();
						String releaseUuid = release.getUuid();
						ContainerType type = ContainerType.forVersion(gc.getVersioningParameters()
								.getVersion());
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
						if (path.getSegments()
								.isEmpty()) {
							return null;
						}
						// Otherwise return the last segment.
						PathSegment lastSegment = path.getSegments()
								.get(path.getSegments()
										.size() - 1);
						return lastSegment.getContainer();
					}
					return null;
				}));

		// .children
		nodeType.field(newPagingFieldWithFetcherBuilder("children", "Load child nodes of the node.", (env) -> {
			GraphQLContext gc = env.getContext();
			Node node = env.getSource();

			// The obj type is validated by graphtype 
			List<String> languageTags = env.getArgument("languages");
			return node.getChildren(gc.getUser(), languageTags, gc.getRelease()
					.getUuid(), null, getPagingInfo(env));

		}, "Node").argument(createLanguageTagListArg()));

		// .parent
		nodeType.field(newFieldDefinition().name("parent")
				.description("Parent node")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(this::parentNodeFetcher));

		// .tags
		nodeType.field(newFieldDefinition().name("tags")
				.argument(getPagingArgs())
				.type(tagTypeProvider.createTagType())
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					Node node = env.getSource();
					return node.getTags(gc.getUser(), createPagingParameters(env), gc.getRelease());
				}));

		// .content
		nodeType.field(newFieldDefinition().name("content")
				.type(contentTypeProvider.createContentType(project))
				.argument(getLanguageTagArg())
				.dataFetcher(this::contentFetcher));

		// TODO Fix name confusion and check what version of schema should be used to determine this type
		// .isContainer
		nodeType.field(newFieldDefinition().name("isContainer")
				.description("Check whether the node can have subnodes via children")
				.type(GraphQLBoolean)
				.dataFetcher((env) -> {
					Node node = env.getSource();
					return node.getSchemaContainer()
							.getLatestVersion()
							.getSchema()
							.isContainer();
				}));

		return nodeType.build();
	}

	private GraphQLType createLinkInfoType() {
		Builder type = newObject().name("LinkInfo");

		// .languageTag
		type.field(newFieldDefinition().name("languageTag")
				.description("Language tag")
				.type(GraphQLString));

		// .link
		type.field(newFieldDefinition().name("link")
				.description("Resolved link")
				.type(GraphQLString));
		return type.build();
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
