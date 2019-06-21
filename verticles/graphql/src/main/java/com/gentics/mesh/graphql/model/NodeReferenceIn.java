package com.gentics.mesh.graphql.model;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Collections;
import java.util.stream.Stream;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.graphql.context.GraphQLContext;

public class NodeReferenceIn {
	private final NodeContent node;
	private final Lazy<String> fieldName;
	private final Lazy<String> micronodeFieldName;

	private NodeReferenceIn(NodeContent node, NodeGraphField nodeGraphField) {
		this.node = node;
		this.fieldName = new Lazy<>(nodeGraphField::getFieldName);
		this.micronodeFieldName = new Lazy<>(() -> nodeGraphField.getMicronodeFieldName().orElse(null));
	}

	public static Stream<NodeReferenceIn> fromContent(GraphQLContext context, NodeContent content) {
		return fromContent(context.getBranch().getUuid(), content);
	}

	public static Stream<NodeReferenceIn> fromContent(String branchUuid, NodeContent content) {
		return content.getNode()
			.getInReferences()
			.flatMap(ref -> toStream(ref.getReferencingContents()
			.filter(container ->
				// TODO Optimize
				container.isType(DRAFT, branchUuid) ||
					container.isType(PUBLISHED, branchUuid)
			).findAny())
			// TODO permissions
			.map(node -> new NodeReferenceIn(
				// TODO fill holes
				new NodeContent(null, node, Collections.emptyList()),
				ref
			)));
	}

	public NodeContent getNode() {
		return node;
	}

	public String getFieldName() {
		return fieldName.get();
	}

	public String getMicronodeFieldName() {
		return micronodeFieldName.get();
	}
}
