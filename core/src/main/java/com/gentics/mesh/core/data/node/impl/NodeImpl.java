package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.ContainerType.forVersion;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.URIUtils.encodeFragment;
import static com.tinkerpop.blueprints.Direction.OUT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
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

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagEdge;
import com.gentics.mesh.core.data.TagFamily;
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
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagListUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
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
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.URIUtils;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.tx.Tx;
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

	public static final String RELEASE_UUID_KEY = "releaseUuid";

	public static void init(Database database) {
		// Node.EventType.CREATED
		database.addVertexType(NodeImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_PARENT_NODE);
		database.addCustomEdgeIndex(HAS_PARENT_NODE, "release", "in", RELEASE_UUID_KEY);
		database.addCustomEdgeIndex(HAS_FIELD_CONTAINER, "field", "out", GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY,
				GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY);
	}

	@Override
	public String getPathSegment(String releaseUuid, ContainerType type, String... languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(releaseUuid) == null) {
			return "";
		}

		// Find the first matching container and fallback to other listed languages
		NodeGraphFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getGraphFieldContainer(tag, releaseUuid, type)) != null) {
				break;
			}
		}
		if (container != null) {
			return container.getSegmentFieldValue();
		}
		return null;
	}

	@Override
	public String getPath(String releaseUuid, ContainerType type, String... languageTag) {
		List<String> segments = new ArrayList<>();
		String segment = getPathSegment(releaseUuid, type, languageTag);
		if (segment == null) {
			return null;
		}
		segments.add(segment);

		// For the path segments of the container, we add all (additional)
		// project languages to the list of languages for the fallback.
		List<String> langList = new ArrayList<>();
		langList.addAll(Arrays.asList(languageTag));

		// TODO maybe we only want to get the project languages?
		langList.addAll(MeshInternal.get().boot().getAllLanguageTags());
		String[] projectLanguages = langList.toArray(new String[langList.size()]);
		Node current = this;
		while (current != null) {
			current = current.getParentNode(releaseUuid);
			if (current == null || current.getParentNode(releaseUuid) == null) {
				break;
			}
			// For the path segments of the container, we allow ANY language (of the project)
			segment = current.getPathSegment(releaseUuid, type, projectLanguages);

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
		Iterator<String> it = segments.iterator();
		while (it.hasNext()) {
			String fragment = it.next();
			builder.append("/").append(URIUtils.encodeFragment(fragment));
		}
		return builder.toString();

	}

	@Override
	public void assertPublishConsistency(InternalActionContext ac, Release release) {

		NodeParameters parameters = ac.getNodeParameters();

		String releaseUuid = release.getUuid();
		// Check whether the node got a published version and thus is published
		boolean isPublished = findNextMatchingFieldContainer(parameters.getLanguageList(), releaseUuid, "published") != null;

		// A published node must have also a published parent node.
		if (isPublished) {
			Node parentNode = getParentNode(releaseUuid);

			// Only assert consistency of parent nodes which are not project
			// base nodes.
			if (parentNode != null && (!parentNode.getUuid().equals(getProject().getBaseNode().getUuid()))) {

				// Check whether the parent node has a published field container
				// for the given release and language
				NodeGraphFieldContainer fieldContainer = parentNode.findNextMatchingFieldContainer(parameters.getLanguageList(), releaseUuid,
						"published");
				if (fieldContainer == null) {
					log.error("Could not find published field container for node {" + parentNode.getUuid() + "} in release {" + releaseUuid + "}");
					throw error(BAD_REQUEST, "node_error_parent_containers_not_published", parentNode.getUuid());
				}
			}
		}

		// A draft node can't have any published child nodes.
		if (!isPublished) {

			for (Node node : getChildren()) {
				NodeGraphFieldContainer fieldContainer = node.findNextMatchingFieldContainer(parameters.getLanguageList(), releaseUuid, "published");
				if (fieldContainer != null) {
					log.error("Found published field container for node {" + node.getUuid() + "} in release {" + releaseUuid + "}. Node is child of {"
							+ getUuid() + "}");
					throw error(BAD_REQUEST, "node_error_children_containers_still_published", node.getUuid());
				}
			}
		}
	}

	@Override
	public List<? extends Tag> getTags(Release release) {
		return TagEdgeImpl.getTagTraversal(this, release).toListExplicit(TagImpl.class);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getDraftGraphFieldContainers() {
		return getGraphFieldContainers(getProject().getLatestRelease(), DRAFT);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getAllInitialGraphFieldContainers() {
		return outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, INITIAL.getCode()).inV()
				.toListExplicit(NodeGraphFieldContainerImpl.class);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers(Release release, ContainerType type) {
		return getGraphFieldContainers(release.getUuid(), type);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers(String releaseUuid, ContainerType type) {
		List<? extends NodeGraphFieldContainerImpl> list = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV().toListExplicit(NodeGraphFieldContainerImpl.class);
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public long getGraphFieldContainerCount() {
		return outE(HAS_FIELD_CONTAINER).or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, DRAFT.getCode()),
				e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, PUBLISHED.getCode())).inV().count();
	}

	@Override
	public NodeGraphFieldContainer getLatestDraftFieldContainer(Language language) {
		return getGraphFieldContainer(language, getProject().getLatestRelease(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Language language, Release release, ContainerType type) {
		return getGraphFieldContainer(language, release, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestRelease().getUuid(), DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag, String releaseUuid, ContainerType type) {
		return getGraphFieldContainer(languageTag, releaseUuid, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User editor) {
		return createGraphFieldContainer(language, release, editor, null, true);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User editor, NodeGraphFieldContainer original,
			boolean handleDraftEdge) {
		NodeGraphFieldContainerImpl previous = null;
		EdgeFrame draftEdge = null;
		String languageTag = language.getLanguageTag();
		String releaseUuid = release.getUuid();

		// check whether there is a current draft version

		if (handleDraftEdge) {
			draftEdge = getGraphFieldContainerEdge(languageTag, releaseUuid, DRAFT);
			if (draftEdge != null) {
				previous = draftEdge.inV().nextOrDefault(NodeGraphFieldContainerImpl.class, null);
			}
		}

		// Create the new container
		NodeGraphFieldContainerImpl container = getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		if (original != null) {
			container.setEditor(editor);
			container.setLastEditedTimestamp();
			container.setLanguage(language);
			container.setSchemaContainerVersion(original.getSchemaContainerVersion());
		} else {
			container.setEditor(editor);
			container.setLastEditedTimestamp();
			container.setLanguage(language);
			// We need create a new container with no reference. So use the latest version available to use.
			container.setSchemaContainerVersion(release.findLatestSchemaVersion(getSchemaContainer()));
		}
		if (previous != null) {
			// set the next version number
			container.setVersion(previous.getVersion().nextDraft());
			previous.setNextVersion(container);
		} else {
			// set the initial version number
			container.setVersion(new VersionNumber());
		}

		// clone the original or the previous container
		if (original != null) {
			container.clone(original);
		} else if (previous != null) {
			container.clone(previous);
		}

		// remove existing draft edge
		if (draftEdge != null) {
			previous.setProperty(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY, null);
			container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_update");
			draftEdge.remove();
		}
		// We need to update the display field property since we created a new
		// node graph field container.
		container.updateDisplayFieldValue();

		if (handleDraftEdge) {
			// create a new draft edge
			GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			edge.setLanguageTag(languageTag);
			edge.setReleaseUuid(releaseUuid);
			edge.setType(DRAFT);
		}

		// if there is no initial edge, create one
		if (getGraphFieldContainerEdge(languageTag, releaseUuid, INITIAL) == null) {
			GraphFieldContainerEdge initialEdge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(languageTag);
			initialEdge.setReleaseUuid(releaseUuid);
			initialEdge.setType(INITIAL);
		}

		return container;
	}

	/**
	 * Get an existing edge.
	 * 
	 * @param languageTag
	 *            language tag
	 * @param releaseUuid
	 *            release uuid
	 * @param type
	 *            edge type
	 * @return existing edge or null
	 */
	protected EdgeFrame getGraphFieldContainerEdge(String languageTag, String releaseUuid, ContainerType type) {
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag)
				.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		if (edgeTraversal.hasNext()) {
			return edgeTraversal.next();
		} else {
			return null;
		}
	}

	/**
	 * Get all graph field.
	 * 
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	protected List<? extends EdgeFrame> getGraphFieldContainerEdges(String releaseUuid, ContainerType type) {
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		return edgeTraversal.toList();
	}

	@Override
	public void addTag(Tag tag, Release release) {
		removeTag(tag, release);
		TagEdge edge = addFramedEdge(HAS_TAG, tag, TagEdgeImpl.class);
		edge.setReleaseUuid(release.getUuid());
	}

	@Override
	public void removeTag(Tag tag, Release release) {
		outE(HAS_TAG).has(TagEdgeImpl.RELEASE_UUID_KEY, release.getUuid()).mark().inV().retain(tag).back().removeAll();
	}

	@Override
	public void removeAllTags(Release release) {
		outE(HAS_TAG).has(TagEdgeImpl.RELEASE_UUID_KEY, release.getUuid()).removeAll();
	}

	@Override
	public void setSchemaContainer(SchemaContainer schema) {
		setLinkOut(schema, HAS_SCHEMA_CONTAINER);
	}

	@Override
	public SchemaContainer getSchemaContainer() {
		return out(HAS_SCHEMA_CONTAINER).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public Iterable<Node> getChildren() {
		Iterator<VertexFrame> it = in(HAS_PARENT_NODE).iterator();
		Iterable<VertexFrame> iterable = () -> it;
		Stream<Node> stream = StreamSupport.stream(iterable.spliterator(), false).map(frame -> frame.reframe(NodeImpl.class));
		return () -> stream.iterator();
	}

	@Override
	public Iterable<Node> getChildren(String releaseUuid) {
		Database db = MeshInternal.get().database();
		FramedGraph graph = Tx.getActive().getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_PARENT_NODE.toLowerCase() + "_release", db.createComposedIndexKey(getId(), releaseUuid));
		Iterator<Edge> it = edges.iterator();
		Iterable<Edge> iterable = () -> it;
		Stream<Edge> stream = StreamSupport.stream(iterable.spliterator(), false);

		Stream<Node> nstream = stream.map(edge -> {
			Vertex vertex = edge.getVertex(Direction.OUT);
			return graph.frameElementExplicit(vertex, NodeImpl.class);
		});
		return () -> nstream.iterator();
	}

	@Override
	public Node getParentNode(String releaseUuid) {
		return outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).inV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public void setParentNode(String releaseUuid, Node parent) {
		outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).removeAll();
		addFramedEdge(HAS_PARENT_NODE, parent).setProperty(RELEASE_UUID_KEY, releaseUuid);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void setProject(Project project) {
		setLinkOut(project, ASSIGNED_TO_PROJECT);
	}

	@Override
	public Node create(User creator, SchemaContainerVersion schemaVersion, Project project) {
		return create(creator, schemaVersion, project, project.getLatestRelease());
	}

	/**
	 * Create a new node and make sure to delegate the creation request to the main node root aggregation node.
	 */
	@Override
	public Node create(User creator, SchemaContainerVersion schemaVersion, Project project, Release release, String uuid) {
		// We need to use the (meshRoot)--(nodeRoot) node instead of the
		// (project)--(nodeRoot) node.
		Node node = MeshInternal.get().boot().nodeRoot().create(creator, schemaVersion, project, uuid);
		node.setParentNode(release.getUuid(), this);
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

		// Increment level for each node transformation to avoid stackoverflow situations
		level = level + 1;
		NodeResponse restNode = new NodeResponse();
		SchemaContainer container = getSchemaContainer();
		if (container == null) {
			throw error(BAD_REQUEST, "The schema container for node {" + getUuid() + "} could not be found.");
		}
		Release release = ac.getRelease(getProject());
		restNode.setAvailableLanguages(getLanguageInfo(ac));
		setFields(ac, release, restNode, level, languageTags);
		setParentNodeInfo(ac, release, restNode);
		setRolePermissions(ac, restNode);
		setChildrenInfo(ac, release, restNode);
		setTagsToRest(ac, restNode, release);
		fillCommonRestFields(ac, restNode);
		setBreadcrumbToRest(ac, restNode);
		setPathsToRest(ac, restNode, release);
		setProjectReference(ac, restNode);
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
	 * @param release
	 *            Use the given release to identify the release specific parent node
	 * @param restNode
	 *            Model to be updated
	 * @return
	 */
	private void setParentNodeInfo(InternalActionContext ac, Release release, NodeResponse restNode) {
		Node parentNode = getParentNode(release.getUuid());
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
	 * @param release
	 *            Release which will be used to locate the correct field container
	 * @param restNode
	 *            Rest model which will be updated
	 * @param level
	 *            Current level of transformation
	 * @param languageTags
	 * @return
	 */
	private void setFields(InternalActionContext ac, Release release, NodeResponse restNode, int level, String... languageTags) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		NodeParameters nodeParameters = ac.getNodeParameters();

		List<String> requestedLanguageTags = null;
		if (languageTags != null && languageTags.length > 0) {
			requestedLanguageTags = Arrays.asList(languageTags);
		} else {
			requestedLanguageTags = nodeParameters.getLanguageList();
		}

		// First check whether the NGFC for the requested language,release and version could be found.
		NodeGraphFieldContainer fieldContainer = findNextMatchingFieldContainer(requestedLanguageTags, release.getUuid(),
				versioiningParameters.getVersion());
		if (fieldContainer == null) {
			// If a published version was requested, we check whether any
			// published language variant exists for the node, if not, response
			// with NOT_FOUND
			if (forVersion(versioiningParameters.getVersion()) == PUBLISHED && getGraphFieldContainers(release, PUBLISHED).isEmpty()) {
				log.error("Could not find field container for languages {" + requestedLanguageTags + "} and release {" + release.getUuid()
						+ "} and version params version {" + versioiningParameters.getVersion() + "}, release {" + release.getUuid() + "}");
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_release_version", getUuid(), release.getUuid());
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
			restNode.setSchema(getSchemaContainer().transformToReference());
			// TODO BUG Issue #119 - Actually we would need to throw a 404 in these cases but many current implementations rely on the empty node response.
			// The response will also contain information about other languages and general structure information.
			// We should change this behaviour and update the client implementations.
			// throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
		} else {
			Schema schema = fieldContainer.getSchemaContainerVersion().getSchema();
			restNode.setContainer(schema.isContainer());
			restNode.setDisplayField(schema.getDisplayField());

			restNode.setLanguage(fieldContainer.getLanguage().getLanguageTag());
			// List<String> fieldsToExpand = ac.getExpandedFieldnames();
			// modify the language fallback list by moving the container's
			// language to the front
			List<String> containerLanguageTags = new ArrayList<>(requestedLanguageTags);
			containerLanguageTags.remove(restNode.getLanguage());
			containerLanguageTags.add(0, restNode.getLanguage());

			// Schema reference
			restNode.setSchema(fieldContainer.getSchemaContainerVersion().transformToReference());

			// Version reference
			if (fieldContainer.getVersion() != null) {
				restNode.setVersion(fieldContainer.getVersion().toString());
			}

			// editor and edited
			User editor = fieldContainer.getEditor();
			if (editor != null) {
				restNode.setEditor(editor.transformToReference());
			} else {
				log.error("Node {" + getUuid() + "} - container {" + fieldContainer.getLanguage().getLanguageTag() + "} has no editor");
			}

			restNode.setEdited(fieldContainer.getLastEditedDate());

			// Iterate over all fields and transform them to rest
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
					restNode.getFields().put(fieldEntry.getName(), null);
				}
				if (restField == null) {
					if (log.isDebugEnabled()) {
						log.debug("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
					}
				} else {
					restNode.getFields().put(fieldEntry.getName(), restField);
				}

			}
		}
	}

	/**
	 * Set the children info to the rest model.
	 * 
	 * @param ac
	 * @param release
	 *            Release which will be used to identify the release specific child nodes
	 * @param restNode
	 *            Rest model which will be updated
	 */
	private void setChildrenInfo(InternalActionContext ac, Release release, NodeResponse restNode) {
		Map<String, NodeChildrenInfo> childrenInfo = new HashMap<>();
		for (Node child : getChildren(release.getUuid())) {
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
	 * @param release
	 *            Release which will be used to identify the release specific tags
	 * @return
	 */
	private void setTagsToRest(InternalActionContext ac, NodeResponse restNode, Release release) {
		for (Tag tag : getTags(release)) {
			TagReference reference = tag.transformToReference();
			restNode.getTags().add(reference);
		}
	}

	/**
	 * Add the release specific webroot and language paths to the given rest node.
	 * 
	 * @param ac
	 * @param restNode
	 *            Rest model which will be updated
	 * @param release
	 *            Release which will be used to identify the nodes relations and thus the correct path can be determined
	 * @return
	 */
	private void setPathsToRest(InternalActionContext ac, NodeResponse restNode, Release release) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			String releaseUuid = ac.getRelease(getProject()).getUuid();
			ContainerType type = forVersion(versioiningParameters.getVersion());

			LinkType linkType = ac.getNodeParameters().getResolveLinks();

			// Path
			WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
			String path = linkReplacer.resolve(releaseUuid, type, getUuid(), linkType, getProject().getName(), restNode.getLanguage());
			restNode.setPath(path);

			// languagePaths
			restNode.setLanguagePaths(getLanguagePaths(ac, linkType, release));
		}
	}

	@Override
	public Map<String, String> getLanguagePaths(InternalActionContext ac, LinkType linkType, Release release) {
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		String releaseUuid = ac.getRelease(getProject()).getUuid();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		Map<String, String> languagePaths = new HashMap<>();
		WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
		for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(release, forVersion(versioiningParameters.getVersion()))) {
			Language currLanguage = currentFieldContainer.getLanguage();
			String languagePath = linkReplacer.resolve(releaseUuid, type, this, linkType, currLanguage.getLanguageTag());
			languagePaths.put(currLanguage.getLanguageTag(), languagePath);
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
		String releaseUuid = ac.getRelease(getProject()).getUuid();
		Node current = this.getParentNode(releaseUuid);
		// The project basenode has no breadcrumb
		if (current == null) {
			return;
		}

		Deque<NodeReference> breadcrumb = new ArrayDeque<>();
		while (current != null) {
			// Don't add the base node to the breadcrumb
			// TODO should we add the basenode to the breadcrumb?
			if (current.getUuid().equals(this.getProject().getBaseNode().getUuid())) {
				break;
			}
			NodeReference reference = current.transformToReference(ac);
			breadcrumb.add(reference);
			current = current.getParentNode(releaseUuid);
		}
		restNode.setBreadcrumb(breadcrumb);
	}

	@Override
	public Deque<Node> getBreadcrumbNodes(InternalActionContext ac) {
		String releaseUuid = ac.getRelease(getProject()).getUuid();
		Node current = this.getParentNode(releaseUuid);
		// The project basenode has no breadcrumb
		if (current == null) {
			return new ArrayDeque<>();
		}

		Deque<Node> breadcrumb = new ArrayDeque<>();
		while (current != null) {
			// Don't add the base node to the breadcrumb
			// TODO should we add the basenode to the breadcrumb?
			if (current.getUuid().equals(this.getProject().getBaseNode().getUuid())) {
				break;
			}
			breadcrumb.add(current);
			current = current.getParentNode(releaseUuid);
		}
		return breadcrumb;
	}

	@Override
	public Single<NavigationResponse> transformToNavigation(InternalActionContext ac) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		if (parameters.getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		return MeshInternal.get().database().operateTx(() -> {
			// TODO assure that the schema version is correct
			if (!getSchemaContainer().getLatestVersion().getSchema().isContainer()) {
				throw error(BAD_REQUEST, "navigation_error_no_container");
			}
			String etagKey = buildNavigationEtagKey(ac, this, parameters.getMaxDepth(), 0, ac.getRelease(getProject()).getUuid(),
					forVersion(ac.getVersioningParameters().getVersion()));
			String etag = ETag.hash(etagKey);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				return Single.error(new NotModifiedException());
			} else {
				NavigationResponse response = new NavigationResponse();
				return buildNavigationResponse(ac, this, parameters.getMaxDepth(), 0, response, response, ac.getRelease(getProject()).getUuid(),
						forVersion(ac.getVersioningParameters().getVersion()));
			}
		});
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
	 * @param releaseUuid
	 *            Release uuid used to extract selected tree structure
	 * @param type
	 * @return
	 */
	private String buildNavigationEtagKey(InternalActionContext ac, Node node, int maxDepth, int level, String releaseUuid, ContainerType type) {
		NavigationParametersImpl parameters = new NavigationParametersImpl(ac);
		StringBuilder builder = new StringBuilder();
		builder.append(node.getETag(ac));

		List<? extends Node> nodes = node.getChildren(ac.getUser(), releaseUuid, null, type);

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return builder.toString();
		}
		for (Node child : nodes) {
			if (child.getSchemaContainer().getLatestVersion().getSchema().isContainer()) {
				builder.append(buildNavigationEtagKey(ac, child, maxDepth, level + 1, releaseUuid, type));
			} else if (parameters.isIncludeAll()) {
				builder.append(buildNavigationEtagKey(ac, child, maxDepth, level, releaseUuid, type));
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
	 * @param releaseUuid
	 *            release uuid to be used for loading children of nodes
	 * @param type
	 *            container type to be used for transformation
	 * @return
	 */
	private Single<NavigationResponse> buildNavigationResponse(InternalActionContext ac, Node node, int maxDepth, int level,
			NavigationResponse navigation, NavigationElement currentElement, String releaseUuid, ContainerType type) {
		List<? extends Node> nodes = node.getChildren(ac.getUser(), releaseUuid, null, type);
		List<Single<NavigationResponse>> obsResponses = new ArrayList<>();

		obsResponses.add(node.transformToRest(ac, 0).map(response -> {
			// Set current element data
			currentElement.setUuid(response.getUuid());
			currentElement.setNode(response);
			return navigation;
		}));

		// Abort recursion when we reach the max level or when no more children
		// can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			List<Observable<NavigationResponse>> obsList = obsResponses.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
			return Observable.merge(obsList).lastOrError();
		}
		NavigationParameters parameters = new NavigationParametersImpl(ac);
		// Add children
		for (Node child : nodes) {
			// TODO assure that the schema version is correct?
			// TODO also allow navigations over containers
			if (child.getSchemaContainer().getLatestVersion().getSchema().isContainer()) {
				NavigationElement childElement = new NavigationElement();
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				currentElement.getChildren().add(childElement);
				obsResponses.add(buildNavigationResponse(ac, child, maxDepth, level + 1, navigation, childElement, releaseUuid, type));
			} else if (parameters.isIncludeAll()) {
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				NavigationElement childElement = new NavigationElement();
				currentElement.getChildren().add(childElement);
				obsResponses.add(buildNavigationResponse(ac, child, maxDepth, level, navigation, childElement, releaseUuid, type));
			}
		}
		List<Observable<NavigationResponse>> obsList = obsResponses.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
		return Observable.merge(obsList).lastOrError();
	}

	@Override
	public NodeReference transformToReference(InternalActionContext ac) {
		Release release = ac.getRelease(getProject());

		NodeReference nodeReference = new NodeReference();
		nodeReference.setUuid(getUuid());
		nodeReference.setDisplayName(getDisplayName(ac));
		nodeReference.setSchema(getSchemaContainer().transformToReference());
		nodeReference.setProjectName(getProject().getName());
		if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
			WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
			ContainerType type = forVersion(ac.getVersioningParameters().getVersion());
			String url = linkReplacer.resolve(release.getUuid(), type, this, ac.getNodeParameters().getResolveLinks(),
					ac.getNodeParameters().getLanguages());
			nodeReference.setPath(url);
		}
		return nodeReference;
	}

	@Override
	public NodeFieldListItem toListItem(InternalActionContext ac, String[] languageTags) {
		// Create the rest field and populate the fields
		NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(getUuid());
		String releaseUuid = ac.getRelease(getProject()).getUuid();
		ContainerType type = forVersion(new VersioningParametersImpl(ac).getVersion());
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			listItem.setUrl(MeshInternal.get().webRootLinkReplacer().resolve(releaseUuid, type, this, ac.getNodeParameters().getResolveLinks(),
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
		Release release = ac.getRelease(getProject());

		getGraphFieldContainers(release, PUBLISHED).stream().forEach(c -> {

			String date = DateUtils.toISO8601(c.getLastEditedTimestamp(), 0);

			PublishStatusModel status = new PublishStatusModel().setPublished(true).setVersion(c.getVersion().toString())
					.setPublisher(c.getEditor().transformToReference()).setPublishDate(date);
			languages.put(c.getLanguage().getLanguageTag(), status);
		});

		getGraphFieldContainers(release, DRAFT).stream().filter(c -> !languages.containsKey(c.getLanguage().getLanguageTag())).forEach(c -> {
			PublishStatusModel status = new PublishStatusModel().setPublished(false).setVersion(c.getVersion().toString());
			languages.put(c.getLanguage().getLanguageTag(), status);
		});
		return languages;
	}

	@Override
	public void publish(InternalActionContext ac, Release release, SearchQueueBatch batch) {
		String releaseUuid = release.getUuid();
		PublishParameters parameters = ac.getPublishParameters();

		batch.store(this, releaseUuid, ContainerType.PUBLISHED, false);

		// Handle recursion
		if (parameters.isRecursive()) {
			for (Node child : getChildren()) {
				child.publish(ac, release, batch);
			}
		}
		assertPublishConsistency(ac, release);
	}

	@Override
	public void publish(InternalActionContext ac, SearchQueueBatch batch) {
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		List<? extends NodeGraphFieldContainer> unpublishedContainers = getGraphFieldContainers(release, ContainerType.DRAFT).stream()
				.filter(c -> !c.isPublished(releaseUuid)).collect(Collectors.toList());

		// publish all unpublished containers and handle recursion
		// publish all unpublished containers
		unpublishedContainers.stream().map(c -> publish(c.getLanguage(), release, ac.getUser())).collect(Collectors.toList());

		PublishParameters parameters = ac.getPublishParameters();
		if (parameters.isRecursive()) {
			for (Node node : getChildren()) {
				node.publish(ac, batch);
			}
		}

		assertPublishConsistency(ac, release);
		batch.store(this, releaseUuid, PUBLISHED, false);
	}

	@Override
	public void takeOffline(InternalActionContext ac, SearchQueueBatch batch, Release release, PublishParameters parameters) {
		List<? extends NodeGraphFieldContainer> published = getGraphFieldContainers(release, PUBLISHED);

		String releaseUuid = release.getUuid();

		// Remove the published edge for each found container
		List<? extends NodeGraphFieldContainer> publishedContainers = getGraphFieldContainers(releaseUuid, PUBLISHED);
		getGraphFieldContainerEdges(releaseUuid, PUBLISHED).stream().forEach(EdgeFrame::remove);
		// Reset the webroot property for each published container
		published.forEach(c -> c.setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, null));

		// Handle recursion
		if (parameters.isRecursive()) {
			for (Node node : getChildren()) {
				node.takeOffline(ac, batch, release, parameters);
			}
		}

		assertPublishConsistency(ac, release);

		// Remove the published node from the index
		for (NodeGraphFieldContainer container : publishedContainers) {
			batch.delete(container, releaseUuid, PUBLISHED, false);
		}
	}

	@Override
	public void takeOffline(InternalActionContext ac, SearchQueueBatch batch) {
		Database db = MeshInternal.get().database();
		Release release = ac.getRelease(getProject());
		PublishParameters parameters = ac.getPublishParameters();
		db.tx(() -> {
			takeOffline(ac, batch, release, parameters);
			return this;
		});
	}

	@Override
	public PublishStatusModel transformToPublishStatus(InternalActionContext ac, String languageTag) {
		Release release = ac.getRelease(getProject());

		NodeGraphFieldContainer container = getGraphFieldContainer(languageTag, release.getUuid(), PUBLISHED);
		if (container != null) {
			String date = container.getLastEditedDate();
			return new PublishStatusModel().setPublished(true).setVersion(container.getVersion().toString())
					.setPublisher(container.getEditor().transformToReference()).setPublishDate(date);
		} else {
			container = getGraphFieldContainer(languageTag, release.getUuid(), DRAFT);
			if (container == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			return new PublishStatusModel().setPublished(false).setVersion(container.getVersion().toString());
		}
	}

	@Override
	public void publish(InternalActionContext ac, SearchQueueBatch batch, String languageTag) {
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		// get the draft version of the given language
		NodeGraphFieldContainer draftVersion = getGraphFieldContainer(languageTag, releaseUuid, DRAFT);

		// if not existent -> NOT_FOUND
		if (draftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		// If the located draft version was already published we are done
		if (draftVersion.isPublished(releaseUuid)) {
			return;
		}

		// TODO check whether all required fields are filled, if not -> unable to publish

		publish(draftVersion.getLanguage(), release, ac.getUser());
		// Invoke a store of the document since it must now also be added to the published index
		batch.store(this, release.getUuid(), PUBLISHED, false);
	}

	@Override
	public void takeOffline(InternalActionContext ac, SearchQueueBatch batch, Release release, String languageTag) {
		String releaseUuid = release.getUuid();

		// 1. Locate the published container
		NodeGraphFieldContainer published = getGraphFieldContainer(languageTag, releaseUuid, PUBLISHED);
		if (published == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}
		// 2. Remove the "published" edge
		getGraphFieldContainerEdge(languageTag, releaseUuid, PUBLISHED).remove();
		published.setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, null);

		assertPublishConsistency(ac, release);

		// 3. Invoke a delete on the document since it must be removed from the published index
		batch.delete(published, releaseUuid, PUBLISHED, false);
	}

	@Override
	public void setPublished(NodeGraphFieldContainer container, String releaseUuid) {
		String languageTag = container.getLanguage().getLanguageTag();

		// Remove an existing published edge
		EdgeFrame currentPublished = getGraphFieldContainerEdge(languageTag, releaseUuid, PUBLISHED);
		if (currentPublished != null) {
			// We need to remove the edge first since updateWebrootPathInfo will
			// check the published edge again
			NodeGraphFieldContainerImpl oldPublishedContainer = currentPublished.inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
			currentPublished.remove();
			oldPublishedContainer.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_publish");
		}

		// create new published edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setReleaseUuid(releaseUuid);
		edge.setType(PUBLISHED);
		container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_publish");
	}

	@Override
	public NodeGraphFieldContainer publish(Language language, Release release, User user) {
		String releaseUuid = release.getUuid();

		// create published version
		NodeGraphFieldContainer newVersion = createGraphFieldContainer(language, release, user);
		newVersion.setVersion(newVersion.getVersion().nextPublished());

		setPublished(newVersion, releaseUuid);
		return newVersion;
	}

	@Override
	public NodeGraphFieldContainer findNextMatchingFieldContainer(List<String> languageTags, String releaseUuid, String version) {
		NodeGraphFieldContainer fieldContainer = null;

		ContainerType type = forVersion(version);

		for (String languageTag : languageTags) {
			fieldContainer = getGraphFieldContainer(languageTag, releaseUuid, type);

			if (fieldContainer != null && type == INITIAL) {
				while (fieldContainer != null && !version.equals(fieldContainer.getVersion().toString())) {
					fieldContainer = fieldContainer.getNextVersion();
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
			languageTags.add(container.getLanguage().getLanguageTag());
		}
		return languageTags;
	}

	@Override
	public List<String> getAvailableLanguageNames(Release release, ContainerType type) {
		List<String> languageTags = new ArrayList<>();
		for (GraphFieldContainer container : getGraphFieldContainers(release, type)) {
			languageTags.add(container.getLanguage().getLanguageTag());
		}
		return languageTags;
	}

	@Override
	public void delete(SearchQueueBatch batch, boolean ignoreChecks) {
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
		for (Node child : getChildren()) {
			child.delete(batch);
		}
		// delete all initial containers (which will delete all containers)
		for (NodeGraphFieldContainer container : getAllInitialGraphFieldContainers()) {
			container.delete(batch);
		}
		if (log.isDebugEnabled()) {
			log.debug("Deleting node {" + getUuid() + "} vertex.");
		}
		getElement().remove();

	}

	@Override
	public void delete(SearchQueueBatch batch) {
		delete(batch, false);
	}

	@Override
	public void deleteFromRelease(InternalActionContext ac, Release release, SearchQueueBatch batch, boolean ignoreChecks) {

		DeleteParameters parameters = ac.getDeleteParameters();

		// 1. Remove subfolders from release
		String releaseUuid = release.getUuid();

		for (Node child : getChildren(releaseUuid)) {
			if (!parameters.isRecursive()) {
				throw error(BAD_REQUEST, "node_error_delete_failed_node_has_children");
			}
			child.deleteFromRelease(ac, release, batch, ignoreChecks);
		}

		// 2. Delete all language containers
		for (NodeGraphFieldContainer container : getGraphFieldContainers(release, DRAFT)) {
			deleteLanguageContainer(ac, release, container.getLanguage(), batch, false);
		}

		// 3. Now check if the node has no more field containers in any release. We can delete it in those cases
		if (getGraphFieldContainerCount() == 0) {
			delete(batch);
		} else {
			// Otherwise we need to remove the "parent" edge for the release
			// first remove the "parent" edge (because the node itself will
			// probably not be deleted, but just removed from the release)
			outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).removeAll();
		}
	}

	/**
	 * Get a vertex traversal to find the children of this node, this user has read permission for.
	 *
	 * @param requestUser
	 *            user
	 * @param releaseUuid
	 *            release uuid
	 * @param languageTags
	 *            Only list nodes which match the given language tags. Don't filter if the language tags list is null
	 * @param type
	 *            edge type
	 * @return vertex traversal
	 */
	private VertexTraversal<?, ?, ?> getChildrenTraversal(MeshAuthUser requestUser, String releaseUuid, List<String> languageTags,
			ContainerType type) {
		String permLabel = type == PUBLISHED ? READ_PUBLISHED_PERM.label() : READ_PERM.label();

		VertexTraversal<?, ?, ?> traversal = null;
		if (releaseUuid != null) {
			traversal = inE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).outV();
		} else {
			traversal = in(HAS_PARENT_NODE);
		}
		traversal = traversal.mark().in(permLabel).out(HAS_ROLE).in(HAS_USER).retain(requestUser).back();
		if (releaseUuid != null || type != null) {
			EdgeTraversal<?, ?, ?> edgeTraversal = traversal.mark().outE(HAS_FIELD_CONTAINER);
			if (releaseUuid != null) {
				edgeTraversal = edgeTraversal.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid);
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
	public List<? extends Node> getChildren(MeshAuthUser requestUser, String releaseUuid, List<String> languageTags, ContainerType type) {
		return getChildrenTraversal(requestUser, releaseUuid, languageTags, type).toListExplicit(NodeImpl.class);
	}

	@Override
	public TransformablePage<? extends Node> getChildren(InternalActionContext ac, List<String> languageTags, String releaseUuid, ContainerType type,
			PagingParameters pagingInfo) {
		String indexName = "e." + HAS_PARENT_NODE.toLowerCase() + "_release";
		Object indexKey = db.createComposedIndexKey(getId(), releaseUuid);

		GraphPermission perm = type == PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		return new DynamicTransformablePageImpl<NodeImpl>(ac.getUser(), indexName, indexKey, NodeImpl.class, pagingInfo, perm, (item) -> {

			// Filter out nodes which do not provide one of the specified language tags and type
			if (languageTags != null) {
				Iterator<Edge> edgeIt = item.getEdges(OUT, HAS_FIELD_CONTAINER).iterator();
				while (edgeIt.hasNext()) {
					Edge edge = edgeIt.next();
					String currentType = edge.getProperty(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY);
					if (!type.getCode().equals(currentType)) {
						continue;
					}
					String languageTag = edge.getProperty(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY);
					if (languageTags.contains(languageTag)) {
						return true;
					}
				}
				return false;
			}
			return true;
		}, true);
	}

	@Override
	public TransformablePage<? extends Tag> getTags(User user, PagingParameters params, Release release) {
		VertexTraversal<?, ?, ?> traversal = TagEdgeImpl.getTagTraversal(this, release);
		return new DynamicTransformablePageImpl<Tag>(user, traversal, params, READ_PERM, TagImpl.class);
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			// TODO for release?
			for (Node child : getChildren()) {
				child.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public String getDisplayName(InternalActionContext ac) {
		NodeParameters nodeParameters = ac.getNodeParameters();
		VersioningParameters versioningParameters = ac.getVersioningParameters();

		NodeGraphFieldContainer container = findNextMatchingFieldContainer(nodeParameters.getLanguageList(), ac.getRelease(getProject()).getUuid(),
				versioningParameters.getVersion());
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
	public Node update(InternalActionContext ac, SearchQueueBatch batch) {
		NodeUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), NodeUpdateRequest.class);
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "error_language_not_set");
		}
		Language language = MeshInternal.get().boot().languageRoot().findByLanguageTag(requestModel.getLanguage());
		if (language == null) {
			throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
		}
		Release release = ac.getRelease(getProject());
		NodeGraphFieldContainer latestDraftVersion = getGraphFieldContainer(language, release, DRAFT);

		// Check whether this is the first time that an update for the given language and release occurs. In this case a new container must be created.
		// This means that no conflict check can be performed. Conflict checks only occur for updates.
		if (latestDraftVersion == null) {
			// Create a new field container
			latestDraftVersion = createGraphFieldContainer(language, release, ac.getUser());
			latestDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());

			// Check whether the node has a parent node in this release, if not, we set the parent node from the previous release (if any)
			if (getParentNode(release.getUuid()) == null) {
				Node previousParent = null;
				Release previousRelease = release.getPreviousRelease();
				while (previousParent == null && previousRelease != null) {
					previousParent = getParentNode(previousRelease.getUuid());
					previousRelease = previousRelease.getPreviousRelease();
				}

				if (previousParent != null) {
					setParentNode(release.getUuid(), previousParent);
				}
			}
			batch.store(latestDraftVersion, release.getUuid(), DRAFT, false);
		} else {
			if (requestModel.getVersion() == null || isEmpty(requestModel.getVersion())) {
				throw error(BAD_REQUEST, "node_error_version_missing");
			}

			// Make sure the container was already migrated. Otherwise the update can't proceed.
			SchemaContainerVersion schemaContainerVersion = latestDraftVersion.getSchemaContainerVersion();
			if (!latestDraftVersion.getSchemaContainerVersion()
					.equals(release.findLatestSchemaVersion(schemaContainerVersion.getSchemaContainer()))) {
				throw error(BAD_REQUEST, "node_error_migration_incomplete");
			}

			// Load the base version field container in order to create the diff
			NodeGraphFieldContainer baseVersionContainer = findNextMatchingFieldContainer(Arrays.asList(requestModel.getLanguage()),
					release.getUuid(), requestModel.getVersion());
			if (baseVersionContainer == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", requestModel.getVersion(), requestModel.getLanguage());
			}

			latestDraftVersion.getSchemaContainerVersion().getSchema().assertForUnhandledFields(requestModel.getFields());

			// TODO handle simplified case in which baseContainerVersion and
			// latestDraftVersion are equal
			List<FieldContainerChange> baseVersionDiff = baseVersionContainer.compareTo(latestDraftVersion);
			List<FieldContainerChange> requestVersionDiff = latestDraftVersion.compareTo(requestModel.getFields());

			// Compare both sets of change sets
			List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains).collect(Collectors.toList());

			// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
			if (!latestDraftVersion.getVersion().equals(requestModel.getVersion())) {

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
				NodeGraphFieldContainer newDraftVersion = createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion, true);
				// Update the existing fields
				newDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());
				latestDraftVersion = newDraftVersion;
				batch.store(newDraftVersion, release.getUuid(), DRAFT, false);
			}
		}
		return this;
	}

	@Override
	public TransformablePage<? extends Tag> updateTags(InternalActionContext ac, SearchQueueBatch batch) {
		Project project = getProject();
		Release release = ac.getRelease();
		TagListUpdateRequest request = JsonUtil.readValue(ac.getBodyAsString(), TagListUpdateRequest.class);
		TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
		User user = ac.getUser();
		batch.store(this);
		removeAllTags(release);
		for (TagReference tagReference : request.getTags()) {
			if (!tagReference.isSet()) {
				throw error(BAD_REQUEST, "tag_error_name_or_uuid_missing");
			}
			if (isEmpty(tagReference.getTagFamily())) {
				throw error(BAD_REQUEST, "tag_error_tagfamily_not_set");
			}
			// 1. Locate the tag family
			TagFamily tagFamily = tagFamilyRoot.findByName(tagReference.getTagFamily());
			// Tag Family could not be found so lets create a new one
			if (tagFamily == null) {
				throw error(NOT_FOUND, "object_not_found_for_name", tagReference.getTagFamily());
			}
			// 2. The uuid was specified so lets try to load the tag this way
			if (!isEmpty(tagReference.getUuid())) {
				Tag tag = tagFamily.findByUuid(tagReference.getUuid());
				if (tag == null) {
					throw error(NOT_FOUND, "object_not_found_for_uuid", tagReference.getUuid());
				}
				addTag(tag, release);
			} else {
				Tag tag = tagFamily.findByName(tagReference.getName());
				// Tag with name could not be found so create it
				if (tag == null) {
					if (user.hasPermission(tagFamily, CREATE_PERM)) {
						tag = tagFamily.create(tagReference.getName(), project, user);
						user.addCRUDPermissionOnRole(tagFamily, CREATE_PERM, tag);
						batch.store(tag, false);
						batch.store(tagFamily, false);
					} else {
						throw error(FORBIDDEN, "tag_error_missing_perm_on_tag_family", tagFamily.getName(), tagFamily.getUuid(),
								tagReference.getName());
					}
				}
				addTag(tag, release);
			}
		}
		return getTags(user, ac.getPagingParameters(), release);
	}

	@Override
	public void moveTo(InternalActionContext ac, Node targetNode, SearchQueueBatch batch) {
		// TODO should we add a guard that terminates this loop when it runs to
		// long?

		// Check whether the target node is part of the subtree of the source
		// node.
		// We must detect and prevent such actions because those would
		// invalidate the tree structure
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();
		Node parent = targetNode.getParentNode(releaseUuid);
		while (parent != null) {
			if (parent.getUuid().equals(getUuid())) {
				throw error(BAD_REQUEST, "node_move_error_not_allowed_to_move_node_into_one_of_its_children");
			}
			parent = parent.getParentNode(releaseUuid);
		}

		if (!targetNode.getSchemaContainer().getLatestVersion().getSchema().isContainer()) {
			throw error(BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		}

		if (getUuid().equals(targetNode.getUuid())) {
			throw error(BAD_REQUEST, "node_move_error_same_nodes");
		}

		setParentNode(releaseUuid, targetNode);

		// Update published graph field containers
		getGraphFieldContainers(releaseUuid, PUBLISHED).stream().forEach(container -> {
			container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_move");
		});
		batch.store(this, releaseUuid, PUBLISHED, false);

		// Update draft graph field containers
		getGraphFieldContainers(releaseUuid, DRAFT).stream().forEach(container -> {
			container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_move");
		});
		batch.store(this, releaseUuid, DRAFT, false);

		assertPublishConsistency(ac, release);
	}

	@Override
	public void deleteLanguageContainer(InternalActionContext ac, Release release, Language language, SearchQueueBatch batch,
			boolean failForLastContainer) {

		// 1. Check whether the container has also a published variant. We need to take it offline in those cases
		NodeGraphFieldContainer container = getGraphFieldContainer(language, release, PUBLISHED);
		if (container != null) {
			takeOffline(ac, batch, release, language.getLanguageTag());
		}

		// 2. Load the draft container and remove it from the release
		container = getGraphFieldContainer(language, release, DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", language.getLanguageTag());
		}
		container.deleteFromRelease(release, batch);
		// No need to delete the published variant because if the container was published the take offline call handled it

		// 3. Check whether this was be the last container of the node for this release
		DeleteParameters parameters = ac.getDeleteParameters();
		if (failForLastContainer) {
			List<? extends NodeGraphFieldContainer> draftContainers = getGraphFieldContainers(release.getUuid(), DRAFT);
			List<? extends NodeGraphFieldContainer> publishContainers = getGraphFieldContainers(release.getUuid(), PUBLISHED);
			boolean wasLastContainer = draftContainers.isEmpty() && publishContainers.isEmpty();

			if (!parameters.isRecursive() && wasLastContainer) {
				throw error(BAD_REQUEST, "node_error_delete_failed_last_container_for_release");
			}
			// Also delete the node and children
			if (parameters.isRecursive() && wasLastContainer) {
				deleteFromRelease(ac, release, batch, false);
			}
		}

	}

	@Override
	public PathSegment getSegment(String releaseUuid, ContainerType type, String segment) {

		// Check the different language versions
		for (NodeGraphFieldContainer container : getGraphFieldContainers(releaseUuid, type)) {
			Schema schema = container.getSchemaContainerVersion().getSchema();
			String segmentFieldName = schema.getSegmentField();
			// First check whether a string field exists for the given name
			StringGraphField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegment(container, field, container.getLanguage().getLanguageTag());
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
					return new PathSegment(container, binaryField, container.getLanguage().getLanguageTag());
				}
			}
		}
		return null;
	}

	@Override
	public Path resolvePath(String releaseUuid, ContainerType type, Path path, Stack<String> pathStack) {
		if (pathStack.isEmpty()) {
			return path;
		}
		String segment = pathStack.pop();

		if (log.isDebugEnabled()) {
			log.debug("Resolving for path segment {" + segment + "}");
		}

		// Check all childnodes
		for (Node childNode : getChildren(releaseUuid)) {
			PathSegment pathSegment = childNode.getSegment(releaseUuid, type, segment);
			if (pathSegment != null) {
				path.addSegment(pathSegment);
				return childNode.resolvePath(releaseUuid, type, path, pathStack);
			}
		}
		throw error(NOT_FOUND, "node_not_found_for_path", path.getTargetPath());

	}

	/**
	 * Generate the etag for nodes. The etag consists of:
	 * <ul>
	 * <li>uuid of the node</li>
	 * <li>parent node uuid (which is release specific)</li>
	 * <li>version and language specific etag of the field container</li>
	 * <li>availableLanguages</li>
	 * <li>breadcrumb</li>
	 * <li>webroot path &amp; language paths</li>
	 * <li>permissions</li>
	 * </ul>
	 */
	@Override
	public String getETag(InternalActionContext ac) {
		String superkey = super.getETag(ac);

		// Parameters
		Release release = ac.getRelease(getProject());
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		ContainerType type = forVersion(versioiningParameters.getVersion());

		Node parentNode = getParentNode(release.getUuid());
		NodeGraphFieldContainer container = findNextMatchingFieldContainer(ac.getNodeParameters().getLanguageList(), release.getUuid(),
				ac.getVersioningParameters().getVersion());

		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(superkey);

		/**
		 * release uuid
		 */
		keyBuilder.append(release.getUuid());
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

		// release specific tags
		for (Tag tag : getTags(release)) {
			// Tags can't be moved across releases thus we don't need to add the
			// tag family etag
			keyBuilder.append(tag.getETag(ac));
		}

		// release specific children
		for (Node child : getChildren(release.getUuid())) {
			if (ac.getUser().hasPermission(child, READ_PERM)) {
				keyBuilder.append("-");
				keyBuilder.append(child.getSchemaContainer().getName());
			}
		}

		// editor etag - (can be omitted since update would also affect the
		// NGFC)
		// creator etag
		// keyBuilder.append("-");
		// keyBuilder.append(getCreator().getETag(ac));

		// availableLanguages
		keyBuilder.append("-");
		keyBuilder.append(Arrays.toString(getAvailableLanguageNames(release, type).toArray()));

		// breadcrumb
		keyBuilder.append("-");
		Node current = getParentNode(release.getUuid());
		if (current != null) {
			while (current != null) {

				String key = current.getUuid() + current.getDisplayName(ac);
				keyBuilder.append(key);
				if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
					WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
					String url = linkReplacer.resolve(release.getUuid(), type, current.getUuid(), ac.getNodeParameters().getResolveLinks(),
							getProject().getName(), container.getLanguage().getLanguageTag());
					keyBuilder.append(url);
				}
				current = current.getParentNode(release.getUuid());

			}
		}

		/**
		 * webroot path & language paths
		 * 
		 * The webroot and language paths must be included in the etag computation in order to invalidate the etag once a node language gets updated or once the
		 * display name of any parent node changes.
		 */
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {

			WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
			String path = linkReplacer.resolve(release.getUuid(), type, getUuid(), ac.getNodeParameters().getResolveLinks(), getProject().getName(),
					container.getLanguage().getLanguageTag());
			keyBuilder.append(path);

			// languagePaths
			for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(release, forVersion(versioiningParameters.getVersion()))) {
				Language currLanguage = currentFieldContainer.getLanguage();
				keyBuilder.append(currLanguage.getLanguageTag() + "=" + linkReplacer.resolve(release.getUuid(), type, this,
						ac.getNodeParameters().getResolveLinks(), currLanguage.getLanguageTag()));
			}

		}

		/**
		 * permissions (&roleUuid query parameter aware)
		 * 
		 * Permissions can change and thus must be included in the etag computation in order to invalidate the etag once the permissions change.
		 */
		String roleUuid = ac.getRolePermissionParameters().getRoleUuid();
		if (!isEmpty(roleUuid)) {
			Role role = MeshInternal.get().boot().meshRoot().getRoleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				Set<GraphPermission> permSet = role.getPermissions(this);
				Set<String> humanNames = new HashSet<>();
				for (GraphPermission permission : permSet) {
					humanNames.add(permission.getRestPerm().getName());
				}
				String[] names = humanNames.toArray(new String[humanNames.size()]);
				keyBuilder.append(names);
			}

		}

		if (log.isDebugEnabled()) {
			log.debug("Creating etag from key {" + keyBuilder.toString() + "}");
		}
		return ETag.hash(keyBuilder.toString());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/" + encodeFragment(getProject().getName()) + "/nodes/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public Single<NodeResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return MeshInternal.get().database().operateTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
