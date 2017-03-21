package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphql.model.LinkInfo;
import com.gentics.mesh.parameter.LinkType;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class NodeTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider tagTypeProvider;

	@Inject
	public ContainerTypeProvider containerTypeProvider;

	@Inject
	public NodeTypeProvider() {
	}

	public Object languagePathsFetcher(DataFetchingEnvironment env) {
		LinkType linkType = env.getArgument("linkType");
		if (linkType != null) {
			InternalActionContext ac = env.getContext();
			Release release = ac.getRelease();
			Node node = env.getSource();
			Map<String, String> map = node.getLanguagePaths(ac, linkType, release);
			return map.entrySet()
					.stream()
					.map(entry -> new LinkInfo(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
		}
		return null;

	}

	public Object projectFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		InternalActionContext ac = env.getContext();
		Project project = node.getProject();
		if (ac.getUser()
				.hasPermission(project, READ_PERM)) {
			return project;
		}
		return null;
	}

	/**
	 * Fetcher for the parent node reference of a node.
	 * 
	 * @param env
	 * @return
	 */
	public Object parentNodeFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		InternalActionContext ac = env.getContext();
		String uuid = ac.getRelease(ac.getProject())
				.getUuid();
		Node parentNode = node.getParentNode(uuid);
		// The project root node can have no parent. Lets check this and exit early. 
		if (parentNode == null) {
			return null;
		}
		if (ac.getUser()
				.hasPermission(parentNode, GraphPermission.READ_PERM)
				|| ac.getUser()
						.hasPermission(parentNode, GraphPermission.READ_PUBLISHED_PERM)) {
			return parentNode;
		}
		return null;
	}

	public Object tagsFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		InternalActionContext ac = env.getContext();
		return node.getTags(ac.getUser(), getPagingParameters(env), ac.getRelease());
	}

	public Object containerFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		String languageTag = env.getArgument("language");
		if (languageTag != null) {
			InternalActionContext ac = env.getContext();
			Release release = ac.getRelease();
			NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag);

			// Check whether the user is allowed to read the published container
			boolean isPublished = container.isPublished(release.getUuid());
			if (isPublished && ac.getUser()
					.hasPermission(node, READ_PUBLISHED_PERM)) {
				return container;
			}
			// Otherwise the container is a draft and we need to use the regular read permission
			if (!isPublished && ac.getUser()
					.hasPermission(node, READ_PERM)) {
				return container;
			}
		}
		return null;
	}

	public Object isContainerFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		return node.getSchemaContainer()
				.getLatestVersion()
				.getSchema()
				.isContainer();
	}

	public Object breadcrumbFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		InternalActionContext ac = env.getContext();
		return node.getBreadcrumbNodes(ac);
	}

	public Object languageNamesFetcher(DataFetchingEnvironment env) {
		Node node = env.getSource();
		//TODO handle release!
		return node.getAvailableLanguageNames();
	}

	public GraphQLObjectType getNodeType(Project project) {
		Builder nodeType = newObject();
		nodeType.name("Node");
		nodeType.description("Mesh Node");
		interfaceTypeProvider.addCommonFields(nodeType);

		// .project
		nodeType.field(newFieldDefinition().name("project")
				.description("Project of the node")
				.type(new GraphQLTypeReference("Project"))
				.dataFetcher(this::projectFetcher));

		// .breadcrumb
		nodeType.field(newFieldDefinition().name("breadcrumb")
				.description("Breadcrumb of the node")
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.dataFetcher(this::breadcrumbFetcher));

		// .availableLanguages
		nodeType.field(newFieldDefinition().name("availableLanguages")
				.type(new GraphQLList(GraphQLString))
				.dataFetcher(this::languageNamesFetcher));

		// .languagePaths
		nodeType.field(newFieldDefinition().name("languagePaths")
				.argument(createLinkTypeArg())
				.type(new GraphQLList(getLinkInfoType()))
				.dataFetcher(this::languagePathsFetcher));

		// .children
		nodeType.field(newPagingFieldWithFetcherBuilder("children", "Load child nodes of the node.", (env) -> {
			Node node = env.getSource();
			InternalActionContext ac = env.getContext();

			// The obj type is validated by graphtype 
			List<String> languageTags = env.getArgument("languages");
			return node.getChildren(ac.getUser(), languageTags, ac.getRelease()
					.getUuid(), null, getPagingInfo(env));

		}, "Node").argument(getLanguageTagListArg()));

		// .parent
		nodeType.field(newFieldDefinition().name("parent")
				.description("Parent node")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(this::parentNodeFetcher));

		// .tags
		nodeType.field(newFieldDefinition().name("tags")
				.argument(getPagingArgs())
				.type(tagTypeProvider.createTagType())
				.dataFetcher(this::tagsFetcher));

		// .container
		nodeType.field(newFieldDefinition().name("container")
				.type(containerTypeProvider.getContainerType(project))
				.argument(getLanguageTagArg())
				.dataFetcher(this::containerFetcher));

		// TODO Fix name confusion and check what version of schema should be used to determine this type
		// .isContainer
		nodeType.field(newFieldDefinition().name("isContainer")
				.description("Check whether the node can have subnodes via children")
				.type(GraphQLBoolean)
				.dataFetcher(this::isContainerFetcher));

		return nodeType.build();
	}

	private GraphQLType getLinkInfoType() {
		Builder type = newObject().name("LinkInfo");

		// .languageTag
		type.field(newFieldDefinition().name("languageTag")
				.description("Language tag")
				.type(GraphQLString)
				.build());

		// .link
		type.field(newFieldDefinition().name("link")
				.description("Resolved link")
				.type(GraphQLString)
				.build());
		return type.build();
	}
}
