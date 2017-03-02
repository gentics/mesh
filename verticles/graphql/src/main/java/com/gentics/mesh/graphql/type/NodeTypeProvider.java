package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.graphql.model.LinkInfo;
import com.gentics.mesh.parameter.impl.LinkType;

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
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof Node) {
						InternalActionContext ac = (InternalActionContext) fetcher.getContext();
						return ((Node) source).getBreadcrumbNodes(ac);
					}
					return null;
				})
				.build());

		// .availableLanguages
		nodeType.field(newFieldDefinition().name("availableLanguages")
				.type(new GraphQLList(GraphQLString))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof Node) {
						//TODO handle release!
						return ((Node) source).getAvailableLanguageNames();
					}
					return null;
				})
				.build());

		// .languagePaths
		nodeType.field(newFieldDefinition().name("languagePaths")
				.argument(getLinkTypeArg())
				.type(new GraphQLList(getLinkInfoType()))
				.dataFetcher(fetcher -> {
					LinkType linkType = fetcher.getArgument("linkType");
					if (linkType != null) {
						InternalActionContext ac = (InternalActionContext) fetcher.getContext();
						Release release = ac.getRelease(ac.getProject());

						Object source = fetcher.getSource();
						if (source instanceof Node) {
							Map<String, String> map = ((Node) source).getLanguagePaths(ac, linkType, release);
							return map.entrySet()
									.stream()
									.map(entry -> new LinkInfo(entry.getKey(), entry.getValue()))
									.collect(Collectors.toList());
						}
					}
					return null;
				})
				.build());

		// .children
		nodeType.field(newFieldDefinition().name("children")
				.argument(getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.build());

		// .parent
		nodeType.field(newFieldDefinition().name("parent")
				.description("Parent node")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(fetcher -> {
					// TODO add checks 
					Object source = fetcher.getSource();
					InternalActionContext ac = (InternalActionContext) fetcher.getContext();
					if (source instanceof Node) {
						String uuid = ac.getRelease(ac.getProject())
								.getUuid();
						return ((Node) source).getParentNode(uuid);
					}
					return null;
				})
				.build());

		// .tags
		nodeType.field(newFieldDefinition().name("tags")
				.argument(getPagingArgs())
				.type(tagTypeProvider.getTagType())
				.dataFetcher(fetcher -> {
					InternalActionContext ac = (InternalActionContext) fetcher.getContext();
					Object source = fetcher.getSource();
					if (source instanceof Node) {
						Release release = ac.getRelease(ac.getProject());
						return ((Node) source).getTags(release);
					}
					return null;
				})
				.build());

		// .container
		nodeType.field(newFieldDefinition().name("container")
				.type(containerTypeProvider.getContainerType(project))
				.argument(getLanguageTagArg())
				.dataFetcher(fetcher -> {
					if (fetcher.getSource() instanceof Node) {
						Node node = (Node) fetcher.getSource();
						String languageTag = fetcher.getArgument("language");
						if (languageTag != null) {
							return node.getGraphFieldContainer(languageTag);
						}
					}
					return null;
				})
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
