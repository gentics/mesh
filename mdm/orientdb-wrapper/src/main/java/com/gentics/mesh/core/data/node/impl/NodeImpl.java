package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.BranchParentEntry.branchParentEntry;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.BRANCH_PARENTS_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PARENTS_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.field.FieldType.STRING_SET;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.traversal.EdgeTraversal;
import com.gentics.madl.traversal.VertexTraversal;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BranchParentEntry;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagEdgeImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.StreamUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Node
 */
public class NodeImpl extends AbstractGenericFieldContainerVertex<NodeResponse, Node> implements Node {

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	/**
	 * Initialize the node vertex type and indices.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(vertexType(NodeImpl.class, MeshVertexImpl.class)
			.withField(PARENTS_KEY_PROPERTY, STRING_SET)
			.withField(BRANCH_PARENTS_KEY_PROPERTY, STRING_SET)
			.withField(PROJECT_KEY_PROPERTY, STRING)
			.withField(SCHEMA_CONTAINER_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("project")
			.withField(PROJECT_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("uuid_project")
			.withField("uuid", STRING)
			.withField(PROJECT_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("schema")
			.withField(SCHEMA_CONTAINER_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("parents")
			.withField(PARENTS_KEY_PROPERTY, STRING_SET));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withPostfix("branch_parents")
			.withField(BRANCH_PARENTS_KEY_PROPERTY, STRING_SET));

		GraphRelationships.addRelation(NodeImpl.class, NodeGraphFieldContainerImpl.class, "fields", HAS_FIELD_CONTAINER, "edgeType", ContainerType.INITIAL.getCode());
		GraphRelationships.addRelation(NodeImpl.class, UserImpl.class, "creator");
		GraphRelationships.addRelation(NodeImpl.class, UserImpl.class, "editor", MeshVertex.UUID_KEY, "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ContainerType.INITIAL.getCode() + "'].inv()[0].editor", null);
		GraphRelationships.addRelation(NodeImpl.class, NodeGraphFieldContainerImpl.class, "edited", null, "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ContainerType.INITIAL.getCode() + "'].inV()[0].last_edited_timestamp", null);
	}

	@Override
	public Result<HibTag> getTags(HibBranch branch) {
		return new TraversalResult<>(TagEdgeImpl.getTagTraversal(this, branch).frameExplicit(TagImpl.class));
	}

	@Override
	public boolean hasTag(HibTag tag, HibBranch branch) {
		return TagEdgeImpl.hasTag(this, tag, branch);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(String branchUuid, ContainerType type) {
		Result<GraphFieldContainerEdgeImpl> it = getFieldContainerEdges(branchUuid, type);
		Iterator<NodeGraphFieldContainer> it2 = it.stream().map(e -> e.getNodeContainer()).iterator();
		return new TraversalResult<>(it2);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(ContainerType type) {
		return new TraversalResult<>(
			outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV()
				.frameExplicit(NodeGraphFieldContainerImpl.class));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public long getFieldContainerCount() {
		return StreamUtil.toStream((Iterable) outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, P.within(Arrays.asList(DRAFT.getCode(), PUBLISHED.getCode()))).inV()).count();
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getBranchRoot().getLatestBranch(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag, HibBranch branch, ContainerType type) {
		return getGraphFieldContainer(languageTag, branch, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getBranchRoot().getLatestBranch().getUuid(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag, String branchUuid, ContainerType type) {
		return getGraphFieldContainer(languageTag, branchUuid, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public GraphFieldContainerEdge getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdge(id(), branchUuid, type, languageTag);
	}

	@Override
	public Result<GraphFieldContainerEdgeImpl> getFieldContainerEdges(String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdges(id(), branchUuid, type);
	}

	@Override
	public void addTag(HibTag tag, HibBranch branch) {
		removeTag(tag, branch);
		TagEdge edge = addFramedEdge(HAS_TAG, toGraph(tag), TagEdgeImpl.class);
		edge.setBranchUuid(branch.getUuid());
	}

	@Override
	public void removeTag(HibTag tag, HibBranch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).inV().has(MeshVertex.UUID_KEY, tag.getUuid()).forEachRemaining(Vertex::remove);
	}

	@Override
	public void removeAllTags(HibBranch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).inV().forEachRemaining(Vertex::remove);
	}

	@Override
	public void setSchemaContainer(HibSchema schema) {
		property(SCHEMA_CONTAINER_KEY_PROPERTY, schema.getUuid());
	}

	@Override
	public HibSchema getSchemaContainer() {
		String uuid = property(SCHEMA_CONTAINER_KEY_PROPERTY);
		if (uuid == null) {
			return null;
		}
		return db().index().findByUuid(SchemaContainerImpl.class, uuid);
	}

	@Override
	public Result<HibNode> getChildren() {
		return new TraversalResult<>(getGraph().frameExplicit(db().getVertices(
			NodeImpl.class,
			new String[] { PARENTS_KEY_PROPERTY },
			new Object[] { getUuid() }), NodeImpl.class));
	}

	@Override
	public Result<HibNode> getChildren(String branchUuid, ContainerType containerType, PagingParameters sorting, Optional<FilterOperation<?>> maybeFilter, Optional<HibUser> maybeUser) {
		return new TraversalResult<>(getGraph().frameExplicit(getUnframedChildren(branchUuid, sorting, maybeFilter.map(f -> maybeUser
				.map(user -> parseFilter(f, containerType, user, containerType == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM, Optional.empty()))
				.orElseGet(() -> parseFilter(f, containerType)))), NodeImpl.class));
	}

	private Iterator<Vertex> getUnframedChildren(String branchUuid, PagingParameters sorting, Optional<String> maybeFilter) {
		return db().getVertices(
			NodeImpl.class,
			new String[] { BRANCH_PARENTS_KEY_PROPERTY },
			new Object[] { branchParentEntry(branchUuid, getUuid()).encode() },
			sorting, Optional.empty(), maybeFilter);
	}

	@Override
	public Stream<Node> getChildrenStream(InternalActionContext ac, InternalPermission perm) {
		HibUser user = ac.getUser();
		Tx tx = GraphDBTx.getGraphTx();
		UserDao userDao = tx.userDao();
		return toStream(getUnframedChildren(tx.getBranch(ac).getUuid(), null, Optional.empty()))
			.filter(node -> userDao.hasPermissionForId(user, node.id(), perm))
			.map(node -> getGraph().frameElementExplicit(node, NodeImpl.class));
	}

	@Override
	public NodeImpl getParentNode(String branchUuid) {
		Set<String> parents = property(BRANCH_PARENTS_KEY_PROPERTY);
		if (parents == null) {
			return null;
		} else {
			return parents.stream()
				.map(BranchParentEntry::fromString)
				.filter(entry -> entry.getBranchUuid().equals(branchUuid))
				.findAny()
				.map(entry -> db().index().findByUuid(NodeImpl.class, entry.getParentUuid()))
				.orElse(null);
		}
	}

	@Override
	public void setParentNode(String branchUuid, HibNode parent) {
		String parentUuid = parent.getUuid();
		removeParent(branchUuid);
		addToStringSetProperty(PARENTS_KEY_PROPERTY, parentUuid);
		addToStringSetProperty(BRANCH_PARENTS_KEY_PROPERTY, branchParentEntry(branchUuid, parentUuid).encode());
	}

	@Override
	public Project getProject() {
		return db().index().findByUuid(ProjectImpl.class, property(PROJECT_KEY_PROPERTY));
	}

	@Override
	public void setProject(HibProject project) {
		property(PROJECT_KEY_PROPERTY, project.getUuid());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<HibNodeField> getInboundReferences(boolean lookupInFields, boolean lookupInLists) {
		EdgeTraversal<?, ?> edges;
		if (lookupInLists && lookupInFields) {
			edges = inE(HAS_FIELD, HAS_ITEM);
		} else if (lookupInFields) {
			edges = inE(HAS_FIELD);
		} else if (lookupInLists) {
			edges = inE(HAS_ITEM);
		} else {
			throw error(BAD_REQUEST, "For inbound references you have to pick at least one source.");
		}
		return toStream((Iterable<NodeGraphFieldImpl>) edges.hasLabel( NodeGraphFieldImpl.class.getSimpleName())).map(NodeGraphFieldImpl.class::cast);
	}

	@Override
	public void delete(BulkActionContext bac) {
		Tx.get().nodeDao().delete(this, bac, false, true);
	}

	@Override
	public void removeParent(String branchUuid) {
		Set<String> branchParents = property(BRANCH_PARENTS_KEY_PROPERTY);
		if (branchParents != null) {
			// Divide parents by branch uuid.
			Map<Boolean, Set<String>> partitions = branchParents.stream()
				.collect(Collectors.partitioningBy(
					parent -> BranchParentEntry.fromString(parent).getBranchUuid().equals(branchUuid),
					Collectors.toSet()));

			Set<String> removedParents = partitions.get(true);
			if (!removedParents.isEmpty()) {
				// If any parents were removed, we set the new set of parents back to the vertex.
				Set<String> newParents = partitions.get(false);
				property(BRANCH_PARENTS_KEY_PROPERTY, newParents);

				String removedParent = BranchParentEntry.fromString(removedParents.iterator().next()).getParentUuid();
				// If the removed parent is not parent of any other branch, remove it from the common parent set.
				boolean parentStillExists = newParents.stream()
					.anyMatch(parent -> BranchParentEntry.fromString(parent).getParentUuid().equals(removedParent));
				if (!parentStillExists) {
					Set<String> parents = property(PARENTS_KEY_PROPERTY);
					parents.remove(removedParent);
					property(PARENTS_KEY_PROPERTY, parents);
				}
			}
		}
	}

	private Stream<HibNode> getChildren(HibUser requestUser, String branchUuid, List<String> languageTags, ContainerType type) {
		InternalPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		UserDao userRoot = GraphDBTx.getGraphTx().userDao();
		ContentDao contentDao = Tx.get().contentDao();

		Predicate<HibNode> languageFilter = languageTags == null || languageTags.isEmpty()
			? item -> true
			: item -> languageTags.stream().anyMatch(languageTag -> contentDao.getFieldContainer(item, languageTag, branchUuid, type) != null);

		return getChildren(branchUuid, Optional.of(requestUser)).stream()
			.filter(languageFilter.and(item -> userRoot.hasPermission(requestUser, item, perm)));
	}

	@Override
	public Page<HibNode> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingInfo) {
		return new DynamicTransformableStreamPageImpl<>(getChildren(ac.getUser(), branchUuid, languageTags, type), pagingInfo);
	}

	@Override
	public Page<? extends HibTag> getTags(HibUser user, PagingParameters params, HibBranch branch) {
		VertexTraversal<?, ?> traversal = TagEdgeImpl.getTagTraversal(this, branch);
		return new DynamicTransformablePageImpl<>(user, traversal, params, READ_PERM, TagImpl.class);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return Tx.get().nodeDao().update(this.getProject(), this, ac, batch);
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public MeshElementEventModel onDeleted() {
		throw new NotImplementedException("Use dedicated onDeleted method for nodes instead.");
	}

	@Override
	public MeshProjectElementEventModel createEvent(MeshEvent event) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		model.setProject(getProject().transformToReference());
		fillEventInfo(model);
		return model;
	}

	@Override
	public boolean isBaseNode() {
		return inE(HAS_ROOT_NODE).hasNext();
	}

	@Override
	public void removeElement() {
		remove();
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}

	@Override
	public Iterator<? extends HibNodeFieldContainerEdge> getWebrootEdges(String segmentInfo, String branchUuid, ContainerType type) {
		return getGraph().frameExplicit(outE(HAS_FIELD_CONTAINER)
			.has(GraphFieldContainerEdge.BRANCH_UUID_KEY, branchUuid)
			.has(GraphFieldContainerEdge.WEBROOT_PROPERTY_KEY, segmentInfo)
			.has(GraphFieldContainerEdge.EDGE_TYPE_KEY, type.getCode()), GraphFieldContainerEdgeImpl.class);
	}

	@Override
	public String mapGraphQlFieldName(String gqlName) {
		switch (gqlName) {
		case "editor": return "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ContainerType.INITIAL.getCode() + "'].inV()[0].`editor`";
		}
		return super.mapGraphQlFieldName(gqlName);
	}
}
