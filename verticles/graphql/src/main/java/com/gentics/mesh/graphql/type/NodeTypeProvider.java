package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphql.model.LinkInfo;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

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
			InternalActionContext ac = (InternalActionContext) env.getContext();
			Release release = ac.getRelease();

			Object source = env.getSource();
			if (source instanceof Node) {
				Map<String, String> map = ((Node) source).getLanguagePaths(ac, linkType, release);
				return map.entrySet()
						.stream()
						.map(entry -> new LinkInfo(entry.getKey(), entry.getValue()))
						.collect(Collectors.toList());
			}
		}
		return null;

	}

	/**
	 * Fetcher for children of a node.
	 * 
	 * @param env
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Object childrenFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof Node) {
			Node node = (Node) source;
			InternalActionContext ac = (InternalActionContext) env.getContext();
			PagingParametersImpl params = ac.getPagingParameters();

			// The obj type is validated by graphtype 
			Object obj = env.getArgument("languages");
			List<String> languageTags = null;
			if (obj instanceof List) {
				languageTags = (List<String>) obj;
			}
			return node.getChildren(ac.getUser(), languageTags, ac.getRelease()
					.getUuid(), null, params);
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
		Object source = env.getSource();
		if (source instanceof Node) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			String uuid = ac.getRelease(ac.getProject())
					.getUuid();
			Node node = ((Node) source).getParentNode(uuid);
			// The project basenode may be null
			if (node == null) {
				return null;
			}
			if (ac.getUser()
					.hasPermission(node, GraphPermission.READ_PERM)
					|| ac.getUser()
							.hasPermission(node, GraphPermission.READ_PUBLISHED_PERM)) {
				return node;
			}
		}
		return null;
	}

	public Object tagsFetcher(DataFetchingEnvironment env) {
		InternalActionContext ac = (InternalActionContext) env.getContext();
		Object source = env.getSource();
		if (source instanceof Node) {
			return ((Node) source).getTags(ac);
		}
		return null;
	}

	public Object containerFetcher(DataFetchingEnvironment env) {
		if (env.getSource() instanceof Node) {
			Node node = (Node) env.getSource();
			String languageTag = env.getArgument("language");
			if (languageTag != null) {
				return node.getGraphFieldContainer(languageTag);
			}
		}
		return null;
	}

	public Object breadcrumbFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof Node) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			return ((Node) source).getBreadcrumbNodes(ac);
		}
		return null;
	}

	public Object languageNamesFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof Node) {
			//TODO handle release!
			return ((Node) source).getAvailableLanguageNames();
		}
		return null;
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
				.build());

		// .breadcrumb
		nodeType.field(newFieldDefinition().name("breadcrumb")
				.description("Breadcrumb of the node")
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.dataFetcher(this::breadcrumbFetcher)
				.build());

		// .availableLanguages
		nodeType.field(newFieldDefinition().name("availableLanguages")
				.type(new GraphQLList(GraphQLString))
				.dataFetcher(this::languageNamesFetcher)
				.build());

		// .languagePaths
		nodeType.field(newFieldDefinition().name("languagePaths")
				.argument(getLinkTypeArg())
				.type(new GraphQLList(getLinkInfoType()))
				.dataFetcher(this::languagePathsFetcher)
				.build());

		// .children
		nodeType.field(newFieldDefinition().name("children")
				.argument(getPagingArgs())
				.argument(getLanguageTagListArg())
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.dataFetcher(this::childrenFetcher)
				.build());

		// .parent
		nodeType.field(newFieldDefinition().name("parent")
				.description("Parent node")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(this::parentNodeFetcher)
				.build());

		// .tags
		nodeType.field(newFieldDefinition().name("tags")
				.argument(getPagingArgs())
				.type(tagTypeProvider.getTagType())
				.dataFetcher(this::tagsFetcher)
				.build());

		// .container
		nodeType.field(newFieldDefinition().name("container")
				.type(containerTypeProvider.getContainerType(project))
				.argument(getLanguageTagArg())
				.dataFetcher(this::containerFetcher)
				.build());

		return nodeType.build();
	}

	private GraphQLType getLinkInfoType() {
		Builder type = newObject().name("LinkInfo");
		type.field(newFieldDefinition().name("languageTag")
				.description("Language tag")
				.type(GraphQLString)
				.build());
		type.field(newFieldDefinition().name("link")
				.description("Resolved link")
				.type(GraphQLString)
				.build());
		return type.build();
	}
}
