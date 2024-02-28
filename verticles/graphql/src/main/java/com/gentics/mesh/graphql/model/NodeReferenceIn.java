package com.gentics.mesh.graphql.model;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.util.Lazy;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphql.context.GraphQLContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Represents an incoming node reference. Use the fromContent methods to create a stream of references.
 * {@link #getFieldName()} and {@link #getMicronodeFieldName()} are lazy to avoid possibly unnecessary database calls.
 */
public class NodeReferenceIn {
	private static final Logger log = LoggerFactory.getLogger(NodeReferenceIn.class);

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
		return fromContent(gc, content, type, true, true, true, true);
	}

	/**
	 * Creates a stream of incoming node references for the given content.
	 * @param gc
	 * @param content
	 * @param type
	 * @return
	 */
	public static Stream<NodeReferenceIn> fromContent(GraphQLContext gc, NodeContent content, ContainerType type, boolean lookupInFields, boolean lookupInLists, boolean lookupInContent, boolean lookupInMicrocontent) {
		Tx tx = Tx.get();
		ContentDao contentDao = tx.contentDao();
		String branchUuid = tx.getBranch(gc).getUuid();
		return contentDao.getInboundReferences(content.getNode(), lookupInFields, lookupInLists)
			.flatMap(ref -> ref.getReferencingContents(lookupInContent, lookupInMicrocontent)
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
		CommonTx tx = CommonTx.get();
		PersistingContentDao contentDao = tx.contentDao();
		String branchUuid = tx.getBranch(gc).getUuid();

		// content per node
		Map<HibNode, List<NodeContent>> nodeOriginalContent = contents.stream().collect(Collectors.groupingBy(NodeContent::getNode, Collectors.mapping(Function.identity(), Collectors.toList())));

		// field referencing node
		Map<HibNodeField, HibNode> refNodes = contentDao.getInboundReferences(nodeOriginalContent.keySet()).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		// field belonging to the referencing content
		Map<HibNodeField, Collection<HibNodeFieldContainer>> fieldReferencingContents = contentDao.getReferencingContents(refNodes.keySet()).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		if (tx.data().options().hasDatabaseLevelCache()) {
			// Here we preload the content nodes to the cache for performance reasons.
			contentDao.getNodes(fieldReferencingContents.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())).forEach(pair -> {
				if (log.isTraceEnabled()) {
					log.trace("{} belongs to {}", pair.getLeft().id(), pair.getRight().id());
				}
			});
		}

		Map<HibNodeFieldContainer, Collection<? extends HibNodeFieldContainerEdge>> referencingContentEdges = contentDao.getContainerEdges(fieldReferencingContents.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

		return fieldReferencingContents.entrySet().stream()
				// get all referencing content
				.flatMap(fieldReferencingContent -> fieldReferencingContent.getValue().stream()
					// map each referencing content to the referenced content
					.flatMap(referencingContent -> {
							return nodeOriginalContent.get(refNodes.get(fieldReferencingContent.getKey())).stream()
									// check type & perms
									.filter(originalContent -> 
											referencingContentEdges.get(referencingContent).stream().anyMatch(edge -> 
												// type
												contentDao.isType(edge, originalContent.getType(), branchUuid) && 
												// perms
												tx.userDao().hasReadPermission(gc.getUser(), edge, branchUuid, originalContent.getType().getHumanCode())))
									// tie together
									.map(originalContent -> Pair.of(referencingContent, originalContent));
						}
					).map(referencingOriginal -> Pair.of(
							// convert to the reference-origin pair
							new NodeReferenceIn(
									new NodeContent(null, referencingOriginal.getKey(), referencingOriginal.getValue().getLanguageFallback(), referencingOriginal.getValue().getType()), 
									fieldReferencingContent.getKey()), 
							referencingOriginal.getValue())));
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
