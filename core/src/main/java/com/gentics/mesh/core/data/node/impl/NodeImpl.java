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
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import java.util.stream.Collectors;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
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
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagEdgeImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.error.NodeVersionConflictException;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.TakeOfflineParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.TraversalHelper;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see Node
 */
public class NodeImpl extends AbstractGenericFieldContainerVertex<NodeResponse, Node> implements Node {

	public static final String RELEASE_UUID_KEY = "releaseUuid";

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(NodeImpl.class);
	}

	@Override
	public String getType() {
		return Node.TYPE;
	}

	@Override
	public Observable<String> getPathSegment(InternalActionContext ac) {
		NodeParameters parameters = ac.getNodeParameters();
		VersioningParameters versioningParameters = ac.getVersioningParameters();
		NodeGraphFieldContainer container = findNextMatchingFieldContainer(parameters.getLanguageList(), ac.getRelease(getProject()).getUuid(),
				versioningParameters.getVersion());
		if (container != null) {
			String fieldName = container.getSchemaContainerVersion().getSchema().getSegmentField();
			StringGraphField field = container.getString(fieldName);
			if (field != null) {
				return Observable.just(field.getString());
			}
		}
		return Observable.error(error(BAD_REQUEST, "node_error_could_not_find_path_segment", getUuid()));
	}

	@Override
	public Observable<String> getPathSegment(String releaseUuid, Type type, String... languageTag) {
		NodeGraphFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getGraphFieldContainer(tag, releaseUuid, type)) != null) {
				break;
			}
		}
		if (container != null) {
			String fieldName = container.getSchemaContainerVersion().getSchema().getSegmentField();
			// 1. Try to load the path segment using the string field
			StringGraphField stringField = container.getString(fieldName);
			if (stringField != null) {
				return Observable.just(stringField.getString());
			}

			// 2. Try to load the path segment using the binary field since the string field could not be found
			if (stringField == null) {
				BinaryGraphField binaryField = container.getBinary(fieldName);
				if (binaryField != null) {
					return Observable.just(binaryField.getFileName());
				}
			}
		}
		return Observable.error(error(BAD_REQUEST, "node_error_could_not_find_path_segment", getUuid()));
	}

	@Override
	public Observable<String> getPath(String releaseUuid, Type type, String... languageTag) throws UnsupportedEncodingException {
		List<Observable<String>> segments = new ArrayList<>();

		segments.add(getPathSegment(releaseUuid, type, languageTag));
		Node current = this;

		// for the path segments of the container, we add all (additional)
		// project languages to the list of languages for the fallback
		List<String> langList = new ArrayList<>();
		langList.addAll(Arrays.asList(languageTag));
		// TODO maybe we only want to get the project languags?
		for (Language l : MeshRoot.getInstance().getLanguageRoot().findAll()) {
			String tag = l.getLanguageTag();
			if (!langList.contains(tag)) {
				langList.add(tag);
			}
		}
		String[] projectLanguages = langList.toArray(new String[langList.size()]);

		while (current != null) {
			current = current.getParentNode(releaseUuid);
			if (current == null || current.getParentNode(releaseUuid) == null) {
				break;
			}
			// for the path segments of the container, we allow ANY language (of the project)
			segments.add(current.getPathSegment(releaseUuid, type, projectLanguages));
		}

		Collections.reverse(segments);
		return RxUtil.concatList(segments).toList().map(list -> {
			StringBuilder builder = new StringBuilder();
			Iterator<String> it = list.iterator();
			while (it.hasNext()) {
				try {
					builder.append("/").append(URLEncoder.encode(it.next(), "UTF-8"));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			return builder.toString();
		});
	}

	@Override
	public void assertPublishConsistency(InternalActionContext ac) {

		NodeParameters parameters = new NodeParameters(ac);

		String releaseUuid = ac.getRelease(getProject()).getUuid();
		boolean isPublished = findNextMatchingFieldContainer(parameters.getLanguageList(), releaseUuid, "published") != null;

		// A published node must have also a published parent node.
		if (isPublished) {
			Node parentNode = getParentNode(releaseUuid);

			// Only assert consistency of parent nodes which are not project base nodes.
			if (!parentNode.getUuid().equals(getProject().getBaseNode().getUuid())) {

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
	public Observable<String> getPath(InternalActionContext ac) {
		List<Observable<String>> segments = new ArrayList<>();
		segments.add(getPathSegment(ac));
		Node current = this;
		String releaseUuid = ac.getRelease(getProject()).getUuid();
		while (current != null) {
			current = current.getParentNode(releaseUuid);
			if (current == null || current.getParentNode(releaseUuid) == null) {
				break;
			}
			segments.add(current.getPathSegment(ac));
		}

		Collections.reverse(segments);
		return RxUtil.concatList(segments).reduce((a, b) -> {
			return "/" + a + "/" + b;
		});
	}

	@Override
	public List<? extends Tag> getTags(Release release) {
		return TagEdgeImpl.getTagTraversal(this, release).toListExplicit(TagImpl.class);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers() {
		return getGraphFieldContainers(getProject().getLatestRelease(), Type.DRAFT);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getAllInitialGraphFieldContainers() {
		return outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.INITIAL.getCode()).inV()
				.has(NodeGraphFieldContainerImpl.class).toListExplicit(NodeGraphFieldContainerImpl.class);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers(Release release, Type type) {
		return getGraphFieldContainers(release.getUuid(), type);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers(String releaseUuid, Type type) {
		return outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).inV().has(NodeGraphFieldContainerImpl.class)
				.toListExplicit(NodeGraphFieldContainerImpl.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public long getGraphFieldContainerCount() {
		return outE(HAS_FIELD_CONTAINER)
				.or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.DRAFT.getCode()),
						e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.PUBLISHED.getCode()))
				.inV().has(NodeGraphFieldContainerImpl.class).count();
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Language language) {
		return getGraphFieldContainer(language, getProject().getLatestRelease(), Type.DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Language language, Release release, Type type) {
		return getGraphFieldContainer(language, release, type, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, getProject().getLatestRelease().getUuid(), Type.DRAFT, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag, String releaseUuid, Type type) {
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
		draftEdge = getGraphFieldContainerEdge(languageTag, releaseUuid, Type.DRAFT);
		if (draftEdge != null) {
			previous = draftEdge.inV().has(NodeGraphFieldContainerImpl.class).nextOrDefault(NodeGraphFieldContainerImpl.class, null);
		}

		// create the new container
		NodeGraphFieldContainerImpl container = getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		if (original != null) {
			container.setEditor(original.getEditor());
			container.setLastEditedTimestamp(System.currentTimeMillis());
			container.setLanguage(language);
			container.setSchemaContainerVersion(original.getSchemaContainerVersion());
		} else {
			container.setEditor(user);
			container.setLastEditedTimestamp(System.currentTimeMillis());
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

		// create a new draft edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setReleaseUuid(releaseUuid);
		edge.setType(Type.DRAFT);

		// if there is no initial edge, create one
		if (getGraphFieldContainerEdge(languageTag, releaseUuid, Type.INITIAL) == null) {
			GraphFieldContainerEdge initialEdge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(languageTag);
			initialEdge.setReleaseUuid(releaseUuid);
			initialEdge.setType(Type.INITIAL);
		}

		return container;
	}

	/**
	 * Get an existing edge
	 * 
	 * @param languageTag
	 *            language tag
	 * @param releaseUuid
	 *            release uuid
	 * @param type
	 *            edge type
	 * @return existing edge or null
	 */
	protected EdgeFrame getGraphFieldContainerEdge(String languageTag, String releaseUuid, Type type) {
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.LANGUAGE_TAG_KEY, languageTag)
				.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		if (edgeTraversal.hasNext()) {
			return edgeTraversal.next();
		} else {
			return null;
		}
	}

	/**
	 * Get all graph field
	 * 
	 * @param releaseUuid
	 * @param type
	 * @return
	 */
	protected List<? extends EdgeFrame> getGraphFieldContainerEdges(String releaseUuid, Type type) {
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
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public List<? extends Node> getChildren(String releaseUuid) {
		return inE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).outV().has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public Node getParentNode(String releaseUuid) {
		return outE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).inV().has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
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
		Node node = BootstrapInitializer.getBoot().nodeRoot().create(creator, schemaVersion, project);
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
	public Observable<NodeResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		NodeParameters nodeParameters = new NodeParameters(ac);
		VersioningParameters versioiningParameters = ac.getVersioningParameters();

		// Increment level for each node transformation to avoid stackoverflow situations
		level = level + 1;
		try {
			Set<Observable<NodeResponse>> obs = new HashSet<>();
			NodeResponse restNode = new NodeResponse();
			SchemaContainer container = getSchemaContainer();
			if (container == null) {
				throw error(BAD_REQUEST, "The schema container for node {" + getUuid() + "} could not be found.");
			}
			Release release = ac.getRelease(getProject());

			// Parent node reference
			Node parentNode = getParentNode(release.getUuid());
			if (parentNode != null) {
				obs.add(parentNode.transformToReference(ac).map(transformedParentNode -> {
					restNode.setParentNode(transformedParentNode);
					return restNode;
				}));
			} else {
				// Only the base node of the project has no parent. Therefore this node must be a container.
				restNode.setContainer(true);
			}

			// Role permissions
			obs.add(setRolePermissions(ac, restNode));

			// Languages
			restNode.setAvailableLanguages(getAvailableLanguageNames(release, Type.forVersion(versioiningParameters.getVersion())));

			// Load the children information
			for (Node child : getChildren(release.getUuid())) {
				if (ac.getUser().hasPermissionSync(ac, child, READ_PERM)) {
					String schemaName = child.getSchemaContainer().getName();
					NodeChildrenInfo info = restNode.getChildrenInfo().get(schemaName);
					if (info == null) {
						info = new NodeChildrenInfo();
						String schemaUuid = child.getSchemaContainer().getUuid();
						info.setSchemaUuid(schemaUuid);
						info.setCount(1);
						restNode.getChildrenInfo().put(schemaName, info);
					} else {
						info.setCount(info.getCount() + 1);
					}
				}
			}

			// Fields
			NodeGraphFieldContainer fieldContainer = null;
			List<String> requestedLanguageTags = null;
			if (languageTags != null && languageTags.length > 0) {
				requestedLanguageTags = Arrays.asList(languageTags);
			} else {
				requestedLanguageTags = nodeParameters.getLanguageList();
			}
			fieldContainer = findNextMatchingFieldContainer(requestedLanguageTags, release.getUuid(), versioiningParameters.getVersion());
			if (fieldContainer == null) {
				// if a published version was requested, we check whether any published language variant exists for the node, if not, response with NOT_FOUND
				if (Type.forVersion(versioiningParameters.getVersion()) == Type.PUBLISHED
						&& getGraphFieldContainers(release, Type.PUBLISHED).isEmpty()) {
					log.error("Could not find field container for languages {" + requestedLanguageTags + "} and release {" + release.getUuid()
							+ "} and version params version {" + versioiningParameters.getVersion() + "}, release {"
							+ versioiningParameters.getRelease() + "}");
					//TODO the response should be specific.. add publish and release info
					throw error(NOT_FOUND, "object_not_found_for_uuid", getUuid());
				}

				// if a specific version was requested, that does not exist, we also return NOT_FOUND
				if (Type.forVersion(versioiningParameters.getVersion()) == Type.INITIAL) {
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
					// TODO throw error and log something
				}
				restNode.setEdited(fieldContainer.getLastEditedTimestamp() == null ? 0 : fieldContainer.getLastEditedTimestamp());

				// Fields
				for (FieldSchema fieldEntry : schema.getFields()) {
					// boolean expandField = fieldsToExpand.contains(fieldEntry.getName()) || ac.getExpandAllFlag();
					Observable<NodeResponse> obsFields = fieldContainer
							.getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, containerLanguageTags, level).map(restField -> {
								if (fieldEntry.isRequired() && restField == null) {
									// TODO i18n
									throw error(BAD_REQUEST, "The field {" + fieldEntry.getName()
											+ "} is a required field but it could not be found in the node. Please add the field using an update call or change the field schema and remove the required flag.");
								}
								if (restField == null) {
									log.info("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
								} else {
									restNode.getFields().put(fieldEntry.getName(), restField);
								}
								return restNode;

							});
					obs.add(obsFields);
				}

			}

			// Tags
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

			// Add common fields
			obs.add(fillCommonRestFields(ac, restNode));

			// breadcrumb
			obs.add(setBreadcrumbToRest(ac, restNode));

			// Add webroot url & lanuagePaths
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				String releaseUuid = ac.getRelease(null).getUuid();
				Type type = Type.forVersion(versioiningParameters.getVersion());

				// Url
				WebRootLinkReplacer linkReplacer = WebRootLinkReplacer.getInstance();
				String url = linkReplacer.resolve(releaseUuid, type, getUuid(), ac.getNodeParameters().getResolveLinks(), getProject().getName(),
						restNode.getLanguage()).toBlocking().single();
				restNode.setPath(url);

				// languagePaths
				Map<String, String> languagePaths = new HashMap<>();
				for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers(release,
						Type.forVersion(versioiningParameters.getVersion()))) {
					Language currLanguage = currentFieldContainer.getLanguage();
					languagePaths.put(currLanguage.getLanguageTag(),
							linkReplacer.resolve(releaseUuid, type, this, ac.getNodeParameters().getResolveLinks(), currLanguage.getLanguageTag())
									.toBlocking().single());
				}
				restNode.setLanguagePaths(languagePaths);
			}

			// Merge and complete
			return Observable.merge(obs).last();
		} catch (Exception e) {
			return Observable.error(e);
		}
	}

	@Override
	public Observable<NodeResponse> setBreadcrumbToRest(InternalActionContext ac, NodeResponse restNode) {
		String releaseUuid = ac.getRelease(getProject()).getUuid();
		Node current = this.getParentNode(releaseUuid);
		// The project basenode has no breadcrumb
		if (current == null) {
			return Observable.just(restNode);
		}

		List<NodeReferenceImpl> breadcrumb = new ArrayList<>();
		while (current != null) {
			// Don't add the base node to the breadcrumb
			// TODO should we add the basenode to the breadcrumb?
			if (current.getUuid().equals(this.getProject().getBaseNode().getUuid())) {
				break;
			}
			NodeReferenceImpl reference = new NodeReferenceImpl();
			reference.setUuid(current.getUuid());
			reference.setDisplayName(current.getDisplayName(ac));
			breadcrumb.add(reference);
			current = current.getParentNode(releaseUuid);
		}
		restNode.setBreadcrumb(breadcrumb);
		return Observable.just(restNode);
	}

	@Override
	public Observable<NavigationResponse> transformToNavigation(InternalActionContext ac) {
		NavigationParameters parameters = new NavigationParameters(ac);
		if (parameters.getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrxExperimental(() -> {
			// TODO assure that the schema version is correct
			if (!getSchemaContainer().getLatestVersion().getSchema().isContainer()) {
				throw error(BAD_REQUEST, "navigation_error_no_container");
			}
			NavigationResponse response = new NavigationResponse();
			return buildNavigationResponse(ac, this, parameters.getMaxDepth(), 0, response, response.getRoot(), ac.getRelease(getProject()).getUuid(),
					Type.forVersion(ac.getVersioningParameters().getVersion()));
		});
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
	 *            TODO
	 * @param type
	 *            TODO
	 * @return
	 */
	private Observable<NavigationResponse> buildNavigationResponse(InternalActionContext ac, Node node, int maxDepth, int level,
			NavigationResponse navigation, NavigationElement currentElement, String releaseUuid, Type type) {
		List<? extends Node> nodes = node.getChildren(ac.getUser(), releaseUuid, type);
		List<Observable<NavigationResponse>> obsResponses = new ArrayList<>();

		obsResponses.add(node.transformToRest(ac, 0).map(response -> {
			// Set current element data
			currentElement.setUuid(response.getUuid());
			currentElement.setNode(response);
			return navigation;
		}));

		// Abort recursion when we reach the max level or when no more children can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return Observable.merge(obsResponses).last();
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
		return Observable.merge(obsResponses).last();
	}

	@Override
	public Observable<NodeReferenceImpl> transformToReference(InternalActionContext ac) {
		NodeReferenceImpl nodeReference = new NodeReferenceImpl();
		nodeReference.setUuid(getUuid());
		nodeReference.setDisplayName(getDisplayName(ac));
		nodeReference.setSchema(getSchemaContainer().transformToReference());
		return Observable.just(nodeReference);
	}

	@Override
	public Observable<PublishStatusResponse> transformToPublishStatus(InternalActionContext ac) {
		Release release = ac.getRelease(getProject());
		PublishStatusResponse publishStatus = new PublishStatusResponse();
		Map<String, PublishStatusModel> languages = new HashMap<>();
		publishStatus.setAvailableLanguages(languages);

		getGraphFieldContainers(release, Type.PUBLISHED).stream().forEach(c -> {
			PublishStatusModel status = new PublishStatusModel().setPublished(true)
					.setVersion(new VersionReference(c.getUuid(), c.getVersion().toString())).setPublisher(c.getEditor().transformToReference())
					.setPublishTime(c.getLastEditedTimestamp());
			languages.put(c.getLanguage().getLanguageTag(), status);
		});

		getGraphFieldContainers(release, Type.DRAFT).stream().filter(c -> !languages.containsKey(c.getLanguage().getLanguageTag())).forEach(c -> {
			PublishStatusModel status = new PublishStatusModel().setPublished(false)
					.setVersion(new VersionReference(c.getUuid(), c.getVersion().toString()));
			languages.put(c.getLanguage().getLanguageTag(), status);
		});

		return Observable.just(publishStatus);
	}

	@Override
	public Observable<Void> publish(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		List<? extends NodeGraphFieldContainer> unpublishedContainers = getGraphFieldContainers(release, Type.DRAFT).stream()
				.filter(c -> !c.isPublished(releaseUuid)).collect(Collectors.toList());

		// TODO check whether all required fields are filled

		return db.trx(() -> {
			// publish all unpublished containers
			List<NodeGraphFieldContainer> published = unpublishedContainers.stream().map(c -> publish(c.getLanguage(), release, ac.getUser()))
					.collect(Collectors.toList());

			assertPublishConsistency(ac);
			return createIndexBatch(STORE_ACTION, published, releaseUuid, Type.PUBLISHED);
		}).process().map(i -> {
			return null;
		});
	}

	@Override
	public Observable<Void> takeOffline(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		TakeOfflineParameters parameters = ac.getTakeOfflineParameters();

		return db.trx(() -> {
			List<? extends NodeGraphFieldContainer> published = getGraphFieldContainers(release, Type.PUBLISHED);
			getGraphFieldContainerEdges(releaseUuid, Type.PUBLISHED).stream().forEach(EdgeFrame::remove);
			published.forEach(c -> c.getImpl().setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, null));

			if (parameters.isRecursive()) {
				for (Node node : getChildren()) {
					node.takeOffline(ac);
				}
			}

			assertPublishConsistency(ac);

			// reindex
			return createIndexBatch(DELETE_ACTION, published, releaseUuid, Type.PUBLISHED);
		}).process().map(i -> {
			return null;
		});
	}

	@Override
	public Observable<PublishStatusModel> transformToPublishStatus(InternalActionContext ac, String languageTag) {
		Release release = ac.getRelease(getProject());

		NodeGraphFieldContainer container = getGraphFieldContainer(languageTag, release.getUuid(), Type.PUBLISHED);
		if (container != null) {
			return Observable.just(new PublishStatusModel().setPublished(true)
					.setVersion(new VersionReference(container.getUuid(), container.getVersion().toString()))
					.setPublisher(container.getEditor().transformToReference()).setPublishTime(container.getLastEditedTimestamp()));
		} else {
			container = getGraphFieldContainer(languageTag, release.getUuid(), Type.DRAFT);
			if (container != null) {
				return Observable.just(new PublishStatusModel().setPublished(false)
						.setVersion(new VersionReference(container.getUuid(), container.getVersion().toString())));
			} else {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
		}
	}

	@Override
	public Observable<Void> publish(InternalActionContext ac, String languageTag) {
		Database db = MeshSpringConfiguration.getInstance().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		// get the draft version of the given language
		NodeGraphFieldContainer draftVersion = getGraphFieldContainer(languageTag, releaseUuid, Type.DRAFT);

		// if not existent -> NOT_FOUND
		if (draftVersion == null) {
			throw error(NOT_FOUND, "error_language_not_found", languageTag);
		}

		// if published -> done
		if (draftVersion.isPublished(releaseUuid)) {
			return Observable.just(null);
		}

		// check whether all required fields are filled, if not -> unable to publish
		// TODO

		return db.trx(() -> {
			NodeGraphFieldContainer published = publish(draftVersion.getLanguage(), release, ac.getUser());

			// reindex
			return createIndexBatch(STORE_ACTION, Arrays.asList(published), release.getUuid(), Type.PUBLISHED);
		}).process().map(i -> {
			return null;
		});
	}

	@Override
	public Observable<Void> takeOffline(InternalActionContext ac, String languageTag) {
		Database db = MeshSpringConfiguration.getInstance().database();
		Release release = ac.getRelease(getProject());
		String releaseUuid = release.getUuid();

		return db.trx(() -> {
			NodeGraphFieldContainer published = getGraphFieldContainer(languageTag, releaseUuid, Type.PUBLISHED);

			if (published == null) {
				throw error(NOT_FOUND, "error_language_not_found", languageTag);
			}
			// remove the "published" edge
			getGraphFieldContainerEdge(languageTag, releaseUuid, Type.PUBLISHED).remove();
			published.getImpl().setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, null);

			// reindex
			return createIndexBatch(DELETE_ACTION, Arrays.asList(published), releaseUuid, Type.PUBLISHED);
		}).process().map(i -> {
			return null;
		});
	}

	@Override
	public void setPublished(NodeGraphFieldContainer container, String releaseUuid) {
		String languageTag = container.getLanguage().getLanguageTag();

		// remove an existing published edge
		EdgeFrame currentPublished = getGraphFieldContainerEdge(languageTag, releaseUuid, Type.PUBLISHED);
		if (currentPublished != null) {
			currentPublished.inV().nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null).updateWebrootPathInfo(releaseUuid,
					"node_conflicting_segmentfield_publish");
			currentPublished.remove();
		}

		// create new published edge
		GraphFieldContainerEdge edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), GraphFieldContainerEdgeImpl.class);
		edge.setLanguageTag(languageTag);
		edge.setReleaseUuid(releaseUuid);
		edge.setType(Type.PUBLISHED);
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

		Type type = Type.forVersion(version);

		for (String languageTag : languageTags) {
			fieldContainer = getGraphFieldContainer(languageTag, releaseUuid, type);

			if (fieldContainer != null && type == Type.INITIAL) {
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
	public List<String> getAvailableLanguageNames(Release release, Type type) {
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
		getGraphFieldContainers(release, Type.DRAFT).forEach(container -> deleteLanguageContainer(release, container.getLanguage(), batch));
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
	private VertexTraversal<?, ?, ?> getChildrenTraversal(MeshAuthUser requestUser, String releaseUuid, Type type) {
		String permLabel = type == Type.PUBLISHED ? READ_PUBLISHED_PERM.label() : READ_PERM.label();

		VertexTraversal<?, ?, ?> traversal = null;
		if (releaseUuid != null) {
			traversal = inE(HAS_PARENT_NODE).has(RELEASE_UUID_KEY, releaseUuid).outV();
		} else {
			traversal = in(HAS_PARENT_NODE);
		}

		traversal = traversal.has(NodeImpl.class).mark().in(permLabel).out(HAS_ROLE).in(HAS_USER).retain(requestUser.getImpl()).back();
		if (releaseUuid != null && type != null) {
			traversal = traversal.mark().outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
					.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).outV().back();
		}
		return traversal;
	}

	@Override
	public List<? extends Node> getChildren(MeshAuthUser requestUser, String releaseUuid, Type type) {
		return getChildrenTraversal(requestUser, releaseUuid, type).toListExplicit(NodeImpl.class);
	}

	@Override
	public PageImpl<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, String releaseUuid, Type type,
			PagingParameters pagingInfo) throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = getChildrenTraversal(requestUser, releaseUuid, type);
		VertexTraversal<?, ?, ?> countTraversal = getChildrenTraversal(requestUser, releaseUuid, type);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
	}

	@Override
	public PageImpl<? extends Tag> getTags(Release release, PagingParameters params) throws InvalidArgumentException {
		// TODO add permissions
		VertexTraversal<?, ?, ?> traversal = TagEdgeImpl.getTagTraversal(this, release);
		VertexTraversal<?, ?, ?> countTraversal = TagEdgeImpl.getTagTraversal(this, release);
		return TraversalHelper.getPagedResult(traversal, countTraversal, params, TagImpl.class);
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
		NodeParameters parameters = ac.getNodeParameters();
		VersioningParameters versioningParameters = ac.getVersioningParameters();
		String displayFieldName = null;
		try {
			NodeGraphFieldContainer container = findNextMatchingFieldContainer(parameters.getLanguageList(), ac.getRelease(getProject()).getUuid(),
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
		} catch (Exception e) {
			log.error("Could not determine displayName for node {" + getUuid() + "} and fieldName {" + displayFieldName + "}", e);
			throw e;
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
	 * <li>Migration check - Nodes which have not yet migrated can't be updated</i>
	 * </ul>
	 * 
	 * 
	 * <p>
	 * Deduplication: Field values that have not been changed in between the request data and the last version will not cause new fields to be created in new
	 * version graph field containers. The new version graph field container will instead reference those fields from the previous graph field container
	 * version. Please note that this deduplication only applies to complex fields (e.g.: Lists, Micronode)
	 *
	 */
	@Override
	public Observable<? extends Node> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		return db.trx(() -> {
			NodeUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), NodeUpdateRequest.class);
			if (isEmpty(requestModel.getLanguage())) {
				throw error(BAD_REQUEST, "error_language_not_set");
			}
			Language language = BootstrapInitializer.getBoot().languageRoot().findByLanguageTag(requestModel.getLanguage());
			if (language == null) {
				throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
			}

			Release release = ac.getRelease(getProject());

			NodeGraphFieldContainer latestDraftVersion = getGraphFieldContainer(language, release, Type.DRAFT);

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

				// Create new field container as clone of the existing
				NodeGraphFieldContainer newDraftVersion = createGraphFieldContainer(language, release, ac.getUser(), latestDraftVersion);

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

				// Update the existing fields
				newDraftVersion.updateFieldsFromRest(ac, requestModel.getFields());
				latestDraftVersion = newDraftVersion;
			}
			return createIndexBatch(STORE_ACTION, Arrays.asList(latestDraftVersion), release.getUuid(), Type.DRAFT);
		}).process().map(i -> this);

	}

	@Override
	public Observable<Void> moveTo(InternalActionContext ac, Node targetNode) {
		Database db = MeshSpringConfiguration.getInstance().database();

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

		return db.trx(() -> {
			setParentNode(releaseUuid, targetNode);
			// update the webroot path info for every field container to ensure

			// Update published graph field containers
			getGraphFieldContainers(releaseUuid, Type.PUBLISHED).stream()
					.forEach(container -> container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_move"));

			// Update draft graph field containers
			getGraphFieldContainers(releaseUuid, Type.DRAFT).stream()
					.forEach(container -> container.updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_move"));

			SearchQueueBatch batch = createIndexBatch(STORE_ACTION);
			return batch;
		}).process().map(i -> {
			return null;
		});
	}

	@Override
	public void deleteLanguageContainer(Release release, Language language, SearchQueueBatch batch) {
		NodeGraphFieldContainer container = getGraphFieldContainer(language, release, Type.DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", language.getLanguageTag());
		}

		container.deleteFromRelease(release, batch);

		// if the published version is a different container, we remove this as well
		container = getGraphFieldContainer(language, release, Type.PUBLISHED);
		if (container != null) {
			container.deleteFromRelease(release, batch);
		}
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// batch.addEntry(getParentNode(), UPDATE_ACTION);
	}

	@Override
	public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		getProject().getReleaseRoot().findAll().forEach((release) -> {
			String releaseUuid = release.getUuid();
			for (Type type : Arrays.asList(Type.DRAFT, Type.PUBLISHED)) {
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
	public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action, List<? extends NodeGraphFieldContainer> containers, String releaseUuid,
			Type type) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		for (NodeGraphFieldContainer container : containers) {
			container.addIndexBatchEntry(batch, action, releaseUuid, type);
		}
		addRelatedEntries(batch, action);
		return batch;
	}

	@Override
	public PathSegment getSegment(String releaseUuid, Type type, String segment) {

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
	public Observable<Path> resolvePath(String releaseUuid, Type type, Path path, Stack<String> pathStack) {
		if (pathStack.isEmpty()) {
			return Observable.just(path);
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
}
