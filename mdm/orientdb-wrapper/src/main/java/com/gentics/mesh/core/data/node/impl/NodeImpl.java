package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.BranchParentEntry.branchParentEntry;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_INDEX_NAME;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
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
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.field.FieldType.STRING_SET;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BranchParentEntry;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagEdgeImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMovedEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.core.webroot.PathPrefixUtil;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NavigationParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.URIUtils;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

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
			.withPostfix("uuid")
			.withField("uuid", STRING));

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
	}

	@Override
	public String getPathSegment(String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(branchUuid) == null) {
			return "";
		}

		// Find the first matching container and fallback to other listed languages
		HibNodeFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getFieldContainer(tag, branchUuid, type)) != null) {
				break;
			}
		}

		if (container == null && anyLanguage) {
			Result<? extends GraphFieldContainerEdgeImpl> traversal = getFieldContainerEdges(branchUuid, type);

			if (traversal.hasNext()) {
				container = traversal.next().getNodeContainer();
			}
		}

		if (container != null) {
			return container.getSegmentFieldValue();
		}
		return null;
	}

	/**
	 * Postfix the path segment for the container that matches the given parameters. This operation is not needed for basenodes (since segment must be / for
	 * those anyway).
	 * 
	 * @param branchUuid
	 * @param type
	 * @param languageTag
	 */
	public void postfixPathSegment(String branchUuid, ContainerType type, String languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(branchUuid) == null) {
			return;
		}

		// Find the first matching container and fallback to other listed languages
		HibNodeFieldContainer container = getFieldContainer(languageTag, branchUuid, type);
		if (container != null) {
			container.postfixSegmentFieldValue();
		}
	}

	@Override
	public String getPath(ActionContext ac, String branchUuid, ContainerType type, String... languageTag) {
		NodeDao nodeDao = GraphDBTx.getGraphTx().nodeDao();

		// We want to avoid rending the path again for nodes which we have already handled.
		// Thus utilise the action context data map to retrieve already handled paths.
		String cacheKey = getUuid() + branchUuid + type.getCode() + Arrays.toString(languageTag);
		return (String) ac.data().computeIfAbsent(cacheKey, key -> {

			List<String> segments = new ArrayList<>();
			String segment = getPathSegment(branchUuid, type, languageTag);
			if (segment == null) {
				// Fall back to url fields
				return getUrlFieldPath(branchUuid, type, languageTag);
			}
			segments.add(segment);

			// For the path segments of the container, we add all (additional)
			// project languages to the list of languages for the fallback.
			HibNode current = this;
			while (current != null) {
				current = nodeDao.getParentNode(current, branchUuid);
				if (current == null || nodeDao.getParentNode(current, branchUuid) == null) {
					break;
				}
				// For the path segments of the container, we allow ANY language (of the project)
				segment = toGraph(current).getPathSegment(branchUuid, type, true, languageTag);

				// Abort early if one of the path segments could not be resolved.
				// We need to fall back to url fields in those cases.
				if (segment == null) {
					return getUrlFieldPath(branchUuid, type, languageTag);
				}
				segments.add(segment);
			}

			Collections.reverse(segments);

			// Finally construct the path from all segments
			StringBuilder builder = new StringBuilder();

			// Append the prefix first
			BranchDao branchDao = GraphDBTx.getGraphTx().branchDao();
			HibBranch branch = branchDao.findByUuid(getProject(), branchUuid);
			if (branch != null) {
				String prefix = PathPrefixUtil.sanitize(branch.getPathPrefix());
				if (!prefix.isEmpty()) {
					String[] prefixSegments = prefix.split("/");
					for (String prefixSegment : prefixSegments) {
						if (prefixSegment.isEmpty()) {
							continue;
						}
						builder.append("/").append(URIUtils.encodeSegment(prefixSegment));
					}
				}
			}

			Iterator<String> it = segments.iterator();
			while (it.hasNext()) {
				String currentSegment = it.next();
				builder.append("/").append(URIUtils.encodeSegment(currentSegment));
			}
			return builder.toString();
		});

	}

	/**
	 * Return the first url field path found.
	 *
	 * @param branchUuid
	 * @param type
	 * @param languages
	 *            The order of languages will be used to search for the url field values.
	 * @return null if no url field could be found.
	 */
	private String getUrlFieldPath(String branchUuid, ContainerType type, String... languages) {
		return Stream.of(languages)
			.flatMap(language -> Stream.ofNullable(getFieldContainer(language, branchUuid, type)))
			.flatMap(HibNodeFieldContainer::getUrlFieldValues)
			.findFirst()
			.orElse(null);
	}

	private void assertPublishConsistency(InternalActionContext ac, HibBranch branch) {

		String branchUuid = branch.getUuid();
		// Check whether the node got a published version and thus is published

		boolean isPublished = hasPublishedContent(branchUuid);

		// A published node must have also a published parent node.
		if (isPublished) {
			NodeImpl parentNode = getParentNode(branchUuid);

			// Only assert consistency of parent nodes which are not project
			// base nodes.
			if (parentNode != null && (!parentNode.getUuid().equals(getProject().getBaseNode().getUuid()))) {

				// Check whether the parent node has a published field container
				// for the given branch and language
				if (!parentNode.hasPublishedContent(branchUuid)) {
					log.error("Could not find published field container for node {" + parentNode.getUuid() + "} in branch {" + branchUuid + "}");
					throw error(BAD_REQUEST, "node_error_parent_containers_not_published", parentNode.getUuid());
				}
			}
		}

		// A draft node can't have any published child nodes.
		if (!isPublished) {

			for (HibNode node : getChildren(branchUuid)) {
				NodeImpl impl = (NodeImpl) node;
				if (impl.hasPublishedContent(branchUuid)) {
					log.error("Found published field container for node {" + node.getUuid() + "} in branch {" + branchUuid + "}. Node is child of {"
						+ getUuid() + "}");
					throw error(BAD_REQUEST, "node_error_children_containers_still_published", node.getUuid());
				}
			}
		}
	}

	@Override
	public Result<HibTag> getTags(HibBranch branch) {
		return new TraversalResult<>(TagEdgeImpl.getTagTraversal(this, branch).frameExplicit(TagImpl.class));
	}

	@Override
	public boolean hasTag(HibTag tag, HibBranch branch) {
		return TagEdgeImpl.hasTag(this, tag, branch);
	}

	private boolean hasPublishedContent(String branchUuid) {
		return GraphFieldContainerEdgeImpl.matchesBranchAndType(getId(), branchUuid, PUBLISHED);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(String branchUuid, ContainerType type) {
		Result<GraphFieldContainerEdgeImpl> it = GraphFieldContainerEdgeImpl.findEdges(this.getId(), branchUuid, type);
		Iterator<NodeGraphFieldContainer> it2 = it.stream().map(e -> e.getNodeContainer()).iterator();
		return new TraversalResult<>(it2);
	}

	@Override
	public Result<HibNodeFieldContainer> getFieldContainers(ContainerType type) {
		return new TraversalResult<>(
			outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV()
				.frameExplicit(NodeGraphFieldContainerImpl.class));
	}

	@Override
	@SuppressWarnings("unchecked")
	public long getFieldContainerCount() {
		return outE(HAS_FIELD_CONTAINER).or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, DRAFT.getCode()), e -> e.traversal()
			.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, PUBLISHED.getCode())).inV().count();
	}

	@Override
	public HibNodeFieldContainer getLatestDraftFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestBranch(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag, HibBranch branch, ContainerType type) {
		return getGraphFieldContainer(languageTag, branch, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestBranch().getUuid(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer getFieldContainer(String languageTag, String branchUuid, ContainerType type) {
		return getGraphFieldContainer(languageTag, branchUuid, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public HibNodeFieldContainer createFieldContainer(String languageTag, HibBranch branch, HibUser editor) {
		return createFieldContainer(languageTag, branch, editor, null, true);
	}

	@Override
	public HibNodeFieldContainer createFieldContainer(String languageTag, HibBranch branch, HibUser editor, HibNodeFieldContainer original,
		boolean handleDraftEdge) {
		NodeGraphFieldContainerImpl previous = null;
		EdgeFrame draftEdge = null;
		String branchUuid = branch.getUuid();

		// check whether there is a current draft version
		if (handleDraftEdge) {
			draftEdge = getGraphFieldContainerEdgeFrame(languageTag, branchUuid, DRAFT);
			if (draftEdge != null) {
				previous = draftEdge.inV().nextOrDefault(NodeGraphFieldContainerImpl.class, null);
			}
		}

		// Create the new container
		NodeGraphFieldContainerImpl newContainer = getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		newContainer.generateBucketId();
		if (original != null) {
			newContainer.setEditor(editor);
			newContainer.setLastEditedTimestamp();
			newContainer.setLanguageTag(languageTag);
			newContainer.setSchemaContainerVersion(original.getSchemaContainerVersion());
		} else {
			newContainer.setEditor(editor);
			newContainer.setLastEditedTimestamp();
			newContainer.setLanguageTag(languageTag);
			// We need create a new container with no reference. So use the latest version available to use.
			newContainer.setSchemaContainerVersion(branch.findLatestSchemaVersion(getSchemaContainer()));
		}
		if (previous != null) {
			// set the next version number
			newContainer.setVersion(previous.getVersion().nextDraft());
			previous.setNextVersion(newContainer);
		} else {
			// set the initial version number
			newContainer.setVersion(new VersionNumber());
		}

		// clone the original or the previous container
		if (original != null) {
			newContainer.clone(original);
		} else if (previous != null) {
			newContainer.clone(previous);
		}

		// remove existing draft edge
		if (draftEdge != null) {
			draftEdge.remove();
			newContainer.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_update");
		}
		// We need to update the display field property since we created a new
		// node graph field container.
		newContainer.updateDisplayFieldValue();

		if (handleDraftEdge) {
			// create a new draft edge
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, newContainer, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(languageTag);
			edge.setBranchUuid(branchUuid);
			edge.setType(DRAFT);
		}

		// if there is no initial edge, create one
		if (getGraphFieldContainerEdge(languageTag, branchUuid, INITIAL) == null) {
			GraphFieldContainerEdge initialEdge = addFramedEdge(HAS_FIELD_CONTAINER, newContainer, GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(languageTag);
			initialEdge.setBranchUuid(branchUuid);
			initialEdge.setType(INITIAL);
		}

		return newContainer;
	}

	@Override
	public EdgeFrame getGraphFieldContainerEdgeFrame(String languageTag, String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdge(getId(), branchUuid, type.getCode(), languageTag);
	}

	/**
	 * Get all graph field edges.
	 *
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	protected Result<? extends GraphFieldContainerEdgeImpl> getFieldContainerEdges(String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdges(getId(), branchUuid, type);
	}

	@Override
	public void addTag(HibTag tag, HibBranch branch) {
		removeTag(tag, branch);
		TagEdge edge = addFramedEdge(HAS_TAG, toGraph(tag), TagEdgeImpl.class);
		edge.setBranchUuid(branch.getUuid());
	}

	@Override
	public void removeTag(HibTag tag, HibBranch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).mark().inV().retain(toGraph(tag)).back().removeAll();
	}

	@Override
	public void removeAllTags(HibBranch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).removeAll();
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
		return new TraversalResult<>(graph.frameExplicit(db().getVertices(
			NodeImpl.class,
			new String[] { PARENTS_KEY_PROPERTY },
			new Object[] { getUuid() }), NodeImpl.class));
	}

	@Override
	public Result<HibNode> getChildren(String branchUuid) {
		return new TraversalResult<>(graph.frameExplicit(getUnframedChildren(branchUuid), NodeImpl.class));
	}

	private Iterator<Vertex> getUnframedChildren(String branchUuid) {
		return db().getVertices(
			NodeImpl.class,
			new String[] { BRANCH_PARENTS_KEY_PROPERTY },
			new Object[] { branchParentEntry(branchUuid, getUuid()).encode() });
	}

	@Override
	public Stream<Node> getChildrenStream(InternalActionContext ac) {
		HibUser user = ac.getUser();
		Tx tx = GraphDBTx.getGraphTx();
		UserDao userDao = tx.userDao();
		return toStream(getUnframedChildren(tx.getBranch(ac).getUuid()))
			.filter(node -> {
				Object id = node.getId();
				return userDao.hasPermissionForId(user, id, READ_PERM) || userDao.hasPermissionForId(user, id, READ_PUBLISHED_PERM);
			}).map(node -> graph.frameElementExplicit(node, NodeImpl.class));
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
	public HibProject getProject() {
		return db().index().findByUuid(ProjectImpl.class, property(PROJECT_KEY_PROPERTY));
	}

	@Override
	public void setProject(HibProject project) {
		property(PROJECT_KEY_PROPERTY, project.getUuid());
	}

	@Override
	public HibNode create(HibUser creator, HibSchemaVersion schemaVersion, HibProject project) {
		return create(creator, schemaVersion, project, project.getLatestBranch());
	}

	/**
	 * Create a new node and make sure to delegate the creation request to the main node root aggregation node.
	 */
	@Override
	public HibNode create(HibUser creator, HibSchemaVersion schemaVersion, HibProject project, HibBranch branch, String uuid) {
		if (!isBaseNode() && !isVisibleInBranch(branch.getUuid())) {
			log.error(String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
				branch.getName(), getUuid()));
			throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
		}

		Node node = toGraph(project).getNodeRoot().create(creator, schemaVersion, project, uuid);
		node.setParentNode(branch.getUuid(), this);
		node.setSchemaContainer(schemaVersion.getSchemaContainer());
		// setCreated(creator);
		return node;
	}

	@Override
	public Result<HibNode> getBreadcrumbNodes(InternalActionContext ac) {
		return new TraversalResult<>(() -> CommonTx.get().nodeDao().getBreadcrumbNodeStream(this, ac).iterator());
	}

	@Override
	public NavigationResponse transformToNavigation(InternalActionContext ac) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		if (parameters.getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		Tx tx = GraphDBTx.getGraphTx();
		// TODO assure that the schema version is correct
		if (!getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
			throw error(BAD_REQUEST, "navigation_error_no_container");
		}
		String etagKey = buildNavigationEtagKey(ac, this, parameters.getMaxDepth(), 0, tx.getBranch(ac, getProject()).getUuid(), forVersion(ac
			.getVersioningParameters().getVersion()));
		String etag = ETag.hash(etagKey);
		ac.setEtag(etag, true);
		if (ac.matches(etag, true)) {
			throw new NotModifiedException();
		} else {
			NavigationResponse response = new NavigationResponse();
			return buildNavigationResponse(ac, this, parameters.getMaxDepth(), 0, response, response, tx.getBranch(ac, getProject()).getUuid(),
				forVersion(ac.getVersioningParameters().getVersion()));
		}
	}

	@Override
	public NodeVersionsResponse transformToVersionList(InternalActionContext ac) {
		NodeVersionsResponse response = new NodeVersionsResponse();
		Map<String, List<VersionInfo>> versions = new HashMap<>();
		getFieldContainers(GraphDBTx.getGraphTx().getBranch(ac), DRAFT).forEach(c -> {
			versions.put(c.getLanguageTag(), c.versions().stream()
				.map(v -> v.transformToVersionInfo(ac))
				.collect(Collectors.toList()));
		});

		response.setVersions(versions);
		return response;
	}

	/**
	 * Generate the etag key for the requested navigation.
	 *
	 * @param ac
	 * @param node
	 *            Current node to start building the navigation
	 * @param maxDepth
	 *            Maximum depth of navigation
	 * @param level
	 *            Current level of recursion
	 * @param branchUuid
	 *            Branch uuid used to extract selected tree structure
	 * @param type
	 * @return
	 */
	private String buildNavigationEtagKey(InternalActionContext ac, NodeImpl node, int maxDepth, int level, String branchUuid, ContainerType type) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		StringBuilder builder = new StringBuilder();
		builder.append(node.getETag(ac));

		List<HibNode> nodes = node.getChildren(ac.getUser(), branchUuid, null, type).collect(Collectors.toList());

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return builder.toString();
		}
		for (HibNode child : nodes) {
			if (child.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				builder.append(buildNavigationEtagKey(ac, (NodeImpl) child, maxDepth, level + 1, branchUuid, type));
			} else if (parameters.isIncludeAll()) {
				builder.append(buildNavigationEtagKey(ac, (NodeImpl) child, maxDepth, level, branchUuid, type));
			}
		}
		return builder.toString();
	}

	/**
	 * Recursively build the navigation response.
	 *
	 * @param ac
	 *            Action context
	 * @param node
	 *            Current node that should be handled in combination with the given navigation element
	 * @param maxDepth
	 *            Maximum depth for the navigation
	 * @param level
	 *            Zero based level of the current navigation element
	 * @param navigation
	 *            Current navigation response
	 * @param currentElement
	 *            Current navigation element for the given level
	 * @param branchUuid
	 *            Branch uuid to be used for loading children of nodes
	 * @param type
	 *            container type to be used for transformation
	 * @return
	 */
	private NavigationResponse buildNavigationResponse(InternalActionContext ac, NodeImpl node, int maxDepth, int level,
		NavigationResponse navigation, NavigationElement currentElement, String branchUuid, ContainerType type) {
		List<HibNode> nodes = node.getChildren(ac.getUser(), branchUuid, null, type).collect(Collectors.toList());
		List<NavigationResponse> responses = new ArrayList<>();

		NodeResponse response = Tx.get().nodeDao().transformToRestSync(node, ac, 0);
		currentElement.setUuid(response.getUuid());
		currentElement.setNode(response);
		responses.add(navigation);

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return responses.get(responses.size() - 1);
		}
		NavigationParameters parameters = new NavigationParametersImpl(ac);
		// Add children
		for (HibNode child : nodes) {
			// TODO assure that the schema version is correct?
			// TODO also allow navigations over containers
			if (child.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				NavigationElement childElement = new NavigationElement();
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				currentElement.getChildren().add(childElement);
				responses.add(buildNavigationResponse(ac, (NodeImpl) child, maxDepth, level + 1, navigation, childElement, branchUuid, type));
			} else if (parameters.isIncludeAll()) {
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				NavigationElement childElement = new NavigationElement();
				currentElement.getChildren().add(childElement);
				responses.add(buildNavigationResponse(ac, (NodeImpl) child, maxDepth, level, navigation, childElement, branchUuid, type));
			}
		}
		return responses.get(responses.size() - 1);
	}

	@Override
	public NodeReference transformToReference(InternalActionContext ac) {
		Tx tx = GraphDBTx.getGraphTx();
		HibBranch branch = tx.getBranch(ac, getProject());

		NodeReference nodeReference = new NodeReference();
		nodeReference.setUuid(getUuid());
		nodeReference.setDisplayName(getDisplayName(ac));
		nodeReference.setSchema(getSchemaContainer().transformToReference());
		nodeReference.setProjectName(getProject().getName());
		if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
			WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
			ContainerType type = forVersion(ac.getVersioningParameters().getVersion());
			String url = linkReplacer.resolve(ac, branch.getUuid(), type, this, ac.getNodeParameters().getResolveLinks(), ac.getNodeParameters()
				.getLanguages());
			nodeReference.setPath(url);
		}
		return nodeReference;
	}

	@Override
	public NodeReference transformToMinimalReference() {
		NodeReference ref = new NodeReference();
		ref.setUuid(getUuid());
		ref.setSchema(getSchemaContainer().transformToReference());
		return ref;
	}

	/**
	 * Create a {@link NodeFieldListItem} that contains the reference to this node.
	 * 
	 * @param ac
	 * @param languageTags
	 * @return
	 */
	public NodeFieldListItem toListItem(InternalActionContext ac, String[] languageTags) {
		Tx tx = GraphDBTx.getGraphTx();
		// Create the rest field and populate the fields
		NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(getUuid());
		String branchUuid = tx.getBranch(ac, getProject()).getUuid();
		ContainerType type = forVersion(new VersioningParametersImpl(ac).getVersion());
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			listItem.setUrl(mesh().webRootLinkReplacer().resolve(ac, branchUuid, type, this, ac.getNodeParameters().getResolveLinks(),
				languageTags));
		}
		return listItem;
	}

	@Override
	public PublishStatusResponse transformToPublishStatus(InternalActionContext ac) {
		PublishStatusResponse publishStatus = new PublishStatusResponse();
		Map<String, PublishStatusModel> languages = CommonTx.get().nodeDao().getLanguageInfo(this, ac);
		publishStatus.setAvailableLanguages(languages);
		return publishStatus;
	}

	@Override
	public void publish(InternalActionContext ac, BulkActionContext bac) {
		Tx tx = GraphDBTx.getGraphTx();
		HibBranch branch = tx.getBranch(ac, getProject());
		String branchUuid = branch.getUuid();

		List<HibNodeFieldContainer> unpublishedContainers = getFieldContainers(branch, ContainerType.DRAFT).stream().filter(c -> !c
			.isPublished(branchUuid)).collect(Collectors.toList());

		// publish all unpublished containers and handle recursion
		unpublishedContainers.stream().forEach(c -> {
			HibNodeFieldContainer newVersion = publish(ac, c.getLanguageTag(), branch, ac.getUser());
			bac.add(newVersion.onPublish(branchUuid));
		});
		assertPublishConsistency(ac, branch);

		// Handle recursion after publishing the current node.
		// This is done to ensure the publish consistency.
		// Even if the publishing process stops at the initial
		// level the consistency is correct.
		PublishParameters parameters = ac.getPublishParameters();
		if (parameters.isRecursive()) {
			for (HibNode node : getChildren(branchUuid)) {
				node.publish(ac, bac);
			}
		}
		bac.process();
	}

	private void takeOffline(InternalActionContext ac, BulkActionContext bac, HibBranch branch, PublishParameters parameters) {
		// Handle recursion first to start at the leaves
		if (parameters.isRecursive()) {
			for (HibNode node : getChildren(branch.getUuid())) {
				NodeImpl impl = (NodeImpl) node;
				impl.takeOffline(ac, bac, branch, parameters);
			}
		}

		String branchUuid = branch.getUuid();

		Result<? extends GraphFieldContainerEdgeImpl> publishEdges = getFieldContainerEdges(branchUuid, PUBLISHED);

		// Remove the published edge for each found container
		publishEdges.forEach(edge -> {
			NodeGraphFieldContainer content = edge.getNodeContainer();
			bac.add(content.onTakenOffline(branchUuid));
			edge.remove();
			if (content.isAutoPurgeEnabled() && content.isPurgeable()) {
				content.purge(bac);
			}
		});

		assertPublishConsistency(ac, branch);

		bac.process();
	}

	@Override
	public void takeOffline(InternalActionContext ac, BulkActionContext bac) {
		Tx tx = GraphDBTx.getGraphTx();
		HibBranch branch = tx.getBranch(ac, getProject());
		PublishParameters parameters = ac.getPublishParameters();
		takeOffline(ac, bac, branch, parameters);
	}

	@Override
	public PublishStatusModel transformToPublishStatus(InternalActionContext ac, String languageTag) {
		Tx tx = GraphDBTx.getGraphTx();
		HibBranch branch = tx.getBranch(ac, getProject());

		HibNodeFieldContainer container = getFieldContainer(languageTag, branch.getUuid(), PUBLISHED);
		if (container != null) {
			String date = container.getLastEditedDate();
			PublishStatusModel status = new PublishStatusModel();
			status.setPublished(true);
			status.setVersion(container.getVersion().toString());
			HibUser editor = container.getEditor();
			if (editor != null) {
				status.setPublisher(editor.transformToReference());
			}
			status.setPublishDate(date);
			return status;
		} else {
			container = getFieldContainer(languageTag, branch.getUuid(), DRAFT);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			return new PublishStatusModel().setPublished(false).setVersion(container.getVersion().toString());
		}
	}

	@Override
	public void publish(InternalActionContext ac, BulkActionContext bac, String languageTag) {
		Tx tx = GraphDBTx.getGraphTx();
		HibBranch branch = tx.getBranch(ac, getProject());
		String branchUuid = branch.getUuid();

		// get the draft version of the given language
		HibNodeFieldContainer draftVersion = getFieldContainer(languageTag, branchUuid, DRAFT);

		// if not existent -> NOT_FOUND
		if (draftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		// If the located draft version was already published we are done
		if (draftVersion.isPublished(branchUuid)) {
			return;
		}

		// TODO check whether all required fields are filled, if not -> unable to publish
		HibNodeFieldContainer publishedContainer = publish(ac, draftVersion.getLanguageTag(), branch, ac.getUser());
		// Invoke a store of the document since it must now also be added to the published index
		bac.add(publishedContainer.onPublish(branchUuid));
	}

	@Override
	public void takeOffline(InternalActionContext ac, BulkActionContext bac, HibBranch branch, String languageTag) {
		String branchUuid = branch.getUuid();

		// Locate the published container
		HibNodeFieldContainer published = getFieldContainer(languageTag, branchUuid, PUBLISHED);
		if (published == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}
		bac.add(published.onTakenOffline(branchUuid));

		// Remove the "published" edge
		getGraphFieldContainerEdge(languageTag, branchUuid, PUBLISHED).remove();
		assertPublishConsistency(ac, branch);

		bac.process();
	}

	@Override
	public void setPublished(InternalActionContext ac, HibNodeFieldContainer container, String branchUuid) {
		String languageTag = container.getLanguageTag();
		boolean isAutoPurgeEnabled = container.isAutoPurgeEnabled();

		// Remove an existing published edge
		EdgeFrame currentPublished = getGraphFieldContainerEdgeFrame(languageTag, branchUuid, PUBLISHED);
		if (currentPublished != null) {
			// We need to remove the edge first since updateWebrootPathInfo will
			// check the published edge again
			NodeGraphFieldContainerImpl oldPublishedContainer = currentPublished.inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
			currentPublished.remove();
			oldPublishedContainer.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_publish");
			if (ac.isPurgeAllowed() && isAutoPurgeEnabled && oldPublishedContainer.isPurgeable()) {
				oldPublishedContainer.purge();
			}
		}

		if (ac.isPurgeAllowed()) {
			// Check whether a previous draft can be purged.
			HibNodeFieldContainer prev = container.getPreviousVersion();
			if (isAutoPurgeEnabled && prev != null && prev.isPurgeable()) {
				prev.purge();
			}
		}

		// create new published edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, toGraph(container), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setBranchUuid(branchUuid);
		edge.setType(PUBLISHED);
		container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_publish");
	}

	@Override
	public HibNodeFieldContainer publish(InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
		String branchUuid = branch.getUuid();

		// create published version
		HibNodeFieldContainer newVersion = createFieldContainer(languageTag, branch, user);
		newVersion.setVersion(newVersion.getVersion().nextPublished());

		setPublished(ac, newVersion, branchUuid);
		return newVersion;
	}

	@Override
	public HibNodeFieldContainer findVersion(List<String> languageTags, String branchUuid, String version) {
		HibNodeFieldContainer fieldContainer = null;

		// TODO refactor the type handling and don't return INITIAL.
		ContainerType type = forVersion(version);

		for (String languageTag : languageTags) {

			// Don't start the version lookup using the initial version. Instead start at the end of the chain and use the DRAFT version instead.
			fieldContainer = getFieldContainer(languageTag, branchUuid, type == INITIAL ? DRAFT : type);

			// Traverse the chain downwards and stop once we found our target version or we reached the end.
			if (fieldContainer != null && type == INITIAL) {
				while (fieldContainer != null && !version.equals(fieldContainer.getVersion().toString())) {
					fieldContainer = fieldContainer.getPreviousVersion();
				}
			}

			// We found a container for one of the languages
			if (fieldContainer != null) {
				break;
			}
		}
		return fieldContainer;
	}

	@Override
	public List<String> getAvailableLanguageNames() {
		List<String> languageTags = new ArrayList<>();
		// TODO it would be better to store the languagetag along with the edge
		for (HibNodeFieldContainer container : getDraftFieldContainers()) {
			languageTags.add(container.getLanguageTag());
		}
		return languageTags;
	}

	@Override
	public void delete(BulkActionContext bac, boolean ignoreChecks, boolean recursive) {
		if (!ignoreChecks) {
			// Prevent deletion of basenode
			if (getProject().getBaseNode().getUuid().equals(getUuid())) {
				throw error(METHOD_NOT_ALLOWED, "node_basenode_not_deletable");
			}
		}
		// Delete subfolders
		if (log.isDebugEnabled()) {
			log.debug("Deleting node {" + getUuid() + "}");
		}
		if (recursive) {
			// No need to check the branch since this delete must affect all branches
			for (HibNode child : getChildren()) {
				toGraph(child).delete(bac);
				bac.process();
			}
		}

		// Delete all initial containers (which will delete all containers)
		for (HibNodeFieldContainer container : getFieldContainers(INITIAL)) {
			container.delete(bac);
		}
		if (log.isDebugEnabled()) {
			log.debug("Deleting node {" + getUuid() + "} vertex.");
		}

		addReferenceUpdates(bac);

		bac.add(onDeleted(getUuid(), getSchemaContainer(), null, null, null));
		getElement().remove();
		bac.process();
	}

	@Override
	public Stream<HibNodeField> getInboundReferences() {
		return toStream(inE(HAS_FIELD, HAS_ITEM)
			.has(NodeGraphFieldImpl.class)
			.frameExplicit(NodeGraphFieldImpl.class));
	}

	/**
	 * Adds reference update events to the context for all draft and published contents that reference this node.
	 *
	 * @param bac
	 */
	private void addReferenceUpdates(BulkActionContext bac) {
		Set<String> handledNodeUuids = new HashSet<>();

		getInboundReferences()
			.flatMap(HibNodeField::getReferencingContents)
			.forEach(nodeContainer -> {
				for (GraphFieldContainerEdgeImpl edge : toGraph(nodeContainer).inE(HAS_FIELD_CONTAINER, GraphFieldContainerEdgeImpl.class)) {
					ContainerType type = edge.getType();
					// Only handle published or draft contents
					if (type.equals(DRAFT) || type.equals(PUBLISHED)) {
						HibNode node = nodeContainer.getNode();
						String uuid = node.getUuid();
						String languageTag = nodeContainer.getLanguageTag();
						String branchUuid = edge.getBranchUuid();
						String key = uuid + languageTag + branchUuid + type.getCode();
						if (!handledNodeUuids.contains(key)) {
							bac.add(onReferenceUpdated(node.getUuid(), node.getSchemaContainer(), branchUuid, type, languageTag));
							handledNodeUuids.add(key);
						}
					}
				}
			});
	}

	@Override
	public void delete(BulkActionContext bac) {
		delete(bac, false, true);
	}

	@Override
	public void deleteFromBranch(InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks) {

		DeleteParameters parameters = ac.getDeleteParameters();

		// 1. Remove subfolders from branch
		String branchUuid = branch.getUuid();

		for (HibNode child : getChildren(branchUuid)) {
			if (!parameters.isRecursive()) {
				throw error(BAD_REQUEST, "node_error_delete_failed_node_has_children");
			}
			child.deleteFromBranch(ac, branch, bac, ignoreChecks);
		}

		// 2. Delete all language containers
		for (HibNodeFieldContainer container : getFieldContainers(branch, DRAFT)) {
			deleteLanguageContainer(ac, branch, container.getLanguageTag(), bac, false);
		}

		// 3. Now check if the node has no more field containers in any branch. We can delete it in those cases
		if (getFieldContainerCount() == 0) {
			delete(bac);
		} else {
			removeParent(branchUuid);
		}
	}

	private void removeParent(String branchUuid) {
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

		Predicate<HibNode> languageFilter = languageTags == null || languageTags.isEmpty()
			? item -> true
			: item -> languageTags.stream().anyMatch(languageTag -> item.getFieldContainer(languageTag, branchUuid, type) != null);

		return getChildren(branchUuid).stream()
			.filter(languageFilter.and(item -> userRoot.hasPermission(requestUser, item, perm)));
	}

	@Override
	public Page<HibNode> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingInfo) {
		return new DynamicTransformableStreamPageImpl<>(getChildren(ac.getUser(), branchUuid, languageTags, type), pagingInfo);
	}

	@Override
	public Page<? extends HibTag> getTags(HibUser user, PagingParameters params, HibBranch branch) {
		VertexTraversal<?, ?, ?> traversal = TagEdgeImpl.getTagTraversal(this, branch);
		return new DynamicTransformablePageImpl<>(user, traversal, params, READ_PERM, TagImpl.class);
	}

	@Override
	public boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		boolean permissionChanged = false;
		if (recursive) {
			// We don't need to filter by branch. Branch nodes can't have dedicated perms
			for (HibNode child : getChildren()) {
				permissionChanged = child.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}

	@Override
	public String getDisplayName(InternalActionContext ac) {
		NodeParameters nodeParameters = ac.getNodeParameters();
		VersioningParameters versioningParameters = ac.getVersioningParameters();

		HibNodeFieldContainer container = findVersion(nodeParameters.getLanguageList(options()), GraphDBTx.getGraphTx().getBranch(ac, getProject()).getUuid(),
			versioningParameters
				.getVersion());
		if (container == null) {
			if (log.isDebugEnabled()) {
				log.debug("Could not find any matching i18n field container for node {" + getUuid() + "}.");
			}
			return null;
		} else {
			// Determine the display field name and load the string value
			// from that field.
			return container.getDisplayFieldValue();
		}

	}

	/**
	 * Update the node language or create a new draft for the specific language. This method will also apply conflict detection and take care of deduplication.
	 *
	 *
	 * <p>
	 * Conflict detection: Conflict detection only occurs during update requests. Two diffs are created. The update request will be compared against base
	 * version graph field container (version which is referenced by the request). The second diff is being created in-between the base version graph field
	 * container and the latest version of the graph field container. This diff identifies previous changes in between those version. These both diffs are
	 * compared in order to determine their intersection. The intersection identifies those fields which have been altered in between both versions and which
	 * would now also be touched by the current request. This situation causes a conflict and the update would abort.
	 *
	 * <p>
	 * Conflict cases
	 * <ul>
	 * <li>Initial creates - No conflict handling needs to be performed</li>
	 * <li>Migration check - Nodes which have not yet migrated can't be updated</li>
	 * </ul>
	 *
	 *
	 * <p>
	 * Deduplication: Field values that have not been changed in between the request data and the last version will not cause new fields to be created in new
	 * version graph field containers. The new version graph field container will instead reference those fields from the previous graph field container
	 * version. Please note that this deduplication only applies to complex fields (e.g.: Lists, Micronode)
	 *
	 * @param ac
	 * @param batch
	 *            Batch which will be used to update the search index
	 * @return
	 */
	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		NodeUpdateRequest requestModel = ac.fromJson(NodeUpdateRequest.class);
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "error_language_not_set");
		}

		// Check whether the tags need to be updated
		List<TagReference> tags = requestModel.getTags();
		if (tags != null) {
			updateTags(ac, batch, requestModel.getTags());
		}

		// Set the language tag parameter here in order to return the updated language in the response
		String languageTag = requestModel.getLanguage();
		NodeParameters nodeParameters = ac.getNodeParameters();
		nodeParameters.setLanguages(languageTag);

		HibLanguage language = GraphDBTx.getGraphTx().languageDao().findByLanguageTag(languageTag);
		if (language == null) {
			throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
		}
		Tx tx = GraphDBTx.getGraphTx();
		NodeDao nodeDao = tx.nodeDao();
		HibBranch branch = tx.getBranch(ac, getProject());
		HibNodeFieldContainer latestDraftVersion = getFieldContainer(languageTag, branch, DRAFT);

		// Check whether this is the first time that an update for the given language and branch occurs. In this case a new container must be created.
		// This means that no conflict check can be performed. Conflict checks only occur for updates on existing contents.
		if (latestDraftVersion == null) {
			// Create a new field container
			latestDraftVersion = createFieldContainer(languageTag, branch, ac.getUser());

			// Check whether the node has a parent node in this branch, if not, the request is supposed to be a create request
			// and we get the parent node from this create request
			if (getParentNode(branch.getUuid()) == null) {
				NodeCreateRequest createRequest = JsonUtil.readValue(ac.getBodyAsString(), NodeCreateRequest.class);
				if (createRequest.getParentNode() == null || isEmpty(createRequest.getParentNode().getUuid())) {
					throw error(BAD_REQUEST, "node_missing_parentnode_field");
				}
				HibNode parentNode = nodeDao.loadObjectByUuid(getProject(), ac, createRequest.getParentNode().getUuid(), CREATE_PERM);
				Node graphParentNode = toGraph(parentNode);
				// check whether the parent node is visible in the branch
				if (!graphParentNode.isBaseNode() && !graphParentNode.isVisibleInBranch(branch.getUuid())) {
					log.error(
						String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
							branch.getName(), parentNode.getUuid()));
					throw error(NOT_FOUND, "object_not_found_for_uuid", createRequest.getParentNode().getUuid());
				}
				setParentNode(branch.getUuid(), graphParentNode);
			}

			latestDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());
			batch.add(latestDraftVersion.onCreated(branch.getUuid(), DRAFT));
			return true;
		} else {
			String version = requestModel.getVersion();
			if (version == null) {
				log.debug("No version was specified. Assuming 'draft' for latest version");
				version = "draft";
			}

			// Make sure the container was already migrated. Otherwise the update can't proceed.
			HibSchemaVersion schemaVersion = latestDraftVersion.getSchemaContainerVersion();
			if (!latestDraftVersion.getSchemaContainerVersion().equals(branch.findLatestSchemaVersion(schemaVersion
				.getSchemaContainer()))) {
				throw error(BAD_REQUEST, "node_error_migration_incomplete");
			}

			// Load the base version field container in order to create the diff
			HibNodeFieldContainer baseVersionContainer = findVersion(requestModel.getLanguage(), branch.getUuid(), version);
			if (baseVersionContainer == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", version, requestModel.getLanguage());
			}

			latestDraftVersion.getSchemaContainerVersion().getSchema().assertForUnhandledFields(requestModel.getFields());

			// TODO handle simplified case in which baseContainerVersion and
			// latestDraftVersion are equal
			List<FieldContainerChange> baseVersionDiff = baseVersionContainer.compareTo(latestDraftVersion);
			List<FieldContainerChange> requestVersionDiff = latestDraftVersion.compareTo(requestModel.getFields());

			// Compare both sets of change sets
			List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains).collect(Collectors.toList());

			// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
			if (!latestDraftVersion.getVersion().getFullVersion().equals(version)) {

				// Check whether a conflict has been detected
				if (intersect.size() > 0) {
					NodeVersionConflictException conflictException = new NodeVersionConflictException("node_error_conflict_detected");
					conflictException.setOldVersion(baseVersionContainer.getVersion().toString());
					conflictException.setNewVersion(latestDraftVersion.getVersion().toString());
					for (FieldContainerChange fcc : intersect) {
						conflictException.addConflict(fcc.getFieldCoordinates());
					}
					throw conflictException;
				}
			}

			// Make sure to only update those fields which have been altered in between the latest version and the current request. Remove
			// unaffected fields from the rest request in order to prevent duplicate references. We don't want to touch field that have not been changed.
			// Otherwise the graph field references would no longer point to older revisions of the same field.
			Set<String> fieldsToKeepForUpdate = requestVersionDiff.stream().map(e -> e.getFieldKey()).collect(Collectors.toSet());
			for (String fieldKey : requestModel.getFields().keySet()) {
				if (fieldsToKeepForUpdate.contains(fieldKey)) {
					continue;
				}
				if (log.isDebugEnabled()) {
					log.debug("Removing field from request {" + fieldKey + "} in order to handle deduplication.");
				}
				requestModel.getFields().remove(fieldKey);
			}

			// Check whether the request still contains data which needs to be updated.
			if (!requestModel.getFields().isEmpty()) {

				// Create new field container as clone of the existing
				HibNodeFieldContainer newDraftVersion = createFieldContainer(language.getLanguageTag(), branch, ac.getUser(),
					latestDraftVersion, true);
				// Update the existing fields
				newDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());

				// Purge the old draft
				if (ac.isPurgeAllowed() && newDraftVersion.isAutoPurgeEnabled() && latestDraftVersion.isPurgeable()) {
					latestDraftVersion.purge();
				}

				latestDraftVersion = newDraftVersion;
				batch.add(newDraftVersion.onUpdated(branch.getUuid(), DRAFT));
				return true;
			}
		}
		return false;
	}

	@Override
	public Page<? extends HibTag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		Tx tx = GraphDBTx.getGraphTx();
		List<HibTag> tags = getTagsToSet(ac, batch);
		HibBranch branch = tx.getBranch(ac);
		applyTags(branch, tags, batch);
		HibUser user = ac.getUser();
		return getTags(user, ac.getPagingParameters(), branch);
	}

	@Override
	public void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> list) {
		Tx tx = GraphDBTx.getGraphTx();
		List<HibTag> tags = getTagsToSet(list, ac, batch);
		HibBranch branch = tx.getBranch(ac);
		applyTags(branch, tags, batch);
	}

	private void applyTags(HibBranch branch, List<? extends HibTag> tags, EventQueueBatch batch) {
		List<HibTag> currentTags = getTags(branch).list();

		List<HibTag> toBeAdded = tags.stream()
			.filter(StreamUtil.not(new HashSet<>(currentTags)::contains))
			.collect(Collectors.toList());
		toBeAdded.forEach(tag -> {
			addTag(tag, branch);
			batch.add(onTagged(tag, branch, ASSIGNED));
		});

		List<HibTag> toBeRemoved = currentTags.stream()
			.filter(StreamUtil.not(new HashSet<>(tags)::contains))
			.collect(Collectors.toList());
		toBeRemoved.forEach(tag -> {
			removeTag(tag, branch);
			batch.add(onTagged(tag, branch, UNASSIGNED));
		});
	}

	@Override
	public void moveTo(InternalActionContext ac, HibNode targetNode, EventQueueBatch batch) {
		Tx tx = GraphDBTx.getGraphTx();
		NodeDao nodeDao = tx.nodeDao();

		// TODO should we add a guard that terminates this loop when it runs to
		// long?

		// Check whether the target node is part of the subtree of the source
		// node.
		// We must detect and prevent such actions because those would
		// invalidate the tree structure
		HibBranch branch = tx.getBranch(ac, getProject());
		String branchUuid = branch.getUuid();
		HibNode parent = nodeDao.getParentNode(targetNode, branchUuid);
		while (parent != null) {
			if (parent.getUuid().equals(getUuid())) {
				throw error(BAD_REQUEST, "node_move_error_not_allowed_to_move_node_into_one_of_its_children");
			}
			parent = nodeDao.getParentNode(parent, branchUuid);
		}

		if (!targetNode.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
			throw error(BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		}

		if (getUuid().equals(targetNode.getUuid())) {
			throw error(BAD_REQUEST, "node_move_error_same_nodes");
		}

		setParentNode(branchUuid, targetNode);

		// Update published graph field containers
		getFieldContainers(branchUuid, PUBLISHED).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});

		// Update draft graph field containers
		getFieldContainers(branchUuid, DRAFT).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});
		batch.add(onNodeMoved(branchUuid, targetNode));
		assertPublishConsistency(ac, branch);
	}

	@Override
	public void deleteLanguageContainer(InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
		boolean failForLastContainer) {

		// 1. Check whether the container has also a published variant. We need to take it offline in those cases
		HibNodeFieldContainer container = getFieldContainer(languageTag, branch, PUBLISHED);
		if (container != null) {
			takeOffline(ac, bac, branch, languageTag);
		}

		// 2. Load the draft container and remove it from the branch
		container = getFieldContainer(languageTag, branch, DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", languageTag);
		}
		container.deleteFromBranch(branch, bac);
		// No need to delete the published variant because if the container was published the take offline call handled it

		// starting with the old draft, delete all GFC that have no next and are not draft (for other branches)
		HibNodeFieldContainer dangling = container;
		while (dangling != null && !dangling.isDraft() && !dangling.hasNextVersion()) {
			HibNodeFieldContainer toDelete = dangling;
			dangling = toDelete.getPreviousVersion();
			toDelete.delete(bac);
		}

		HibNodeFieldContainer initial = getFieldContainer(languageTag, branch, INITIAL);
		if (initial != null) {
			// Remove the initial edge
			toGraph(initial).inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid())
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).removeAll();

			// starting with the old initial, delete all GFC that have no previous and are not initial (for other branches)
			dangling = initial;
			while (dangling != null && !dangling.isInitial() && !dangling.hasPreviousVersion()) {
				HibNodeFieldContainer toDelete = dangling;
				// since the GFC "toDelete" was only used by this branch, it can not have more than one "next" GFC
				// (multiple "next" would have to belong to different branches, and for every branch, there would have to be
				// an INITIAL, which would have to be either this GFC or a previous)
				dangling = mesh().boot().contentDao().getNextVersions(toDelete).iterator().next();
				toDelete.delete(bac, false);
			}
		}

		// 3. Check whether this was be the last container of the node for this branch
		DeleteParameters parameters = ac.getDeleteParameters();
		if (failForLastContainer) {
			Result<HibNodeFieldContainer> draftContainers = getFieldContainers(branch.getUuid(), DRAFT);
			Result<HibNodeFieldContainer> publishContainers = getFieldContainers(branch.getUuid(), PUBLISHED);
			boolean wasLastContainer = !draftContainers.iterator().hasNext() && !publishContainers.iterator().hasNext();

			if (!parameters.isRecursive() && wasLastContainer) {
				throw error(BAD_REQUEST, "node_error_delete_failed_last_container_for_branch");
			}

			// Also delete the node and children
			if (parameters.isRecursive() && wasLastContainer) {
				deleteFromBranch(ac, branch, bac, false);
			}
		}

	}

	private PathSegment getSegment(String branchUuid, ContainerType type, String segment) {

		// Check the different language versions
		for (HibNodeFieldContainer container : getFieldContainers(branchUuid, type)) {
			SchemaModel schema = container.getSchemaContainerVersion().getSchema();
			String segmentFieldName = schema.getSegmentField();
			// First check whether a string field exists for the given name
			HibStringField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegmentImpl(container, field, container.getLanguageTag(), segment);
				}
			}

			// No luck yet - lets check whether a binary field matches the
			// segmentField
			HibBinaryField binaryField = container.getBinary(segmentFieldName);
			if (binaryField == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
						+ "}");
				}
			} else {
				String binaryFilename = binaryField.getFileName();
				if (segment.equals(binaryFilename)) {
					return new PathSegmentImpl(container, binaryField, container.getLanguageTag(), segment);
				}
			}
			// No luck yet - lets check whether a S3 binary field matches the segmentField
			S3HibBinaryField s3Binary = container.getS3Binary(segmentFieldName);
			if (s3Binary == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
							+ "}");
				}
			} else {
				String s3binaryFilename = s3Binary.getS3Binary().getFileName();
				if (segment.equals(s3binaryFilename)) {
					return new PathSegmentImpl(container, s3Binary, container.getLanguageTag(), segment);
				}
			}

		}
		return null;
	}

	@Override
	public Path resolvePath(String branchUuid, ContainerType type, Path path, Stack<String> pathStack) {
		if (pathStack.isEmpty()) {
			return path;
		}
		String segment = pathStack.pop();

		if (log.isDebugEnabled()) {
			log.debug("Resolving for path segment {" + segment + "}");
		}

		FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
		String segmentInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(this, segment);
		Object key = GraphFieldContainerEdgeImpl.composeWebrootIndexKey(db(), segmentInfo, branchUuid, type);
		Iterator<? extends GraphFieldContainerEdge> edges = graph.getFramedEdges(WEBROOT_INDEX_NAME, key, GraphFieldContainerEdgeImpl.class)
			.iterator();
		if (edges.hasNext()) {
			GraphFieldContainerEdge edge = edges.next();
			NodeImpl childNode = (NodeImpl) edge.getNode();
			PathSegment pathSegment = childNode.getSegment(branchUuid, type, segment);
			if (pathSegment != null) {
				path.addSegment(pathSegment);
				return childNode.resolvePath(branchUuid, type, path, pathStack);
			}
		}
		return path;

	}

	/**
	 * Generate the etag for nodes. The etag consists of:
	 * <ul>
	 * <li>uuid of the node</li>
	 * <li>parent node uuid (which is branch specific)</li>
	 * <li>version and language specific etag of the field container</li>
	 * <li>availableLanguages</li>
	 * <li>breadcrumb</li>
	 * <li>webroot path &amp; language paths</li>
	 * <li>permissions</li>
	 * </ul>
	 */
	@Override
	public String getSubETag(InternalActionContext ac) {
		Tx tx = GraphDBTx.getGraphTx();
		UserDao userDao = tx.userDao();
		TagDao tagDao = tx.tagDao();
		NodeDao nodeDao = tx.nodeDao();

		StringBuilder keyBuilder = new StringBuilder();

		// Parameters
		HibBranch branch = tx.getBranch(ac, getProject());
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		Node parentNode = getParentNode(branch.getUuid());
		HibNodeFieldContainer container = findVersion(ac.getNodeParameters().getLanguageList(options()), branch.getUuid(),
			ac.getVersioningParameters()
				.getVersion());

		/**
		 * branch uuid
		 */
		keyBuilder.append(branch.getUuid());
		keyBuilder.append("-");

		// TODO version, language list

		// We can omit further etag keys since this would return a 404 anyhow
		// since the requested container could not be found.
		if (container == null) {
			keyBuilder.append("404-no-container");
			return keyBuilder.toString();
		}

		/**
		 * Parent node
		 *
		 * The node can be moved and this would also affect the response. The etag must also be changed when the node is moved.
		 */
		if (parentNode != null) {
			keyBuilder.append("-");
			keyBuilder.append(parentNode.getUuid());
		}

		// fields version
		if (container != null) {
			keyBuilder.append("-");
			keyBuilder.append(container.getETag(ac));
		}

		/**
		 * Expansion (all)
		 *
		 * The expandAll parameter changes the json response and thus must be included in the etag computation.
		 */
		if (ac.getNodeParameters().getExpandAll()) {
			keyBuilder.append("-");
			keyBuilder.append("expand:true");
		}

		// expansion (selective)
		String expandedFields = Arrays.toString(ac.getNodeParameters().getExpandedFieldNames());
		keyBuilder.append("-");
		keyBuilder.append("expandFields:");
		keyBuilder.append(expandedFields);

		// branch specific tags
		for (HibTag tag : getTags(branch)) {
			// Tags can't be moved across branches thus we don't need to add the
			// tag family etag
			keyBuilder.append(tagDao.getETag(tag, ac));
		}

		// branch specific children
		for (HibNode child : getChildren(branch.getUuid())) {
			if (userDao.hasPermission(ac.getUser(), child, READ_PUBLISHED_PERM)) {
				keyBuilder.append("-");
				keyBuilder.append(child.getSchemaContainer().getName());
			}
		}

		// Publish state & availableLanguages
		for (HibNodeFieldContainer c : getFieldContainers(branch, PUBLISHED)) {
			keyBuilder.append(c.getLanguageTag() + "published");
		}
		for (HibNodeFieldContainer c : getFieldContainers(branch, DRAFT)) {
			keyBuilder.append(c.getLanguageTag() + "draft");
		}

		// breadcrumb
		keyBuilder.append("-");
		HibNode current = getParentNode(branch.getUuid());
		if (current != null) {
			while (current != null) {
				String key = current.getUuid() + toGraph(current).getDisplayName(ac);
				keyBuilder.append(key);
				if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
					WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
					String url = linkReplacer.resolve(ac, branch.getUuid(), type, current.getUuid(), ac.getNodeParameters().getResolveLinks(),
						getProject().getName(), container.getLanguageTag());
					keyBuilder.append(url);
				}
				current = nodeDao.getParentNode(current, branch.getUuid());

			}
		}

		/**
		 * webroot path & language paths
		 *
		 * The webroot and language paths must be included in the etag computation in order to invalidate the etag once a node language gets updated or once the
		 * display name of any parent node changes.
		 */
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {

			WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
			String path = linkReplacer.resolve(ac, branch.getUuid(), type, getUuid(), ac.getNodeParameters().getResolveLinks(), getProject()
				.getName(), container.getLanguageTag());
			keyBuilder.append(path);

			// languagePaths
			for (HibNodeFieldContainer currentFieldContainer : getFieldContainers(branch, forVersion(versioiningParameters.getVersion()))) {
				String currLanguage = currentFieldContainer.getLanguageTag();
				keyBuilder.append(currLanguage + "=" + linkReplacer.resolve(ac, branch.getUuid(), type, this, ac.getNodeParameters()
					.getResolveLinks(), currLanguage));
			}

		}

		if (log.isDebugEnabled()) {
			log.debug("Creating etag from key {" + keyBuilder.toString() + "}");
		}
		return keyBuilder.toString();
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public MeshElementEventModel onDeleted() {
		throw new NotImplementedException("Use dedicated onDeleted method for nodes instead.");
	}

	/**
	 * Create a new node moved event model.
	 * 
	 * @param branchUuid
	 * @param target
	 * @return
	 */
	public NodeMovedEventModel onNodeMoved(String branchUuid, HibNode target) {
		NodeMovedEventModel model = new NodeMovedEventModel();
		model.setEvent(NODE_MOVED);
		model.setBranchUuid(branchUuid);
		model.setProject(getProject().transformToReference());
		fillEventInfo(model);
		model.setTarget(target.transformToMinimalReference());
		return model;
	}

	@Override
	public MeshProjectElementEventModel createEvent(MeshEvent event) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		model.setProject(getProject().transformToReference());
		fillEventInfo(model);
		return model;
	}

	/**
	 * Create a new referenced element update event model.
	 * 
	 * @param uuid
	 *            Uuid of the referenced node
	 * @param schema
	 *            Schema of the referenced node
	 * @param branchUuid
	 *            Branch of the referenced node
	 * @param type
	 *            Type of the content that was updated (if known)
	 * @param languageTag
	 *            Language of the content that was updated (if known)
	 * @return
	 */
	public NodeMeshEventModel onReferenceUpdated(String uuid, HibSchema schema, String branchUuid, ContainerType type, String languageTag) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(NODE_REFERENCE_UPDATED);
		event.setUuid(uuid);
		event.setLanguageTag(languageTag);
		event.setType(type);
		event.setBranchUuid(branchUuid);
		event.setProject(getProject().transformToReference());
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	@Override
	public NodeMeshEventModel onDeleted(String uuid, HibSchema schema, String branchUuid, ContainerType type, String languageTag) {
		NodeMeshEventModel event = new NodeMeshEventModel();
		event.setEvent(getTypeInfo().getOnDeleted());
		event.setUuid(uuid);
		event.setLanguageTag(languageTag);
		event.setType(type);
		event.setBranchUuid(branchUuid);
		event.setProject(getProject().transformToReference());
		if (schema != null) {
			event.setSchema(schema.transformToReference());
		}
		return event;
	}

	@Override
	public NodeTaggedEventModel onTagged(HibTag tag, HibBranch branch, Assignment assignment) {
		NodeTaggedEventModel model = new NodeTaggedEventModel();
		model.setTag(tag.transformToReference());

		model.setBranch(branch.transformToReference());
		model.setProject(getProject().transformToReference());
		model.setNode(transformToMinimalReference());

		switch (assignment) {
		case ASSIGNED:
			model.setEvent(NODE_TAGGED);
			break;

		case UNASSIGNED:
			model.setEvent(NODE_UNTAGGED);
			break;
		}

		return model;
	}

	@Override
	public boolean isBaseNode() {
		return inE(HAS_ROOT_NODE).hasNext();
	}

	@Override
	public boolean isVisibleInBranch(String branchUuid) {
		return GraphFieldContainerEdgeImpl.matchesBranchAndType(getId(), branchUuid, ContainerType.DRAFT);
	}

	@Override
	public PermissionChangedProjectElementEventModel onPermissionChanged(HibRole role) {
		PermissionChangedProjectElementEventModel model = new PermissionChangedProjectElementEventModel();
		fillPermissionChanged(model, role);
		return model;
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
	public void generateBucketId() {
		BucketableElementHelper.generateBucketId(this);
	}
}
