package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_INDEX_NAME;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_KEY_PROPERTY;
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
import static com.gentics.mesh.madl.field.FieldType.LINK;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.tx.Tx;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagEdgeImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
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
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.version.NodeVersionsResponse;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.webroot.PathPrefixUtil;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NavigationParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.PublishParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.URIUtils;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see Node
 */
public class NodeImpl extends AbstractGenericFieldContainerVertex<NodeResponse, Node> implements Node {

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(vertexType(NodeImpl.class, MeshVertexImpl.class)
			.withField(SCHEMA_CONTAINER_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeImpl.class)
			.withField(SCHEMA_CONTAINER_KEY_PROPERTY, STRING));

		index.createIndex(edgeIndex(HAS_PARENT_NODE));

		index.createIndex(edgeIndex(HAS_PARENT_NODE)
			.withPostfix("branch_out")
			.withField("out", LINK)
			.withField(BRANCH_UUID_KEY, STRING));

		index.createIndex(edgeIndex(HAS_PARENT_NODE)
			.withPostfix("branch")
			.withField("in", LINK)
			.withField(BRANCH_UUID_KEY, STRING));

	}

	@Override
	public String getPathSegment(String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(branchUuid) == null) {
			return "";
		}

		// Find the first matching container and fallback to other listed languages
		NodeGraphFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getGraphFieldContainer(tag, branchUuid, type)) != null) {
				break;
			}
		}

		if (container == null && anyLanguage) {
			TraversalResult<? extends GraphFieldContainerEdgeImpl> traversal = getGraphFieldContainerEdges(branchUuid, type);

			if (traversal.hasNext()) {
				container = traversal.next().getNodeContainer();
			}
		}

		if (container != null) {
			return container.getSegmentFieldValue();
		}
		return null;
	}

	@Override
	public void postfixPathSegment(String branchUuid, ContainerType type, String languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(branchUuid) == null) {
			return;
		}

		// Find the first matching container and fallback to other listed languages
		NodeGraphFieldContainer container = getGraphFieldContainer(languageTag, branchUuid, type);
		if (container != null) {
			container.postfixSegmentFieldValue();
		}
	}

	@Override
	public String getPath(ActionContext ac, String branchUuid, ContainerType type, String... languageTag) {
		// We want to avoid rending the path again for nodes which we have already handled.
		// Thus utilise the action context data map to retrieve already handled paths.
		String cacheKey = getUuid() + branchUuid + type.getCode() + Arrays.toString(languageTag);
		return (String) ac.data().computeIfAbsent(cacheKey, key -> {

			List<String> segments = new ArrayList<>();
			String segment = getPathSegment(branchUuid, type, languageTag);
			if (segment == null) {
				return null;
			}
			segments.add(segment);

			// For the path segments of the container, we add all (additional)
			// project languages to the list of languages for the fallback.
			Node current = this;
			while (current != null) {
				current = current.getParentNode(branchUuid);
				if (current == null || current.getParentNode(branchUuid) == null) {
					break;
				}
				// For the path segments of the container, we allow ANY language (of the project)
				segment = current.getPathSegment(branchUuid, type, true, languageTag);

				// Abort early if one of the path segments could not be resolved. We
				// need to return a 404 in those cases.
				if (segment == null) {
					return null;
				}
				segments.add(segment);
			}

			Collections.reverse(segments);

			// Finally construct the path from all segments
			StringBuilder builder = new StringBuilder();

			// Append the prefix first
			Branch branch = getProject().getBranchRoot().findByUuid(branchUuid);
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

	@Override
	public void assertPublishConsistency(InternalActionContext ac, Branch branch) {

		String branchUuid = branch.getUuid();
		// Check whether the node got a published version and thus is published

		boolean isPublished = hasPublishedContent(branchUuid);

		// A published node must have also a published parent node.
		if (isPublished) {
			Node parentNode = getParentNode(branchUuid);

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

			for (Node node : getChildren(branchUuid)) {
				if (node.hasPublishedContent(branchUuid)) {
					log.error("Found published field container for node {" + node.getUuid() + "} in branch {" + branchUuid + "}. Node is child of {"
						+ getUuid() + "}");
					throw error(BAD_REQUEST, "node_error_children_containers_still_published", node.getUuid());
				}
			}
		}
	}

	@Override
	public TraversalResult<? extends Tag> getTags(Branch branch) {
		return new TraversalResult<>(TagEdgeImpl.getTagTraversal(this, branch).frameExplicit(TagImpl.class));
	}

	@Override
	public boolean hasTag(Tag tag, Branch branch) {
		return TagEdgeImpl.hasTag(this, tag, branch);
	}

	@Override
	public boolean hasPublishedContent(String branchUuid) {
		return GraphFieldContainerEdgeImpl.matchesBranchAndType(getId(), branchUuid, PUBLISHED);
	}

	@Override
	public TraversalResult<? extends NodeGraphFieldContainer> getGraphFieldContainers(String branchUuid, ContainerType type) {
		TraversalResult<? extends GraphFieldContainerEdgeImpl> it = GraphFieldContainerEdgeImpl.findEdges(this.getId(), branchUuid, type);
		Iterator<NodeGraphFieldContainer> it2 = it.stream().map(e -> e.getNodeContainer()).iterator();
		return new TraversalResult<>(it2);
	}

	@Override
	public TraversalResult<? extends NodeGraphFieldContainer> getGraphFieldContainers(ContainerType type) {
		return new TraversalResult<>(
			outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV()
				.frameExplicit(NodeGraphFieldContainerImpl.class));
	}

	@Override
	@SuppressWarnings("unchecked")
	public long getGraphFieldContainerCount() {
		return outE(HAS_FIELD_CONTAINER).or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, DRAFT.getCode()), e -> e.traversal()
			.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, PUBLISHED.getCode())).inV().count();
	}

	@Override
	public NodeGraphFieldContainer getLatestDraftFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestBranch(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag, Branch branch, ContainerType type) {
		return getGraphFieldContainer(languageTag, branch, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestBranch().getUuid(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag, String branchUuid, ContainerType type) {
		return getGraphFieldContainer(languageTag, branchUuid, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(String languageTag, Branch branch, User editor) {
		return createGraphFieldContainer(languageTag, branch, editor, null, true);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(String languageTag, Branch branch, User editor, NodeGraphFieldContainer original,
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
	protected TraversalResult<? extends GraphFieldContainerEdgeImpl> getGraphFieldContainerEdges(String branchUuid, ContainerType type) {
		return GraphFieldContainerEdgeImpl.findEdges(getId(), branchUuid, type);
	}

	@Override
	public void addTag(Tag tag, Branch branch) {
		removeTag(tag, branch);
		TagEdge edge = addFramedEdge(HAS_TAG, tag, TagEdgeImpl.class);
		edge.setBranchUuid(branch.getUuid());
	}

	@Override
	public void removeTag(Tag tag, Branch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).mark().inV().retain(tag).back().removeAll();
	}

	@Override
	public void removeAllTags(Branch branch) {
		outE(HAS_TAG).has(TagEdgeImpl.BRANCH_UUID_KEY, branch.getUuid()).removeAll();
	}

	@Override
	public void setSchemaContainer(SchemaContainer schema) {
		property(SCHEMA_CONTAINER_KEY_PROPERTY, schema.getUuid());
	}

	@Override
	public SchemaContainer getSchemaContainer() {
		return db().index().findByUuid(SchemaContainerImpl.class, property(SCHEMA_CONTAINER_KEY_PROPERTY));
	}

	@Override
	public TraversalResult<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE, NodeImpl.class);
	}

	@Override
	public TraversalResult<Node> getChildren(String branchUuid) {
		FramedGraph graph = Tx.get().getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_PARENT_NODE.toLowerCase() + "_branch", db().createComposedIndexKey(id(), branchUuid));
		Iterator<Edge> it = edges.iterator();
		Iterable<Edge> iterable = () -> it;
		Stream<Edge> stream = StreamSupport.stream(iterable.spliterator(), false);

		Stream<Node> nstream = stream.map(edge -> {
			Vertex vertex = edge.getVertex(OUT);
			return graph.frameElementExplicit(vertex, NodeImpl.class);
		});
		return new TraversalResult<>(() -> nstream.iterator());
	}

	@Override
	public Stream<Node> getChildrenStream(InternalActionContext ac) {
		FramedGraph graph = Tx.get().getGraph();
		MeshAuthUser user = ac.getUser();

		Iterable<Edge> edges = graph.getEdges("e." + HAS_PARENT_NODE.toLowerCase() + "_branch",
			db().createComposedIndexKey(id(), ac.getBranch().getUuid()));
		Iterator<Edge> it = edges.iterator();
		Iterable<Edge> iterable = () -> it;
		Stream<Edge> stream = StreamSupport.stream(iterable.spliterator(), false);
		return stream
			.map(edge -> edge.getVertex(OUT))
			.filter(vertex -> {
				Object id = vertex.getId();
				return user.hasPermissionForId(id, READ_PERM) || user.hasPermissionForId(id, READ_PUBLISHED_PERM);
			})
			.map(vertex -> graph.frameElementExplicit(vertex, NodeImpl.class));
	}

	@Override
	public Node getParentNode(String branchUuid) {
		FramedGraph graph = Tx.get().getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_PARENT_NODE.toLowerCase() + "_branch_out", db().createComposedIndexKey(id(), branchUuid));
		Iterator<Edge> it = edges.iterator();
		if (it.hasNext()) {
			Vertex in = it.next().getVertex(IN);
			return graph.frameElementExplicit(in, NodeImpl.class);
		} else {
			return null;
		}
	}

	@Override
	public void setParentNode(String branchUuid, Node parent) {
		outE(HAS_PARENT_NODE).has(BRANCH_UUID_KEY, branchUuid).removeAll();
		addFramedEdge(HAS_PARENT_NODE, parent).setProperty(BRANCH_UUID_KEY, branchUuid);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT, ProjectImpl.class).nextOrNull();
	}

	@Override
	public void setProject(Project project) {
		setLinkOut(project, ASSIGNED_TO_PROJECT);
	}

	@Override
	public Node create(User creator, SchemaContainerVersion schemaVersion, Project project) {
		return create(creator, schemaVersion, project, project.getLatestBranch());
	}

	/**
	 * Create a new node and make sure to delegate the creation request to the main node root aggregation node.
	 */
	@Override
	public Node create(User creator, SchemaContainerVersion schemaVersion, Project project, Branch branch, String uuid) {
		if (!isBaseNode() && !isVisibleInBranch(branch.getUuid())) {
			log.error(String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
				branch.getName(), getUuid()));
			throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
		}

		// We need to use the (meshRoot)--(nodeRoot) node instead of the
		// (project)--(nodeRoot) node.
		Node node = mesh().boot().nodeRoot().create(creator, schemaVersion, project, uuid);
		node.setParentNode(branch.getUuid(), this);
		node.setSchemaContainer(schemaVersion.getSchemaContainer());
		// setCreated(creator);
		return node;
	}

	private String getLanguageInfo(List<String> languageTags) {
		Iterator<String> it = languageTags.iterator();

		String langInfo = "[";
		while (it.hasNext()) {
			langInfo += it.next();
			if (it.hasNext()) {
				langInfo += ",";
			}
		}
		langInfo += "]";
		return langInfo;
	}

	@Override
	public NodeResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {

		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		// Increment level for each node transformation to avoid stackoverflow situations
		level = level + 1;
		NodeResponse restNode = new NodeResponse();
		if (fields.has("uuid")) {
			restNode.setUuid(getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return restNode;
			}
		}

		SchemaContainer container = getSchemaContainer();
		if (container == null) {
			throw error(BAD_REQUEST, "The schema container for node {" + getUuid() + "} could not be found.");
		}
		Branch branch = ac.getBranch(getProject());
		if (fields.has("languages")) {
			restNode.setAvailableLanguages(getLanguageInfo(ac));
		}

		setFields(ac, branch, restNode, level, fields, languageTags);

		if (fields.has("parent")) {
			setParentNodeInfo(ac, branch, restNode);
		}
		if (fields.has("perms")) {
			setRolePermissions(ac, restNode);
		}
		if (fields.has("children")) {
			setChildrenInfo(ac, branch, restNode);
		}
		if (fields.has("tags")) {
			setTagsToRest(ac, restNode, branch);
		}
		fillCommonRestFields(ac, fields, restNode);
		if (fields.has("breadcrumb")) {
			setBreadcrumbToRest(ac, restNode);
		}
		if (fields.has("path")) {
			setPathsToRest(ac, restNode, branch);
		}
		if (fields.has("project")) {
			setProjectReference(ac, restNode);
		}

		return restNode;
	}

	/**
	 * Set the project reference to the node response model.
	 *
	 * @param ac
	 * @param restNode
	 */
	private void setProjectReference(InternalActionContext ac, NodeResponse restNode) {
		restNode.setProject(getProject().transformToReference());
	}

	/**
	 * Set the parent node reference to the rest model.
	 *
	 * @param ac
	 * @param branch
	 *            Use the given branch to identify the branch specific parent node
	 * @param restNode
	 *            Model to be updated
	 * @return
	 */
	private void setParentNodeInfo(InternalActionContext ac, Branch branch, NodeResponse restNode) {
		Node parentNode = getParentNode(branch.getUuid());
		if (parentNode != null) {
			restNode.setParentNode(parentNode.transformToReference(ac));
		} else {
			// Only the base node of the project has no parent. Therefore this
			// node must be a container.
			restNode.setContainer(true);
		}
	}

	/**
	 * Set the node fields to the given rest model.
	 *
	 * @param ac
	 * @param branch
	 *            Branch which will be used to locate the correct field container
	 * @param restNode
	 *            Rest model which will be updated
	 * @param level
	 *            Current level of transformation
	 * @param fieldsSet
	 * @param languageTags
	 * @return
	 */
	private void setFields(InternalActionContext ac, Branch branch, NodeResponse restNode, int level, FieldsSet fieldsSet, String... languageTags) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		NodeParameters nodeParameters = ac.getNodeParameters();

		List<String> requestedLanguageTags = null;
		if (languageTags != null && languageTags.length > 0) {
			requestedLanguageTags = Arrays.asList(languageTags);
		} else {
			requestedLanguageTags = nodeParameters.getLanguageList(options());
		}

		// First check whether the NGFC for the requested language,branch and version could be found.
		NodeGraphFieldContainer fieldContainer = findVersion(requestedLanguageTags, branch.getUuid(), versioiningParameters.getVersion());
		if (fieldContainer == null) {
			// If a published version was requested, we check whether any
			// published language variant exists for the node, if not, response
			// with NOT_FOUND
			if (forVersion(versioiningParameters.getVersion()) == PUBLISHED && !getGraphFieldContainers(branch, PUBLISHED).iterator().hasNext()) {
				log.error("Could not find field container for languages {" + requestedLanguageTags + "} and branch {" + branch.getUuid()
					+ "} and version params version {" + versioiningParameters.getVersion() + "}, branch {" + branch.getUuid() + "}");
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_branch_version", getUuid(), branch.getUuid());
			}

			// If a specific version was requested, that does not exist, we also
			// return NOT_FOUND
			if (forVersion(versioiningParameters.getVersion()) == INITIAL) {
				throw error(NOT_FOUND, "object_not_found_for_version", versioiningParameters.getVersion());
			}

			String langInfo = getLanguageInfo(requestedLanguageTags);
			if (log.isDebugEnabled()) {
				log.debug("The fields for node {" + getUuid() + "} can't be populated since the node has no matching language for the languages {"
					+ langInfo + "}. Fields will be empty.");
			}
			// No field container was found so we can only set the schema
			// reference that points to the container (no version information
			// will be included)
			if (fieldsSet.has("schema")) {
				restNode.setSchema(getSchemaContainer().transformToReference());
			}
			// TODO BUG Issue #119 - Actually we would need to throw a 404 in these cases but many current implementations rely on the empty node response.
			// The response will also contain information about other languages and general structure information.
			// We should change this behaviour and update the client implementations.
			// throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
		} else {
			Schema schema = fieldContainer.getSchemaContainerVersion().getSchema();
			if (fieldsSet.has("container")) {
				restNode.setContainer(schema.getContainer());
			}
			if (fieldsSet.has("displayField")) {
				restNode.setDisplayField(schema.getDisplayField());
			}
			if (fieldsSet.has("displayName")) {
				restNode.setDisplayName(getDisplayName(ac));
			}

			if (fieldsSet.has("language")) {
				restNode.setLanguage(fieldContainer.getLanguageTag());
			}
			// List<String> fieldsToExpand = ac.getExpandedFieldnames();
			// modify the language fallback list by moving the container's
			// language to the front
			List<String> containerLanguageTags = new ArrayList<>(requestedLanguageTags);
			containerLanguageTags.remove(restNode.getLanguage());
			containerLanguageTags.add(0, restNode.getLanguage());

			// Schema reference
			if (fieldsSet.has("schema")) {
				restNode.setSchema(fieldContainer.getSchemaContainerVersion().transformToReference());
			}

			// Version reference
			if (fieldsSet.has("version") && fieldContainer.getVersion() != null) {
				restNode.setVersion(fieldContainer.getVersion().toString());
			}

			// editor and edited
			if (fieldsSet.has("editor")) {
				User editor = fieldContainer.getEditor();
				if (editor != null) {
					restNode.setEditor(editor.transformToReference());
				}
			}
			if (fieldsSet.has("edited")) {
				restNode.setEdited(fieldContainer.getLastEditedDate());
			}

			if (fieldsSet.has("fields")) {
				// Iterate over all fields and transform them to rest
				com.gentics.mesh.core.rest.node.FieldMap fields = new FieldMapImpl();
				for (FieldSchema fieldEntry : schema.getFields()) {
					// boolean expandField =
					// fieldsToExpand.contains(fieldEntry.getName()) ||
					// ac.getExpandAllFlag();
					Field restField = fieldContainer.getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, containerLanguageTags, level);
					if (fieldEntry.isRequired() && restField == null) {
						// TODO i18n
						// throw error(BAD_REQUEST, "The field {" +
						// fieldEntry.getName()
						// + "} is a required field but it could not be found in the
						// node. Please add the field using an update call or change
						// the field schema and
						// remove the required flag.");
						fields.put(fieldEntry.getName(), null);
					}
					if (restField == null) {
						if (log.isDebugEnabled()) {
							log.debug("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
						}
					} else {
						fields.put(fieldEntry.getName(), restField);
					}

				}
				restNode.setFields(fields);
			}
		}
	}

	/**
	 * Set the children info to the rest model.
	 *
	 * @param ac
	 * @param branch
	 *            Branch which will be used to identify the branch specific child nodes
	 * @param restNode
	 *            Rest model which will be updated
	 */
	private void setChildrenInfo(InternalActionContext ac, Branch branch, NodeResponse restNode) {
		Map<String, NodeChildrenInfo> childrenInfo = new HashMap<>();
		for (Node child : getChildren(branch.getUuid())) {
			if (ac.getUser().hasPermission(child, READ_PERM)) {
				String schemaName = child.getSchemaContainer().getName();
				NodeChildrenInfo info = childrenInfo.get(schemaName);
				if (info == null) {
					info = new NodeChildrenInfo();
					String schemaUuid = child.getSchemaContainer().getUuid();
					info.setSchemaUuid(schemaUuid);
					info.setCount(1);
					childrenInfo.put(schemaName, info);
				} else {
					info.setCount(info.getCount() + 1);
				}
			}
		}
		restNode.setChildrenInfo(childrenInfo);
	}

	/**
	 * Set the tag information to the rest model.
	 *
	 * @param ac
	 * @param restNode
	 *            Rest model which will be updated
	 * @param branch
	 *            Branch which will be used to identify the branch specific tags
	 * @return
	 */
	private void setTagsToRest(InternalActionContext ac, NodeResponse restNode, Branch branch) {
		List<TagReference> list = getTags(branch).stream()
			.map(Tag::transformToReference)
			.collect(Collectors.toList());
		restNode.setTags(list);
	}

	/**
	 * Add the branch specific webroot and language paths to the given rest node.
	 *
	 * @param ac
	 * @param restNode
	 *            Rest model which will be updated
	 * @param branch
	 *            Branch which will be used to identify the nodes relations and thus the correct path can be determined
	 * @return
	 */
	private void setPathsToRest(InternalActionContext ac, NodeResponse restNode, Branch branch) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			String branchUuid = ac.getBranch(getProject()).getUuid();
			ContainerType type = forVersion(versioiningParameters.getVersion());

			LinkType linkType = ac.getNodeParameters().getResolveLinks();

			// Path
			WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
			String path = linkReplacer.resolve(ac, branchUuid, type, getUuid(), linkType, getProject().getName(), restNode.getLanguage());
			restNode.setPath(path);

			// languagePaths
			restNode.setLanguagePaths(getLanguagePaths(ac, linkType, branch));
		}
	}

	@Override
	public Map<String, String> getLanguagePaths(InternalActionContext ac, LinkType linkType, Branch branch) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		String branchUuid = ac.getBranch(getProject()).getUuid();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		Map<String, String> languagePaths = new HashMap<>();
		WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
		for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(branch, forVersion(versioiningParameters.getVersion()))) {
			String currLanguage = currentFieldContainer.getLanguageTag();
			String languagePath = linkReplacer.resolve(ac, branchUuid, type, this, linkType, currLanguage);
			languagePaths.put(currLanguage, languagePath);
		}
		return languagePaths;
	}

	/**
	 * Set the breadcrumb information to the given rest node.
	 *
	 * @param ac
	 * @param restNode
	 */
	private void setBreadcrumbToRest(InternalActionContext ac, NodeResponse restNode) {
		List<NodeReference> breadcrumbs = getBreadcrumbNodeStream(ac)
			.map(node -> node.transformToReference(ac))
			.collect(Collectors.toList());
		restNode.setBreadcrumb(breadcrumbs);
	}

	@Override
	public TraversalResult<Node> getBreadcrumbNodes(InternalActionContext ac) {
		return new TraversalResult<>(() -> getBreadcrumbNodeStream(ac).iterator());
	}

	private Stream<Node> getBreadcrumbNodeStream(InternalActionContext ac) {
		String branchUuid = ac.getBranch(getProject()).getUuid();
		Node current = this;

		Deque<Node> breadcrumb = new ArrayDeque<>();
		while (current != null) {
			breadcrumb.addFirst(current);
			current = current.getParentNode(branchUuid);
		}

		return breadcrumb.stream();
	}

	@Override
	public Single<NavigationResponse> transformToNavigation(InternalActionContext ac) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		if (parameters.getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		return db().asyncTx(() -> {
			// TODO assure that the schema version is correct
			if (!getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				throw error(BAD_REQUEST, "navigation_error_no_container");
			}
			String etagKey = buildNavigationEtagKey(ac, this, parameters.getMaxDepth(), 0, ac.getBranch(getProject()).getUuid(), forVersion(ac
				.getVersioningParameters().getVersion()));
			String etag = ETag.hash(etagKey);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				return Single.error(new NotModifiedException());
			} else {
				NavigationResponse response = new NavigationResponse();
				return buildNavigationResponse(ac, this, parameters.getMaxDepth(), 0, response, response, ac.getBranch(getProject()).getUuid(),
					forVersion(ac.getVersioningParameters().getVersion()));
			}
		});
	}

	@Override
	public NodeVersionsResponse transformToVersionList(InternalActionContext ac) {
		NodeVersionsResponse response = new NodeVersionsResponse();
		Map<String, List<VersionInfo>> versions = new HashMap<>();
		getGraphFieldContainers(ac.getBranch(), DRAFT).forEach(c -> {
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
	private String buildNavigationEtagKey(InternalActionContext ac, Node node, int maxDepth, int level, String branchUuid, ContainerType type) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		StringBuilder builder = new StringBuilder();
		builder.append(node.getETag(ac));

		TraversalResult<? extends Node> nodes = node.getChildren(ac.getUser(), branchUuid, null, type);

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || !nodes.iterator().hasNext()) {
			return builder.toString();
		}
		for (Node child : nodes) {
			if (child.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				builder.append(buildNavigationEtagKey(ac, child, maxDepth, level + 1, branchUuid, type));
			} else if (parameters.isIncludeAll()) {
				builder.append(buildNavigationEtagKey(ac, child, maxDepth, level, branchUuid, type));
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
	private Single<NavigationResponse> buildNavigationResponse(InternalActionContext ac, Node node, int maxDepth, int level,
		NavigationResponse navigation, NavigationElement currentElement, String branchUuid, ContainerType type) {
		TraversalResult<? extends Node> nodes = node.getChildren(ac.getUser(), branchUuid, null, type);
		List<Single<NavigationResponse>> obsResponses = new ArrayList<>();

		obsResponses.add(node.transformToRest(ac, 0).map(response -> {
			// Set current element data
			currentElement.setUuid(response.getUuid());
			currentElement.setNode(response);
			return navigation;
		}));

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || !nodes.iterator().hasNext()) {
			List<Observable<NavigationResponse>> obsList = obsResponses.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
			return Observable.merge(obsList).lastOrError();
		}
		NavigationParameters parameters = new NavigationParametersImpl(ac);
		// Add children
		for (Node child : nodes) {
			// TODO assure that the schema version is correct?
			// TODO also allow navigations over containers
			if (child.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
				NavigationElement childElement = new NavigationElement();
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				currentElement.getChildren().add(childElement);
				obsResponses.add(buildNavigationResponse(ac, child, maxDepth, level + 1, navigation, childElement, branchUuid, type));
			} else if (parameters.isIncludeAll()) {
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				NavigationElement childElement = new NavigationElement();
				currentElement.getChildren().add(childElement);
				obsResponses.add(buildNavigationResponse(ac, child, maxDepth, level, navigation, childElement, branchUuid, type));
			}
		}
		List<Observable<NavigationResponse>> obsList = obsResponses.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
		return Observable.merge(obsList).lastOrError();
	}

	@Override
	public NodeReference transformToReference(InternalActionContext ac) {
		Branch branch = ac.getBranch(getProject());

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

	@Override
	public NodeFieldListItem toListItem(InternalActionContext ac, String[] languageTags) {
		// Create the rest field and populate the fields
		NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(getUuid());
		String branchUuid = ac.getBranch(getProject()).getUuid();
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
		Map<String, PublishStatusModel> languages = getLanguageInfo(ac);
		publishStatus.setAvailableLanguages(languages);
		return publishStatus;
	}

	private Map<String, PublishStatusModel> getLanguageInfo(InternalActionContext ac) {
		Map<String, PublishStatusModel> languages = new HashMap<>();
		Branch branch = ac.getBranch(getProject());

		getGraphFieldContainers(branch, PUBLISHED).stream().forEach(c -> {

			String date = DateUtils.toISO8601(c.getLastEditedTimestamp(), 0);

			PublishStatusModel status = new PublishStatusModel();
			status.setPublished(true);
			status.setVersion(c.getVersion().toString());
			User editor = c.getEditor();
			if (editor != null) {
				status.setPublisher(editor.transformToReference());
			}
			status.setPublishDate(date);
			languages.put(c.getLanguageTag(), status);
		});

		getGraphFieldContainers(branch, DRAFT).stream().filter(c -> !languages.containsKey(c.getLanguageTag())).forEach(c -> {
			PublishStatusModel status = new PublishStatusModel().setPublished(false).setVersion(c.getVersion().toString());
			languages.put(c.getLanguageTag(), status);
		});
		return languages;
	}

	@Override
	public void publish(InternalActionContext ac, Branch branch, BulkActionContext bac) {
		PublishParameters parameters = ac.getPublishParameters();

		bac.batch().add(onUpdated());

		// Handle recursion
		if (parameters.isRecursive()) {
			for (Node child : getChildren(branch.getUuid())) {
				child.publish(ac, branch, bac);
			}
		}
		assertPublishConsistency(ac, branch);
	}

	@Override
	public void publish(InternalActionContext ac, BulkActionContext bac) {

		Branch branch = ac.getBranch(getProject());
		String branchUuid = branch.getUuid();

		List<? extends NodeGraphFieldContainer> unpublishedContainers = getGraphFieldContainers(branch, ContainerType.DRAFT).stream().filter(c -> !c
			.isPublished(branchUuid)).collect(Collectors.toList());

		// publish all unpublished containers and handle recursion
		unpublishedContainers.stream().forEach(c -> {
			NodeGraphFieldContainer newVersion = publish(ac, c.getLanguageTag(), branch, ac.getUser());
			bac.add(newVersion.onPublish(branchUuid));
		});
		assertPublishConsistency(ac, branch);

		// Handle recursion after publishing the current node.
		// This is done to ensure the publish consistency.
		// Even if the publishing process stops at the initial
		// level the consistency is correct.
		PublishParameters parameters = ac.getPublishParameters();
		if (parameters.isRecursive()) {
			for (Node node : getChildren(branchUuid)) {
				node.publish(ac, bac);
			}
		}
		bac.process();
	}

	@Override
	public void takeOffline(InternalActionContext ac, BulkActionContext bac, Branch branch, PublishParameters parameters) {

		// Handle recursion first to start at the leafs
		if (parameters.isRecursive()) {
			for (Node node : getChildren(branch.getUuid())) {
				node.takeOffline(ac, bac, branch, parameters);
			}
		}

		String branchUuid = branch.getUuid();

		TraversalResult<? extends GraphFieldContainerEdgeImpl> publishEdges = getGraphFieldContainerEdges(branchUuid, PUBLISHED);

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
		Branch branch = ac.getBranch(getProject());
		PublishParameters parameters = ac.getPublishParameters();
		takeOffline(ac, bac, branch, parameters);
	}

	@Override
	public PublishStatusModel transformToPublishStatus(InternalActionContext ac, String languageTag) {
		Branch branch = ac.getBranch(getProject());

		NodeGraphFieldContainer container = getGraphFieldContainer(languageTag, branch.getUuid(), PUBLISHED);
		if (container != null) {
			String date = container.getLastEditedDate();
			PublishStatusModel status = new PublishStatusModel();
			status.setPublished(true);
			status.setVersion(container.getVersion().toString());
			User editor = container.getEditor();
			if (editor != null) {
				status.setPublisher(editor.transformToReference());
			}
			status.setPublishDate(date);
			return status;
		} else {
			container = getGraphFieldContainer(languageTag, branch.getUuid(), DRAFT);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			return new PublishStatusModel().setPublished(false).setVersion(container.getVersion().toString());
		}
	}

	@Override
	public void publish(InternalActionContext ac, BulkActionContext bac, String languageTag) {
		Branch branch = ac.getBranch(getProject());
		String branchUuid = branch.getUuid();

		// get the draft version of the given language
		NodeGraphFieldContainer draftVersion = getGraphFieldContainer(languageTag, branchUuid, DRAFT);

		// if not existent -> NOT_FOUND
		if (draftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		// If the located draft version was already published we are done
		if (draftVersion.isPublished(branchUuid)) {
			return;
		}

		// TODO check whether all required fields are filled, if not -> unable to publish
		NodeGraphFieldContainer publishedContainer = publish(ac, draftVersion.getLanguageTag(), branch, ac.getUser());
		// Invoke a store of the document since it must now also be added to the published index
		bac.add(publishedContainer.onPublish(branchUuid));
	}

	@Override
	public void takeOffline(InternalActionContext ac, BulkActionContext bac, Branch branch, String languageTag) {
		String branchUuid = branch.getUuid();

		// Locate the published container
		NodeGraphFieldContainer published = getGraphFieldContainer(languageTag, branchUuid, PUBLISHED);
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
	public void setPublished(InternalActionContext ac, NodeGraphFieldContainer container, String branchUuid) {
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
			NodeGraphFieldContainer prev = container.getPreviousVersion();
			if (isAutoPurgeEnabled && prev != null && prev.isPurgeable()) {
				prev.purge();
			}
		}

		// create new published edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setBranchUuid(branchUuid);
		edge.setType(PUBLISHED);
		container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_publish");
	}

	@Override
	public NodeGraphFieldContainer publish(InternalActionContext ac, String languageTag, Branch branch, User user) {
		String branchUuid = branch.getUuid();

		// create published version
		NodeGraphFieldContainer newVersion = createGraphFieldContainer(languageTag, branch, user);
		newVersion.setVersion(newVersion.getVersion().nextPublished());

		setPublished(ac, newVersion, branchUuid);
		return newVersion;
	}

	@Override
	public NodeGraphFieldContainer findVersion(List<String> languageTags, String branchUuid, String version) {
		NodeGraphFieldContainer fieldContainer = null;

		// TODO refactor the type handling and don't return INITIAL.
		ContainerType type = forVersion(version);

		for (String languageTag : languageTags) {

			// Don't start the version lookup using the initial version. Instead start at the end of the chain and use the DRAFT version instead.
			fieldContainer = getGraphFieldContainer(languageTag, branchUuid, type == INITIAL ? DRAFT : type);

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
		for (GraphFieldContainer container : getDraftGraphFieldContainers()) {
			languageTags.add(container.getLanguageTag());
		}
		return languageTags;
	}

	@Override
	public List<String> getAvailableLanguageNames(Branch branch, ContainerType type) {
		List<String> languageTags = new ArrayList<>();
		for (GraphFieldContainer container : getGraphFieldContainers(branch, type)) {
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
			for (Node child : getChildren()) {
				child.delete(bac);
				bac.process();
			}
		}

		// Delete all initial containers (which will delete all containers)
		for (NodeGraphFieldContainer container : getGraphFieldContainers(INITIAL)) {
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
	public Stream<? extends NodeGraphField> getInboundReferences() {
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
			.flatMap(NodeGraphField::getReferencingContents)
			.forEach(nodeContainer -> {
				for (GraphFieldContainerEdgeImpl edge : nodeContainer.inE(HAS_FIELD_CONTAINER, GraphFieldContainerEdgeImpl.class)) {
					ContainerType type = edge.getType();
					// Only handle published or draft contents
					if (type.equals(DRAFT) || type.equals(PUBLISHED)) {
						Node node = nodeContainer.getParentNode();
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
	public void deleteFromBranch(InternalActionContext ac, Branch branch, BulkActionContext bac, boolean ignoreChecks) {

		DeleteParameters parameters = ac.getDeleteParameters();

		// 1. Remove subfolders from branch
		String branchUuid = branch.getUuid();

		for (Node child : getChildren(branchUuid)) {
			if (!parameters.isRecursive()) {
				throw error(BAD_REQUEST, "node_error_delete_failed_node_has_children");
			}
			child.deleteFromBranch(ac, branch, bac, ignoreChecks);
		}

		// 2. Delete all language containers
		for (NodeGraphFieldContainer container : getGraphFieldContainers(branch, DRAFT)) {
			deleteLanguageContainer(ac, branch, container.getLanguageTag(), bac, false);
		}

		// 3. Now check if the node has no more field containers in any branch. We can delete it in those cases
		if (getGraphFieldContainerCount() == 0) {
			delete(bac);
		} else {
			// Otherwise we need to remove the "parent" edge for the branch
			// first remove the "parent" edge (because the node itself will
			// probably not be deleted, but just removed from the branch)
			outE(HAS_PARENT_NODE).has(BRANCH_UUID_KEY, branchUuid).removeAll();
		}
	}

	/**
	 * Get a vertex traversal to find the children of this node, this user has read permission for.
	 *
	 * @param requestUser
	 *            user
	 * @param branchUuid
	 *            branch uuid
	 * @param languageTags
	 *            Only list nodes which match the given language tags. Don't filter if the language tags list is null
	 * @param type
	 *            edge type
	 * @return vertex traversal
	 */
	private VertexTraversal<?, ?, ?> getChildrenTraversal(MeshAuthUser requestUser, String branchUuid, List<String> languageTags,
		ContainerType type) {
		GraphPermission permissionToCheck = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;

		VertexTraversal<?, ?, ?> traversal = null;
		if (branchUuid != null) {
			traversal = inE(HAS_PARENT_NODE).has(BRANCH_UUID_KEY, branchUuid).outV();
		} else {
			traversal = in(HAS_PARENT_NODE);
		}
		traversal = traversal.filter(v -> requestUser.hasPermissionForId(v.getId(), permissionToCheck));
		if (branchUuid != null || type != null) {
			EdgeTraversal<?, ?, ?> edgeTraversal = traversal.mark().outE(HAS_FIELD_CONTAINER);
			if (branchUuid != null) {
				edgeTraversal = edgeTraversal.has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid);
			}
			if (type != null) {
				edgeTraversal = edgeTraversal.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
			}

			// Filter out nodes which are not listed in the given language tags
			if (languageTags != null) {
				edgeTraversal = edgeTraversal.filter(edge -> {
					String languageTag = edge.getProperty(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY);
					return languageTags.contains(languageTag);
				});
			}
			traversal = (VertexTraversal<?, ?, ?>) edgeTraversal.outV().back();
		}
		return traversal;
	}

	@Override
	public TraversalResult<? extends Node> getChildren(MeshAuthUser requestUser, String branchUuid, List<String> languageTags, ContainerType type) {
		return new TraversalResult<>(getChildrenTraversal(requestUser, branchUuid, languageTags, type).frameExplicit(NodeImpl.class));
	}

	@Override
	public TransformablePage<? extends Node> getChildren(InternalActionContext ac, List<String> languageTags, String branchUuid, ContainerType type,
		PagingParameters pagingInfo) {
		String indexName = "e." + HAS_PARENT_NODE.toLowerCase() + "_branch";
		Object indexKey = db().createComposedIndexKey(id(), branchUuid);

		GraphPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		if (languageTags == null) {
			return new DynamicTransformablePageImpl<>(ac.getUser(), indexName, indexKey, Direction.OUT, NodeImpl.class, pagingInfo, perm, null, true);
		} else {
			return new DynamicTransformablePageImpl<>(ac.getUser(), indexName, indexKey, Direction.OUT, NodeImpl.class, pagingInfo, perm, (item) -> {
				// Filter out nodes which do not provide one of the specified language tags and type
				for (String languageTag : languageTags) {
					if (item.getGraphFieldContainerEdge(languageTag, branchUuid, type) != null) {
						return true;
					}
				}
				return false;
			}, true);
		}
	}

	@Override
	public TransformablePage<? extends Tag> getTags(User user, PagingParameters params, Branch branch) {
		VertexTraversal<?, ?, ?> traversal = TagEdgeImpl.getTagTraversal(this, branch);
		return new DynamicTransformablePageImpl<Tag>(user, traversal, params, READ_PERM, TagImpl.class);
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			// We don't need to filter by branch. Branch nodes can't have dedicated perms
			for (Node child : getChildren()) {
				child.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public String getDisplayName(InternalActionContext ac) {
		NodeParameters nodeParameters = ac.getNodeParameters();
		VersioningParameters versioningParameters = ac.getVersioningParameters();

		NodeGraphFieldContainer container = findVersion(nodeParameters.getLanguageList(options()), ac.getBranch(getProject()).getUuid(), versioningParameters
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

		Language language = mesh().boot().languageRoot().findByLanguageTag(languageTag);
		if (language == null) {
			throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
		}
		Branch branch = ac.getBranch(getProject());
		NodeGraphFieldContainer latestDraftVersion = getGraphFieldContainer(languageTag, branch, DRAFT);

		// Check whether this is the first time that an update for the given language and branch occurs. In this case a new container must be created.
		// This means that no conflict check can be performed. Conflict checks only occur for updates on existing contents.
		if (latestDraftVersion == null) {
			// Create a new field container
			latestDraftVersion = createGraphFieldContainer(languageTag, branch, ac.getUser());

			// Check whether the node has a parent node in this branch, if not, the request is supposed to be a create request
			// and we get the parent node from this create request
			if (getParentNode(branch.getUuid()) == null) {
				NodeCreateRequest createRequest = JsonUtil.readValue(ac.getBodyAsString(), NodeCreateRequest.class);
				if (createRequest.getParentNode() == null || isEmpty(createRequest.getParentNode().getUuid())) {
					throw error(BAD_REQUEST, "node_missing_parentnode_field");
				}
				Node parentNode = getProject().getNodeRoot().loadObjectByUuid(ac, createRequest.getParentNode().getUuid(), CREATE_PERM);
				// check whether the parent node is visible in the branch
				if (!parentNode.isBaseNode() && !parentNode.isVisibleInBranch(branch.getUuid())) {
					log.error(
						String.format("Error while creating node in branch {%s}: requested parent node {%s} exists, but is not visible in branch.",
							branch.getName(), parentNode.getUuid()));
					throw error(NOT_FOUND, "object_not_found_for_uuid", createRequest.getParentNode().getUuid());
				}
				setParentNode(branch.getUuid(), parentNode);
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
			SchemaContainerVersion schemaContainerVersion = latestDraftVersion.getSchemaContainerVersion();
			if (!latestDraftVersion.getSchemaContainerVersion().equals(branch.findLatestSchemaVersion(schemaContainerVersion
				.getSchemaContainer()))) {
				throw error(BAD_REQUEST, "node_error_migration_incomplete");
			}

			// Load the base version field container in order to create the diff
			NodeGraphFieldContainer baseVersionContainer = findVersion(requestModel.getLanguage(), branch.getUuid(), version);
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
				NodeGraphFieldContainer newDraftVersion = createGraphFieldContainer(language.getLanguageTag(), branch, ac.getUser(),
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
	public TransformablePage<? extends Tag> updateTags(InternalActionContext ac, EventQueueBatch batch) {
		List<Tag> tags = getTagsToSet(ac, batch);
		Branch branch = ac.getBranch();
		applyTags(branch, tags, batch);
		User user = ac.getUser();
		return getTags(user, ac.getPagingParameters(), branch);
	}

	@Override
	public void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> list) {
		List<Tag> tags = getTagsToSet(list, ac, batch);
		Branch branch = ac.getBranch();
		applyTags(branch, tags, batch);
	}

	private void applyTags(Branch branch, List<? extends Tag> tags, EventQueueBatch batch) {
		List<? extends Tag> currentTags = getTags(branch).list();

		List<Tag> toBeAdded = tags.stream()
			.filter(StreamUtil.not(new HashSet<>(currentTags)::contains))
			.collect(Collectors.toList());
		toBeAdded.forEach(tag -> {
			addTag(tag, branch);
			batch.add(onTagged(tag, branch, ASSIGNED));
		});

		List<Tag> toBeRemoved = currentTags.stream()
			.filter(StreamUtil.not(new HashSet<>(tags)::contains))
			.collect(Collectors.toList());
		toBeRemoved.forEach(tag -> {
			removeTag(tag, branch);
			batch.add(onTagged(tag, branch, UNASSIGNED));
		});
	}

	@Override
	public void moveTo(InternalActionContext ac, Node targetNode, EventQueueBatch batch) {
		// TODO should we add a guard that terminates this loop when it runs to
		// long?

		// Check whether the target node is part of the subtree of the source
		// node.
		// We must detect and prevent such actions because those would
		// invalidate the tree structure
		Branch branch = ac.getBranch(getProject());
		String branchUuid = branch.getUuid();
		Node parent = targetNode.getParentNode(branchUuid);
		while (parent != null) {
			if (parent.getUuid().equals(getUuid())) {
				throw error(BAD_REQUEST, "node_move_error_not_allowed_to_move_node_into_one_of_its_children");
			}
			parent = parent.getParentNode(branchUuid);
		}

		if (!targetNode.getSchemaContainer().getLatestVersion().getSchema().getContainer()) {
			throw error(BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		}

		if (getUuid().equals(targetNode.getUuid())) {
			throw error(BAD_REQUEST, "node_move_error_same_nodes");
		}

		setParentNode(branchUuid, targetNode);

		// Update published graph field containers
		getGraphFieldContainers(branchUuid, PUBLISHED).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});

		// Update draft graph field containers
		getGraphFieldContainers(branchUuid, DRAFT).stream().forEach(container -> {
			container.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_move");
		});
		batch.add(onNodeMoved(branchUuid, targetNode));
		assertPublishConsistency(ac, branch);
	}

	@Override
	public void deleteLanguageContainer(InternalActionContext ac, Branch branch, String languageTag, BulkActionContext bac,
		boolean failForLastContainer) {

		// 1. Check whether the container has also a published variant. We need to take it offline in those cases
		NodeGraphFieldContainer container = getGraphFieldContainer(languageTag, branch, PUBLISHED);
		if (container != null) {
			takeOffline(ac, bac, branch, languageTag);
		}

		// 2. Load the draft container and remove it from the branch
		container = getGraphFieldContainer(languageTag, branch, DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", languageTag);
		}
		container.deleteFromBranch(branch, bac);
		// No need to delete the published variant because if the container was published the take offline call handled it

		// starting with the old draft, delete all GFC that have no next and are not draft (for other branches)
		NodeGraphFieldContainer dangling = container;
		while (dangling != null && !dangling.isDraft() && !dangling.hasNextVersion()) {
			NodeGraphFieldContainer toDelete = dangling;
			dangling = toDelete.getPreviousVersion();
			toDelete.delete(bac);
		}

		NodeGraphFieldContainer initial = getGraphFieldContainer(languageTag, branch, INITIAL);
		if (initial != null) {
			// Remove the initial edge
			initial.inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid())
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).removeAll();

			// starting with the old initial, delete all GFC that have no previous and are not initial (for other branches)
			dangling = initial;
			while (dangling != null && !dangling.isInitial() && !dangling.hasPreviousVersion()) {
				NodeGraphFieldContainer toDelete = dangling;
				// since the GFC "toDelete" was only used by this branch, it can not have more than one "next" GFC
				// (multiple "next" would have to belong to different branches, and for every branch, there would have to be
				// an INITIAL, which would have to be either this GFC or a previous)
				dangling = toDelete.getNextVersions().iterator().next();
				toDelete.delete(bac, false);
			}
		}

		// 3. Check whether this was be the last container of the node for this branch
		DeleteParameters parameters = ac.getDeleteParameters();
		if (failForLastContainer) {
			TraversalResult<? extends NodeGraphFieldContainer> draftContainers = getGraphFieldContainers(branch.getUuid(), DRAFT);
			TraversalResult<? extends NodeGraphFieldContainer> publishContainers = getGraphFieldContainers(branch.getUuid(), PUBLISHED);
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

	@Override
	public PathSegment getSegment(String branchUuid, ContainerType type, String segment) {

		// Check the different language versions
		for (NodeGraphFieldContainer container : getGraphFieldContainers(branchUuid, type)) {
			Schema schema = container.getSchemaContainerVersion().getSchema();
			String segmentFieldName = schema.getSegmentField();
			// First check whether a string field exists for the given name
			StringGraphField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegment(container, field, container.getLanguageTag(), segment);
				}
			}

			// No luck yet - lets check whether a binary field matches the
			// segmentField
			BinaryGraphField binaryField = container.getBinary(segmentFieldName);
			if (binaryField == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
						+ "}");
				}
			} else {
				String binaryFilename = binaryField.getFileName();
				if (segment.equals(binaryFilename)) {
					return new PathSegment(container, binaryField, container.getLanguageTag(), segment);
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

		FramedGraph graph = Tx.get().getGraph();
		String segmentInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(this, segment);
		Object key = GraphFieldContainerEdgeImpl.composeWebrootIndexKey(db(), segmentInfo, branchUuid, type);
		Iterator<? extends GraphFieldContainerEdge> edges = graph.getFramedEdges(WEBROOT_INDEX_NAME, key, GraphFieldContainerEdgeImpl.class)
			.iterator();
		if (edges.hasNext()) {
			GraphFieldContainerEdge edge = edges.next();
			Node childNode = edge.getNode();
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
		StringBuilder keyBuilder = new StringBuilder();

		// Parameters
		Branch branch = ac.getBranch(getProject());
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		Node parentNode = getParentNode(branch.getUuid());
		NodeGraphFieldContainer container = findVersion(ac.getNodeParameters().getLanguageList(options()), branch.getUuid(), ac.getVersioningParameters()
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
		for (Tag tag : getTags(branch)) {
			// Tags can't be moved across branches thus we don't need to add the
			// tag family etag
			keyBuilder.append(tag.getETag(ac));
		}

		// branch specific children
		for (Node child : getChildren(branch.getUuid())) {
			if (ac.getUser().hasPermission(child, READ_PUBLISHED_PERM)) {
				keyBuilder.append("-");
				keyBuilder.append(child.getSchemaContainer().getName());
			}
		}

		// Publish state & availableLanguages
		for (NodeGraphFieldContainer c : getGraphFieldContainers(branch, PUBLISHED)) {
			keyBuilder.append(c.getLanguageTag() + "published");
		}
		for (NodeGraphFieldContainer c : getGraphFieldContainers(branch, DRAFT)) {
			keyBuilder.append(c.getLanguageTag() + "draft");
		}

		// breadcrumb
		keyBuilder.append("-");
		Node current = getParentNode(branch.getUuid());
		if (current != null) {
			while (current != null) {

				String key = current.getUuid() + current.getDisplayName(ac);
				keyBuilder.append(key);
				if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
					WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
					String url = linkReplacer.resolve(ac, branch.getUuid(), type, current.getUuid(), ac.getNodeParameters().getResolveLinks(),
						getProject().getName(), container.getLanguageTag());
					keyBuilder.append(url);
				}
				current = current.getParentNode(branch.getUuid());

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
			for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(branch, forVersion(versioiningParameters.getVersion()))) {
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
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/nodes/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR, UserImpl.class).nextOrNull();
	}

	@Override
	public MeshElementEventModel onDeleted() {
		throw new NotImplementedException("Use dedicated onDeleted method for nodes instead.");
	}

	public NodeMovedEventModel onNodeMoved(String branchUuid, Node target) {
		NodeMovedEventModel model = new NodeMovedEventModel();
		model.setEvent(NODE_MOVED);
		model.setBranchUuid(branchUuid);
		model.setProject(getProject().transformToReference());
		fillEventInfo(model);
		model.setTarget(target.transformToMinimalReference());
		return model;
	}

	@Override
	protected MeshProjectElementEventModel createEvent(MeshEvent event) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		model.setProject(getProject().transformToReference());
		fillEventInfo(model);
		return model;
	}

	public NodeMeshEventModel onReferenceUpdated(String uuid, SchemaContainer schema, String branchUuid, ContainerType type, String languageTag) {
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
	public NodeMeshEventModel onDeleted(String uuid, SchemaContainer schema, String branchUuid, ContainerType type, String languageTag) {
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
	public NodeTaggedEventModel onTagged(Tag tag, Branch branch, Assignment assignment) {
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
	public Single<NodeResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
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
	public PermissionChangedProjectElementEventModel onPermissionChanged(Role role) {
		PermissionChangedProjectElementEventModel model = new PermissionChangedProjectElementEventModel();
		fillPermissionChanged(model, role);
		return model;
	}
}
