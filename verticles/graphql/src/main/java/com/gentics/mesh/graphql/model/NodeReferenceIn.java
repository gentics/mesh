package com.gentics.mesh.graphql.model;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
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

	private NodeReferenceIn(NodeContent node, HibNodeField nodeField) {
		this.node = node;
		this.fieldName = new Lazy<>(nodeField::getFieldName);
		this.micronodeFieldName = new Lazy<>(() -> nodeField.getMicronodeFieldName().orElse(null));
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
		ContentDao contentDao = tx.contentDao();
		String branchUuid = tx.getBranch(gc).getUuid();
		return contentDao.getInboundReferences(content.getNode())
			.flatMap(ref -> ref.getReferencingContents()
				.filter(container -> {
					if (type == DRAFT && contentDao.isDraft(container, branchUuid)) {
						return true;
					}
					if (type == PUBLISHED && contentDao.isPublished(container, branchUuid)) {
						return true;
					}
					return false;
				})
				.filter(c -> gc.hasReadPerm(c, type))
				.findAny().stream()
					.map(referencingContent -> new NodeReferenceIn(
						new NodeContent(null, referencingContent, content.getLanguageFallback(), type),
						ref)));
	}

	/**
	 * Creates a stream of incoming node references for the given content.
	 * @param gc
	 * @param contentTypes a content/type pair
	 * @return
	 */
	public static Stream<Pair<NodeReferenceIn, NodeContent>> fromContent(GraphQLContext gc, Collection<NodeContent> contents) {
		Tx tx = Tx.get();
		ContentDao contentDao = tx.contentDao();
		String branchUuid = tx.getBranch(gc).getUuid();

		// content per node
		Map<HibNode, List<NodeContent>> nodeContent = contents.stream().collect(Collectors.groupingBy(NodeContent::getNode, Collectors.mapping(Function.identity(), Collectors.toList())));

		// 1. Collecting referencing fields for the nodes
		return contentDao.getInboundReferences(
					contents.stream().map(NodeContent::getNode).collect(Collectors.toSet())
				// 2. Collect the contents of the referencing fields
				).flatMap(refNode -> refNode.getKey().getReferencingContents()
						// Filter by type & access
						.filter(container -> 
							Optional.ofNullable(nodeContent.get(refNode.getValue())).flatMap(nodeContents -> nodeContents.stream().map(NodeContent::getType).filter(type -> {
								if (type == DRAFT && !contentDao.isDraft(container, branchUuid)) {
									return false;
								}
								if (type == PUBLISHED && !contentDao.isPublished(container, branchUuid)) {
									return false;
								}
								return gc.hasReadPerm(container, type);
							}).findAny()).isPresent())
						.findAny().stream()
						.flatMap(referencingContent -> nodeContent.get(refNode.getValue()).stream()
								.map(content -> Pair.of(new NodeReferenceIn(
									new NodeContent(null, referencingContent, content.getLanguageFallback(), content.getType()), refNode.getKey()), 
									content))));
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
