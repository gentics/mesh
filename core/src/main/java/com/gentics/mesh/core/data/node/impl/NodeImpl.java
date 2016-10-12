package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static com.gentics.mesh.util.URIUtils.encodeFragment;
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
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
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
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.gentics.mesh.util.URIUtils;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * @see Node
 */
public class NodeImpl extends AbstractGenericFieldContainerVertex<NodeResponse, Node> implements Node {

	public static final String RELEASE_UUID_KEY = "releaseUuid";

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	public static void init(Database database) {
		database.addVertexType(NodeImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_PARENT_NODE);
		database.addCustomEdgeIndex(HAS_PARENT_NODE, "release", "in", RELEASE_UUID_KEY);
	}

	@Override
	public String getType() {
		return Node.TYPE;
	}

	@Override
	public Single<String> getPathSegment(InternalActionContext ac) {
		NodeParameters parameters = ac.getNodeParameters();
		VersioningParameters versioningParameters = ac.getVersioningParameters();
		NodeGraphFieldContainer container = findNextMatchingFieldContainer(parameters.getLanguageList(), ac.getRelease(getProject()).getUuid(),
				versioningParameters.getVersion());
		if (container != null) {
			String fieldName = container.getSchemaContainerVersion().getSchema().getSegmentField();
			StringGraphField field = container.getString(fieldName);
			if (field != null) {
				return Single.just(field.getString());
			}
		}
		return Single.error(error(BAD_REQUEST, "node_error_could_not_find_path_segment", getUuid()));
	}

	@Override
	public String getPathSegment(String releaseUuid, ContainerType type, String... languageTag) {

		// Check whether this node is the base node.
		if (getParentNode(releaseUuid) == null) {
			return "";
		}
		NodeGraphFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getGraphFieldContainer(tag, releaseUuid, type)) != null) {
				break;
			}
		}
		if (container != null) {
			String segmentFieldKey = container.getSchemaContainerVersion().getSchema().getSegmentField();
			// 1. The container may reference a schema which has no segment field set thus no path segment can be determined
			if (segmentFieldKey == null) {
				return null;
			}

			// 2. Try to load the path segment using the string field
			StringGraphField stringField = container.getString(segmentFieldKey);
			if (stringField != null) {
				return stringField.getString();
			}

			// 3. Try to load the path segment using the binary field since the string field could not be found
			if (stringField == null) {
				BinaryGraphField binaryField = container.getBinary(segmentFieldKey);
				if (binaryField != null) {
					return binaryField.getFileName();
				}
			}
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

		// for the path segments of the container, we add all (additional)
		// project languages to the list of languages for the fallback
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
			// for the path segments of the container, we allow ANY language (of the project)
			segment = current.getPathSegment(releaseUuid, type, projectLanguages);

			// Abort early if one of the path segments could not be resolved. We need to return a 404 in those cases.
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
	public void assertPublishConsistency(InternalActionContext ac) {

		NodeParameters parameters = new NodeParameters(ac);

		String releaseUuid = ac.getRelease(getProject()).getUuid();
		// Check whether the node got a published version and thus is published
		boolean isPublished = findNextMatchingFieldContainer(parameters.getLanguageList(), releaseUuid, "published") != null;

		// A published node must have also a published parent node.
		if (isPublished) {
			Node parentNode = getParentNode(releaseUuid);

			// Only assert consistency of parent nodes which are not project base nodes.
			if (parentNode != null && (!parentNode.getUuid().equals(getProject().getBaseNode().getUuid()))) {

				// Check whether the parent node has a published field container for the given release and language
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
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers() {
		return getGraphFieldContainers(getProject().getLatestRelease(), ContainerType.DRAFT);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getAllInitialGraphFieldContainers() {
		return outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).inV()
				.toListExplicit(NodeGraphFieldContainerImpl.class);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers(Release release, ContainerType type) {
		return getGraphFieldContainers(release.getUuid(), type);
	}

	// static Map<String, Object> map2 = new HashMap<>();

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers(String releaseUuid, ContainerType type) {
		// TODO ADD INDEX!
		// String key = "r:" + releaseUuid + "t:" + type + "i:" + getId();
		//
		// Object result = map2.get(key);
		// if(result!=null) {
		// return (List<? extends NodeGraphFieldContainer>) result;
		// }

		List<? extends NodeGraphFieldContainerImpl> list = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV().toListExplicit(NodeGraphFieldContainerImpl.class);
		// map2.put(key, list);
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public long getGraphFieldContainerCount() {
		return outE(HAS_FIELD_CONTAINER).or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode()),
				e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.PUBLISHED.getCode())).inV().count();
	}

	@Override
	public NodeGraphFieldContainer getLatestDraftFieldContainer(Language language) {
		return getGraphFieldContainer(language, getProject().getLatestRelease(), ContainerType.DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Language language, Release release, ContainerType type) {
		return getGraphFieldContainer(language, release, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestRelease().getUuid(), ContainerType.DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag, String releaseUuid, ContainerType type) {
		return getGraphFieldContainer(languageTag, releaseUuid, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User user) {
		return createGraphFieldContainer(language, release, user, null);
	}

	@Override
	public NodeGraphFieldContainer createGraphFieldContainer(Language language, Release release, User user, NodeGraphFieldContainer original) {
		NodeGraphFieldContainerImpl previous = null;
		EdgeFrame draftEdge = null;
		String languageTag = language.getLanguageTag();
		String releaseUuid = release.getUuid();

		// check whether there is a current draft version
		draftEdge = getGraphFieldContainerEdge(languageTag, releaseUuid, ContainerType.DRAFT);
		if (draftEdge != null) {
			previous = draftEdge.inV().nextOrDefault(NodeGraphFieldContainerImpl.class, null);
		}

		// Create the new container
		NodeGraphFieldContainerImpl container = getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		if (original != null) {
			container.setEditor(original.getEditor());
			container.setLastEditedTimestamp();
			container.setLanguage(language);
			container.setSchemaContainerVersion(original.getSchemaContainerVersion());
		} else {
			container.setEditor(user);
			container.setLastEditedTimestamp();
			container.setLanguage(language);
			container.setSchemaContainerVersion(release.getVersion(getSchemaContainer()));
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
		// We need to update the display field property since we created a new node graph field container.
		container.updateDisplayFieldValue();

		// create a new draft edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setReleaseUuid(releaseUuid);
		edge.setType(ContainerType.DRAFT);

		// if there is no initial edge, create one
		if (getGraphFieldContainerEdge(languageTag, releaseUuid, ContainerType.INITIAL) == null) {
			GraphFieldContainerEdge initialEdge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(languageTag);
			initialEdge.setReleaseUuid(releaseUuid);
			initialEdge.setType(ContainerType.INITIAL);
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
		TagEdge edge = addFramedEdge(HAS_TAG, tag.getImpl(), TagEdgeImpl.class);
		edge.setReleaseUuid(release.getUuid());
	}

	@Override
	public void removeTag(Tag tag, Release release) {
		outE(HAS_TAG).has(TagEdgeImpl.RELEASE_UUID_KEY, release.getUuid()).mark().inV().retain(tag.getImpl()).back().removeAll();
	}

	@Override
	public void setSchemaContainer(SchemaContainer schema) {
		setLinkOut(schema.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	@Override
	public SchemaContainer getSchemaContainer() {
		return out(HAS_SCHEMA_CONTAINER).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).toListExplicit(NodeImpl.class);
	}

	@Override
	public List<? extends Node> getChildren(String releaseUuid) {
		// return inE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).outV().has(NodeImpl.class).toListExplicit(NodeImpl.class);
		Database db = MeshInternal.get().database();
		FramedGraph graph = Database.getThreadLocalGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_PARENT_NODE.toLowerCase() + "_release", db.createComposedIndexKey(getId(), releaseUuid));
		List<Node> nodes = new ArrayList<>();
		Iterator<Edge> it = edges.iterator();
		while (it.hasNext()) {
			Vertex vertex = it.next().getVertex(Direction.OUT);
			nodes.add(graph.frameElementExplicit(vertex, NodeImpl.class));
		}
		return nodes;
	}

	@Override
	public Node getParentNode(String releaseUuid) {
		return outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).inV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public void setParentNode(String releaseUuid, Node parent) {
		outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).removeAll();
		addFramedEdge(HAS_PARENT_NODE, parent.getImpl()).setProperty(RELEASE_UUID_KEY, releaseUuid);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void setProject(Project project) {
		setLinkOut(project.getImpl(), ASSIGNED_TO_PROJECT);
	}

	@Override
	public Node create(User creator, SchemaContainerVersion schemaVersion, Project project) {
		return create(creator, schemaVersion, project, project.getLatestRelease());
	}

	/**
	 * Create a new node and make sure to delegate the creation request to the main node root aggregation node.
	 */
	@Override
	public Node create(User creator, SchemaContainerVersion schemaVersion, Project project, Release release) {
		// We need to use the (meshRoot)--(nodeRoot) node instead of the (project)--(nodeRoot) node.
		Node node = MeshInternal.get().boot().nodeRoot().create(creator, schemaVersion, project);
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
		VersioningParameters versioiningParameters = ac.getVersioningParameters();

		NodeResponse restNode = new NodeResponse();
		SchemaContainer container = getSchemaContainer();
		if (container == null) {
			throw error(BAD_REQUEST, "The schema container for node {" + getUuid() + "} could not be found.");
		}
		Release release = ac.getRelease(getProject());
		restNode.setAvailableLanguages(getAvailableLanguageNames(release, ContainerType.forVersion(versioiningParameters.getVersion())));

		setFields(ac, release, restNode, level, languageTags);
		setParentNodeInfo(ac, release, restNode);
		setRolePermissions(ac, restNode);
		setChildrenInfo(ac, release, restNode);
		setTagsToRest(ac, restNode, release);
		fillCommonRestFields(ac, restNode);
		setBreadcrumbToRest(ac, restNode);
		setPathsToRest(ac, restNode, release);
		return restNode;
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
			// Only the base node of the project has no parent. Therefore this node must be a container.
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
			// If a published version was requested, we check whether any published language variant exists for the node, if not, response with NOT_FOUND
			if (ContainerType.forVersion(versioiningParameters.getVersion()) == ContainerType.PUBLISHED
					&& getGraphFieldContainers(release, ContainerType.PUBLISHED).isEmpty()) {
				log.error("Could not find field container for languages {" + requestedLanguageTags + "} and release {" + release.getUuid()
						+ "} and version params version {" + versioiningParameters.getVersion() + "}, release {" + release.getUuid() + "}");
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_release_version", getUuid(), release.getUuid());
			}

			// If a specific version was requested, that does not exist, we also return NOT_FOUND
			if (ContainerType.forVersion(versioiningParameters.getVersion()) == ContainerType.INITIAL) {
				throw error(NOT_FOUND, "object_not_found_for_version", versioiningParameters.getVersion());
			}

			String langInfo = getLanguageInfo(requestedLanguageTags);
			if (log.isDebugEnabled()) {
				log.debug("The fields for node {" + getUuid() + "} can't be populated since the node has no matching language for the languages {"
						+ langInfo + "}. Fields will be empty.");
			}
			// No field container was found so we can only set the schema reference that points to the container (no version information will be included)
			restNode.setSchema(getSchemaContainer().transformToReference());
			// TODO return a 404 and adapt mesh rest client in order to return a mesh response
			// ac.data().put("statuscode", NOT_FOUND.code());
		} else {
			Schema schema = fieldContainer.getSchemaContainerVersion().getSchema();
			restNode.setContainer(schema.isContainer());
			restNode.setDisplayField(schema.getDisplayField());

			restNode.setLanguage(fieldContainer.getLanguage().getLanguageTag());
			// List<String> fieldsToExpand = ac.getExpandedFieldnames();
			// modify the language fallback list by moving the container's language to the front
			List<String> containerLanguageTags = new ArrayList<>(requestedLanguageTags);
			containerLanguageTags.remove(restNode.getLanguage());
			containerLanguageTags.add(0, restNode.getLanguage());

			// Schema reference
			restNode.setSchema(fieldContainer.getSchemaContainerVersion().transformToReference());

			// Version reference
			if (fieldContainer.getVersion() != null) {
				restNode.setVersion(new VersionReference(fieldContainer.getUuid(), fieldContainer.getVersion().toString()));
			}

			// editor and edited
			User editor = fieldContainer.getEditor();
			if (editor != null) {
				restNode.setEditor(editor.transformToReference());
			} else {
				log.error("Node {" + getUuid() + "} - container {" + fieldContainer.getLanguage().getLanguageTag() + "} has no editor");
			}

			// Convert unixtime to iso-8601
			String date = DateUtils.toISO8601(fieldContainer.getLastEditedTimestamp(), 0);
			restNode.setEdited(date);

			// Iterate over all fields and transform them to rest
			for (FieldSchema fieldEntry : schema.getFields()) {
				// boolean expandField = fieldsToExpand.contains(fieldEntry.getName()) || ac.getExpandAllFlag();
				Field restField = fieldContainer.getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, containerLanguageTags, level);
				if (fieldEntry.isRequired() && restField == null) {
					// TODO i18n
					//throw error(BAD_REQUEST, "The field {" + fieldEntry.getName()
					//		+ "} is a required field but it could not be found in the node. Please add the field using an update call or change the field schema and remove the required flag.");
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
			TagFamily tagFamily = tag.getTagFamily();
			String tagFamilyName = tagFamily.getName();
			String tagFamilyUuid = tagFamily.getUuid();
			TagReference reference = tag.transformToReference();
			TagFamilyTagGroup group = restNode.getTags().get(tagFamilyName);
			if (group == null) {
				group = new TagFamilyTagGroup();
				group.setUuid(tagFamilyUuid);
				restNode.getTags().put(tagFamilyName, group);
			}
			group.getItems().add(reference);
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
			ContainerType type = ContainerType.forVersion(versioiningParameters.getVersion());

			// Path
			WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
			String path = linkReplacer.resolve(releaseUuid, type, getUuid(), ac.getNodeParameters().getResolveLinks(), getProject().getName(),
					restNode.getLanguage());
			restNode.setPath(path);

			// languagePaths
			Map<String, String> languagePaths = new HashMap<>();
			for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(release,
					ContainerType.forVersion(versioiningParameters.getVersion()))) {
				Language currLanguage = currentFieldContainer.getLanguage();
				String languagePath = linkReplacer.resolve(releaseUuid, type, this, ac.getNodeParameters().getResolveLinks(),
						currLanguage.getLanguageTag());
				languagePaths.put(currLanguage.getLanguageTag(), languagePath);
			}
			restNode.setLanguagePaths(languagePaths);
		}
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

		Deque<NodeReferenceImpl> breadcrumb = new ArrayDeque<>();
		while (current != null) {
			// Don't add the base node to the breadcrumb
			// TODO should we add the basenode to the breadcrumb?
			if (current.getUuid().equals(this.getProject().getBaseNode().getUuid())) {
				break;
			}
			NodeReferenceImpl reference = new NodeReferenceImpl();
			reference.setUuid(current.getUuid());
			reference.setDisplayName(current.getDisplayName(ac));

			if (LinkType.OFF != ac.getNodeParameters().getResolveLinks()) {
				WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
				ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
				String url = linkReplacer.resolve(releaseUuid, type, current.getUuid(), ac.getNodeParameters().getResolveLinks(),
						getProject().getName(), ac.getNodeParameters().getLanguages());
				reference.setPath(url);
			}
			breadcrumb.add(reference);
			current = current.getParentNode(releaseUuid);
		}
		restNode.setBreadcrumb(breadcrumb);
	}

	@Override
	public Single<NavigationResponse> transformToNavigation(InternalActionContext ac) {
		NavigationParameters parameters = new NavigationParameters(ac);
		if (parameters.getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		return operateNoTx(() -> {
			// TODO assure that the schema version is correct
			if (!getSchemaContainer().getLatestVersion().getSchema().isContainer()) {
				throw error(BAD_REQUEST, "navigation_error_no_container");
			}
			String etagKey = buildNavigationEtagKey(ac, this, parameters.getMaxDepth(), 0, ac.getRelease(getProject()).getUuid(),
					ContainerType.forVersion(ac.getVersioningParameters().getVersion()));
			String etag = ETag.hash(etagKey);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				return Single.error(new NotModifiedException());
			} else {
				NavigationResponse response = new NavigationResponse();
				return buildNavigationResponse(ac, this, parameters.getMaxDepth(), 0, response, response.getRoot(),
						ac.getRelease(getProject()).getUuid(), ContainerType.forVersion(ac.getVersioningParameters().getVersion()));
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
		NavigationParameters parameters = new NavigationParameters(ac);
		StringBuilder builder = new StringBuilder();
		builder.append(node.getETag(ac));

		List<? extends Node> nodes = node.getChildren(ac.getUser(), releaseUuid, type);

		// Abort recursion when we reach the max level or when no more children can be found.
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
		List<? extends Node> nodes = node.getChildren(ac.getUser(), releaseUuid, type);
		List<Single<NavigationResponse>> obsResponses = new ArrayList<>();

		obsResponses.add(node.transformToRest(ac, 0).map(response -> {
			// Set current element data
			currentElement.setUuid(response.getUuid());
			currentElement.setNode(response);
			return navigation;
		}));

		// Abort recursion when we reach the max level or when no more children can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			List<Observable<NavigationResponse>> obsList = obsResponses.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());
			return Observable.merge(obsList).last().toSingle();
		}
		NavigationParameters parameters = new NavigationParameters(ac);
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
		return Observable.merge(obsList).last().toSingle();
	}

	@Override
	public NodeReferenceImpl transformToReference(InternalActionContext ac) {
		NodeReferenceImpl nodeReference = new NodeReferenceImpl();
		nodeReference.setUuid(getUuid());
		nodeReference.setDisplayName(getDisplayName(ac));
		nodeReference.setSchema(getSchemaContainer().transformToReference());
		return nodeReference;
	}

	@Override
	public NodeFieldListItem toListItem(InternalActionContext ac, String[] languageTags) {
		// Create the rest field and populate the fields
		NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(getUuid());
		String releaseUuid = ac.getRelease(null).getUuid();
		ContainerType type = ContainerType.forVersion(new VersioningParameters(ac).getVersion());
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			listItem.setUrl(MeshInternal.get().webRootLinkReplacer().resolve(releaseUuid, type, this, ac.getNodeParameters().getResolveLinks(),
					languageTags));
		}
		return listItem;
	}

	@Override
	public PublishStatusResponse transformToPublishStatus(InternalActionContext ac) {
		Release release = ac.getRelease(getProject());
		PublishStatusResponse publishStatus = new PublishStatusResponse();
		Map<String, PublishStatusModel> languages = new HashMap<>();
		publishStatus.setAvailableLanguages(languages);

		getGraphFieldContainers(release, ContainerType.PUBLISHED).stream().forEach(c -> {

			String date = DateUtils.toISO8601(c.getLastEditedTimestamp(), 0);

			PublishStatusModel status = new PublishStatusModel().setPublished(true)
					.setVersion(new VersionReference(c.getUuid(), c.getVersion().toString())).setPublisher(c.getEditor().transformToReference())
					.setPublishTime(date);
			languages.put(c.getLanguage().getLanguageTag(), status);
		});

		getGraphFieldContainers(release, ContainerType.DRAFT).stream().filter(c -> !languages.containsKey(c.getLanguage().getLanguageTag()))
				.forEach(c -> {
					PublishStatusModel status = new PublishStatusModel().setPublished(false)
							.setVersion(new VersionReference(c.getUuid(), c.getVersion().toString()));
					languages.put(c.getLanguage().getLanguageTag(), status);
				});

		return publishStatus;
	}

	@Override
	public List<Completable> publish(InternalActionContext ac, Release release) {
		String releaseUuid = release.getUuid();

		SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch();
		List<Completable> obs = new ArrayList<>();
		// publish all unpublished containers

		PublishParameters parameters = ac.getPublishParameters();

		List<? extends NodeGraphFieldContainer> unpublishedContainers = getGraphFieldContainers(release, ContainerType.DRAFT).stream()
				.filter(c -> !c.isPublished(releaseUuid)).collect(Collectors.toList());

		List<NodeGraphFieldContainer> published = unpublishedContainers.stream().map(c -> publish(c.getLanguage(), release, ac.getUser()))
				.collect(Collectors.toList());
		obs.add(addIndexBatch(batch, STORE_ACTION, published, releaseUuid, ContainerType.PUBLISHED).processAsync());

		// Handle recursion
		if (parameters.isRecursive()) {
			for (Node child : getChildren()) {
				obs.add(Completable.merge(child.publish(ac, release)));
			}
		}

		assertPublishConsistency(ac);
		return obs;
	}

	@Override
	public Completable publish(InternalActionContext ac) {
		Database db = MeshInternal.get().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		List<? extends NodeGraphFieldContainer> unpublishedContainers = getGraphFieldContainers(release, ContainerType.DRAFT).stream()
				.filter(c -> !c.isPublished(releaseUuid)).collect(Collectors.toList());

		// TODO check whether all required fields are filled
		List<Completable> obs = new ArrayList<>();

		obs.add(db.tx(() -> {
			SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
			SearchQueueBatch batch = queue.createBatch();
			// publish all unpublished containers
			List<NodeGraphFieldContainer> published = unpublishedContainers.stream().map(c -> publish(c.getLanguage(), release, ac.getUser()))
					.collect(Collectors.toList());

			// Handle recursion
			PublishParameters parameters = ac.getPublishParameters();
			if (parameters.isRecursive()) {
				for (Node node : getChildren()) {
					obs.add(node.publish(ac));
				}
			}

			assertPublishConsistency(ac);
			addIndexBatch(batch, STORE_ACTION, published, releaseUuid, ContainerType.PUBLISHED);
			return batch;
		}).processAsync());

		return Completable.merge(obs);
	}

	@Override
	public Completable takeOffline(InternalActionContext ac) {
		Database db = MeshInternal.get().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		return db.tx(() -> {
			SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
			SearchQueueBatch batch = queue.createBatch();
			List<? extends NodeGraphFieldContainer> published = getGraphFieldContainers(release, ContainerType.PUBLISHED);

			// Remove the published edge for each found container
			getGraphFieldContainerEdges(releaseUuid, ContainerType.PUBLISHED).stream().forEach(EdgeFrame::remove);
			// Reset the webroot property for each published container
			published.forEach(c -> c.getImpl().setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, null));

			// Handle recursion
			PublishParameters parameters = ac.getPublishParameters();
			if (parameters.isRecursive()) {
				for (Node node : getChildren()) {
					node.takeOffline(ac).await();
				}
			}

			assertPublishConsistency(ac);

			// Remove the published node from the index
			return addIndexBatch(batch, DELETE_ACTION, published, releaseUuid, ContainerType.PUBLISHED);
		}).processAsync();
	}

	@Override
	public PublishStatusModel transformToPublishStatus(InternalActionContext ac, String languageTag) {
		Release release = ac.getRelease(getProject());

		NodeGraphFieldContainer container = getGraphFieldContainer(languageTag, release.getUuid(), ContainerType.PUBLISHED);
		if (container != null) {
			String date = DateUtils.toISO8601(container.getLastEditedTimestamp(), 0);
			return new PublishStatusModel().setPublished(true)
					.setVersion(new VersionReference(container.getUuid(), container.getVersion().toString()))
					.setPublisher(container.getEditor().transformToReference()).setPublishTime(date);
		} else {
			container = getGraphFieldContainer(languageTag, release.getUuid(), ContainerType.DRAFT);
			if (container != null) {
				return new PublishStatusModel().setPublished(false)
						.setVersion(new VersionReference(container.getUuid(), container.getVersion().toString()));
			} else {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
		}
	}

	@Override
	public Completable publish(InternalActionContext ac, String languageTag) {
		Database db = MeshInternal.get().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		// get the draft version of the given language
		NodeGraphFieldContainer draftVersion = getGraphFieldContainer(languageTag, releaseUuid, ContainerType.DRAFT);

		// if not existent -> NOT_FOUND
		if (draftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		// If the located draft version was already published we are done
		if (draftVersion.isPublished(releaseUuid)) {
			return Completable.complete();
		}

		// TODO check whether all required fields are filled, if not -> unable to publish

		return db.tx(() -> {
			SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
			SearchQueueBatch batch = queue.createBatch();
			NodeGraphFieldContainer published = publish(draftVersion.getLanguage(), release, ac.getUser());

			// Invoke a store of the document since it must now also be added to the published index
			return addIndexBatch(batch, STORE_ACTION, Arrays.asList(published), release.getUuid(), ContainerType.PUBLISHED);
		}).processAsync();
	}

	@Override
	public Completable takeOffline(InternalActionContext ac, String languageTag) {
		Database db = MeshInternal.get().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		return db.tx(() -> {
			SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
			SearchQueueBatch batch = queue.createBatch();

			NodeGraphFieldContainer published = getGraphFieldContainer(languageTag, releaseUuid, ContainerType.PUBLISHED);

			if (published == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			// remove the "published" edge
			getGraphFieldContainerEdge(languageTag, releaseUuid, ContainerType.PUBLISHED).remove();
			published.getImpl().setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, null);

			assertPublishConsistency(ac);

			// Invoke a delete on the document since it must be removed from the published index
			return addIndexBatch(batch, DELETE_ACTION, Arrays.asList(published), releaseUuid, ContainerType.PUBLISHED);
		}).processAsync();
	}

	@Override
	public void setPublished(NodeGraphFieldContainer container, String releaseUuid) {
		String languageTag = container.getLanguage().getLanguageTag();

		// Remove an existing published edge
		EdgeFrame currentPublished = getGraphFieldContainerEdge(languageTag, releaseUuid, ContainerType.PUBLISHED);
		if (currentPublished != null) {
			// We need to remove the edge first since updateWebrootPathInfo will check the published edge again
			NodeGraphFieldContainerImpl oldPublishedContainer = currentPublished.inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
			currentPublished.remove();
			oldPublishedContainer.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_publish");
		}

		// create new published edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setReleaseUuid(releaseUuid);
		edge.setType(ContainerType.PUBLISHED);
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

		ContainerType type = ContainerType.forVersion(version);

		for (String languageTag : languageTags) {
			fieldContainer = getGraphFieldContainer(languageTag, releaseUuid, type);

			if (fieldContainer != null && type == ContainerType.INITIAL) {
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
		for (GraphFieldContainer container : getGraphFieldContainers()) {
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

	public void delete(boolean ignoreChecks, SearchQueueBatch batch) {
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
		getElement().remove();

	}

	@Override
	public void delete(SearchQueueBatch batch) {
		delete(false, batch);
	}

	@Override
	public void deleteFromRelease(Release release, SearchQueueBatch batch) {
		getGraphFieldContainers(release, ContainerType.DRAFT).forEach(container -> deleteLanguageContainer(release, container.getLanguage(), batch));
		String releaseUuid = release.getUuid();

		for (Node child : getChildren(releaseUuid)) {
			// remove the child from the release
			child.deleteFromRelease(release, batch);
		}

		// if the node has no more field containers in any release, it will be deleted
		if (getGraphFieldContainerCount() == 0) {
			delete(batch);
		} else {
			// otherwise we need to remove the "parent" edge for the release
			// first remove the "parent" edge (because the node itself will
			// probably not be deleted, but just removed from the release)
			outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).removeAll();
		}
	}

	/**
	 * Get a vertex traversal to find the children of this node, this user has read permission for
	 *
	 * @param requestUser
	 *            user
	 * @param releaseUuid
	 *            release uuid
	 * @param type
	 *            edge type
	 * @return vertex traversal
	 */
	private VertexTraversal<?, ?, ?> getChildrenTraversal(MeshAuthUser requestUser, String releaseUuid, ContainerType type) {
		String permLabel = type == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM.label() : READ_PERM.label();

		VertexTraversal<?, ?, ?> traversal = null;
		if (releaseUuid != null) {
			traversal = inE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).outV();
		} else {
			traversal = in(HAS_PARENT_NODE);
		}

		traversal = traversal.mark().in(permLabel).out(HAS_ROLE).in(HAS_USER).retain(requestUser.getImpl()).back();
		if (releaseUuid != null && type != null) {
			traversal = traversal.mark().outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
					.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).outV().back();
		}
		return traversal;
	}

	@Override
	public List<? extends Node> getChildren(MeshAuthUser requestUser, String releaseUuid, ContainerType type) {
		return getChildrenTraversal(requestUser, releaseUuid, type).toListExplicit(NodeImpl.class);
	}

	@Override
	public PageImpl<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, String releaseUuid, ContainerType type,
			PagingParameters pagingInfo) throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = getChildrenTraversal(requestUser, releaseUuid, type);
		return TraversalHelper.getPagedResult(traversal, pagingInfo, NodeImpl.class);
	}

	@Override
	public PageImpl<? extends Tag> getTags(Release release, PagingParameters params) throws InvalidArgumentException {
		// TODO add permissions
		VertexTraversal<?, ?, ?> traversal = TagEdgeImpl.getTagTraversal(this, release);
		return TraversalHelper.getPagedResult(traversal, params, TagImpl.class);
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
		NodeGraphFieldContainer latestDraftVersion = getGraphFieldContainer(language, release, ContainerType.DRAFT);

		// No existing container was found. This means that no conflict check can be performed. Conflict checks only occur for updates.
		if (latestDraftVersion == null) {
			// Create a new field container
			latestDraftVersion = createGraphFieldContainer(language, release, ac.getUser());
			latestDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());

			// check whether the node has a parent node in this
			// release, if not, we set the parent node from the previous
			// release (if any)
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
		} else {
			if (requestModel.getVersion() == null || isEmpty(requestModel.getVersion().getNumber())) {
				throw error(BAD_REQUEST, "node_error_version_missing");
			}

			// Make sure the container was already migrated. Otherwise the update can't proceed.
			SchemaContainerVersion schemaContainerVersion = latestDraftVersion.getSchemaContainerVersion();
			if (!latestDraftVersion.getSchemaContainerVersion().equals(release.getVersion(schemaContainerVersion.getSchemaContainer()))) {
				throw error(BAD_REQUEST, "node_error_migration_incomplete");
			}

			// Load the base version field container in order to create the diff
			NodeGraphFieldContainer baseVersionContainer = findNextMatchingFieldContainer(Arrays.asList(requestModel.getLanguage()),
					release.getUuid(), requestModel.getVersion().getNumber());
			if (baseVersionContainer == null) {
				throw error(BAD_REQUEST, "node_error_draft_not_found", requestModel.getVersion().getNumber(), requestModel.getLanguage());
			}

			latestDraftVersion.getSchemaContainerVersion().getSchema().assertForUnhandledFields(requestModel.getFields());

			// TODO handle simplified case in which baseContainerVersion and latestDraftVersion are equal
			List<FieldContainerChange> baseVersionDiff = baseVersionContainer.compareTo(latestDraftVersion);
			List<FieldContainerChange> requestVersionDiff = latestDraftVersion.compareTo(requestModel.getFields());

			// Compare both sets of change sets
			List<FieldContainerChange> intersect = baseVersionDiff.stream().filter(requestVersionDiff::contains).collect(Collectors.toList());

			// Check whether the update was not based on the latest draft version. In that case a conflict check needs to occur.
			if (!latestDraftVersion.getVersion().equals(requestModel.getVersion().getNumber())) {

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

			// Make sure to only update those fields which have been altered in between the latest version and the current request. Remove unaffected fields
			// from the rest request in order to prevent duplicate references.
			// We don't want to touch field that have not been changed. Otherwise the graph field references would no longer point to older revisions of the
			// same field.
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
				NodeGraphFieldContainer newDraftVersion = createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion);
				// Update the existing fields
				newDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());
				latestDraftVersion = newDraftVersion;
			}
		}
		addIndexBatch(batch, STORE_ACTION, Arrays.asList(latestDraftVersion), release.getUuid(), ContainerType.DRAFT);
		return this;
	}

	@Override
	public void moveTo(InternalActionContext ac, Node targetNode, SearchQueueBatch batch) {
		// TODO should we add a guard that terminates this loop when it runs to
		// long?
		// Check whether the target node is part of the subtree of the source
		// node.
		String releaseUuid = ac.getRelease(getProject()).getUuid();
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
		// update the webroot path info for every field container to ensure

		// Update published graph field containers
		getGraphFieldContainers(releaseUuid, ContainerType.PUBLISHED).stream()
				.forEach(container -> container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_move"));

		// Update draft graph field containers
		getGraphFieldContainers(releaseUuid, ContainerType.DRAFT).stream()
				.forEach(container -> container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_move"));

		assertPublishConsistency(ac);
		addIndexBatchEntry(batch, STORE_ACTION);
	}

	@Override
	public void deleteLanguageContainer(Release release, Language language, SearchQueueBatch batch) {
		NodeGraphFieldContainer container = getGraphFieldContainer(language, release, ContainerType.DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", language.getLanguageTag());
		}

		container.deleteFromRelease(release, batch);

		// if the published version is a different container, we remove this as well
		container = getGraphFieldContainer(language, release, ContainerType.PUBLISHED);
		if (container != null) {
			container.deleteFromRelease(release, batch);
		}
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// batch.addEntry(getParentNode(), UPDATE_ACTION);
	}

	@Override
	public SearchQueueBatch addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// Add all graph field containers for all releases to the batch
		getProject().getReleaseRoot().findAll().forEach((release) -> {
			String releaseUuid = release.getUuid();
			for (ContainerType type : Arrays.asList(ContainerType.DRAFT, ContainerType.PUBLISHED)) {
				getGraphFieldContainers(release, type).forEach((container) -> {
					container.addIndexBatchEntry(batch, action, releaseUuid, type);
				});
			}
		});
		addRelatedEntries(batch, action);
		return batch;
	}

	/**
	 * Create an index batch for the given list of containers
	 * 
	 * @param batch
	 * @param action
	 *            action
	 * @param containers
	 *            containers
	 * @param releaseUuid
	 *            release Uuid
	 * @param type
	 *            type
	 * @return batch
	 */
	public SearchQueueBatch addIndexBatch(SearchQueueBatch batch, SearchQueueEntryAction action, List<? extends NodeGraphFieldContainer> containers,
			String releaseUuid, ContainerType type) {
		for (NodeGraphFieldContainer container : containers) {
			container.addIndexBatchEntry(batch, action, releaseUuid, type);
		}
		addRelatedEntries(batch, action);
		return batch;
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
					return new PathSegment(this, field, container.getLanguage().getLanguageTag());
				}
			}

			// No luck yet - lets check whether a binary field matches the
			// segmentField
			BinaryGraphField binaryField = container.getBinary(segmentFieldName);
			if (binaryField == null) {
				log.error(
						"The node {" + getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName + "}");
			} else {
				String binaryFilename = binaryField.getFileName();
				if (segment.equals(binaryFilename)) {
					return new PathSegment(this, binaryField, container.getLanguage().getLanguageTag());
				}
			}
		}
		return null;
	}

	@Override
	public Single<Path> resolvePath(String releaseUuid, ContainerType type, Path path, Stack<String> pathStack) {
		if (pathStack.isEmpty()) {
			return Single.just(path);
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
		// Parameters
		Release release = ac.getRelease(getProject());
		VersioningParameters versioiningParameters = ac.getVersioningParameters();
		ContainerType type = ContainerType.forVersion(versioiningParameters.getVersion());

		Node parentNode = getParentNode(release.getUuid());
		NodeGraphFieldContainer container = findNextMatchingFieldContainer(ac.getNodeParameters().getLanguageList(), release.getUuid(),
				ac.getVersioningParameters().getVersion());

		StringBuilder keyBuilder = new StringBuilder();

		/**
		 * node uuid
		 * 
		 * The node uuid must be part of the etag computation.
		 */
		keyBuilder.append(getUuid());
		keyBuilder.append("-");

		/**
		 * release uuid
		 */
		keyBuilder.append(release.getUuid());
		keyBuilder.append("-");

		//TODO version, language list

		// We can omit further etag keys since this would return a 404 anyhow since the requested container could not be found. 
		if (container == null) {
			keyBuilder.append("404-no-container");
			return keyBuilder.toString();
		}

		/**
		 * parent node
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
		 * expansion (all)
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
			// Tags can't be moved across releases thus we don't need to add the tag family etag
			keyBuilder.append(tag.getETag(ac));
		}

		// release specific children
		for (Node child : getChildren(release.getUuid())) {
			if (ac.getUser().hasPermission(child, READ_PERM)) {
				keyBuilder.append("-");
				keyBuilder.append(child.getSchemaContainer().getName());
			}
		}

		// editor etag - (can be omitted since update would also affect the NGFC)
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
			for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(release,
					ContainerType.forVersion(versioiningParameters.getVersion()))) {
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
			Role role = MeshRootImpl.getInstance().getRoleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				Set<GraphPermission> permSet = role.getPermissions(this);
				Set<String> humanNames = new HashSet<>();
				for (GraphPermission permission : permSet) {
					humanNames.add(permission.getSimpleName());
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
}
