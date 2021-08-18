package com.gentics.mesh.graphql.model;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.stream.Stream;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphql.context.GraphQLContext;

/**
 * Represents an incoming node reference. Use the fromContent methods to create a stream of references.
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
	 * Creates a stream of incoming node references for the given content.
	 * @param gc
	 * @param content
	 * @param type
	 * @return
	 */
	public static Stream<NodeReferenceIn> fromContent(GraphQLContext gc, NodeContent content, ContainerType type) {
		Tx tx = Tx.get();
		ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
		String branchUuid = tx.getBranch(gc).getUuid();
		return contentDao.getInboundReferences(content.getNode())
			.flatMap(ref -> ref.getReferencingContents()
				.filter(container -> {
					if (type == DRAFT && container.isDraft(branchUuid)) {
						return true;
					}
					if (type == PUBLISHED && container.isPublished(branchUuid)) {
						return true;
					}
					return false;
				})
				.filter(gc::hasReadPerm)
				.findAny().stream()
					.map(referencingContent -> new NodeReferenceIn(
						new NodeContent(null, referencingContent, content.getLanguageFallback(), type),
						ref)));
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
