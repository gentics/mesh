package com.gentics.mesh.graphql.model;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.stream.Stream;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Represents an ingoing node reference. Use the fromContent methods to create a stream of references.
 * {@link #getFieldName()} and {@link #getMicronodeFieldName()} are lazy to avoid possibly unnecessary database calls.
 */
public class NodeReferenceIn {
	private final NodeContent node;
	private final Lazy<String> fieldName;
	private final Lazy<String> micronodeFieldName;

	private NodeReferenceIn(NodeContent node, NodeGraphField nodeGraphField) {
		this.node = node;
		this.fieldName = new Lazy<>(nodeGraphField::getFieldName);
		this.micronodeFieldName = new Lazy<>(() -> nodeGraphField.getMicronodeFieldName().orElse(null));
	}

	/**
	 * Creates a stream of ingoing node references for the given content.
	 * @param context
	 * @param content
	 * @return
	 */
	public static Stream<NodeReferenceIn> fromContent(GraphQLContext context, NodeContent content) {
		String branchUuid = context.getBranch().getUuid();
		MeshAuthUser user = context.getUser();
		return content.getNode()
			.getInReferences()
			.flatMap(ref -> toStream(ref.getReferencingContents()
			.filter(container -> {
				Node node = container.getParentNode();
				return container.isType(DRAFT, branchUuid) && user.hasPermission(node, GraphPermission.READ_PERM) ||
					container.isType(PUBLISHED, branchUuid) && user.hasPermission(node, GraphPermission.READ_PUBLISHED_PERM);
			}).findAny())
			.map(referencingContent -> new NodeReferenceIn(
				new NodeContent(null, referencingContent, content.getLanguageFallback()),
				ref
			)));
	}

	/**
	 * Gets the content that references the original node.
	 * @return
	 */
	public NodeContent getNode() {
		return node;
	}

	/**
	 * Gets the name of the field where the reference originated.
	 * @return
	 */
	public String getFieldName() {
		return fieldName.get();
	}

	/**
	 * Gets the name of the field in the micronode where the reference originated.
	 * Null if the reference did not originate from a micronode.
	 * @return
	 */
	public String getMicronodeFieldName() {
		return micronodeFieldName.get();
	}
}
