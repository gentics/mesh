package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.IOException;
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
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.VersionNumber;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.TraversalHelper;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see Node
 */
public class NodeImpl extends AbstractGenericFieldContainerVertex<NodeResponse, Node> implements Node {

	private static final String PUBLISHED_PROPERTY_KEY = "published";

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
		NodeGraphFieldContainer container = findNextMatchingFieldContainer(ac.getSelectedLanguageTags());
		if (container != null) {
			String fieldName = getSchemaContainer().getSchema().getSegmentField();
			StringGraphField field = container.getString(fieldName);
			if (field != null) {
				return Observable.just(field.getString());
			}
		}
		return Observable.error(error(BAD_REQUEST, "node_error_could_not_find_path_segment", getUuid()));
	}

	@Override
	public Observable<String> getPathSegment(String... languageTag) {
		NodeGraphFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getGraphFieldContainer(tag)) != null) {
				break;
			}
		}
		if (container != null) {
			String fieldName = getSchemaContainer().getSchema().getSegmentField();
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
	public Observable<String> getPath(String... languageTag) throws UnsupportedEncodingException {
		List<Observable<String>> segments = new ArrayList<>();

		segments.add(getPathSegment(languageTag));
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
			current = current.getParentNode();
			if (current == null || current.getParentNode() == null) {
				break;
			}
			// for the path segments of the container, we allow ANY language (of the project)
			segments.add(current.getPathSegment(projectLanguages));
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
	public Observable<String> getPath(InternalActionContext ac) {
		List<Observable<String>> segments = new ArrayList<>();
		segments.add(getPathSegment(ac));
		Node current = this;
		while (current != null) {
			current = current.getParentNode();
			if (current == null || current.getParentNode() == null) {
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
	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getGraphFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(NodeGraphFieldContainerImpl.class).toListExplicit(NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(Language language) {
		return getGraphFieldContainer(language, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public NodeGraphFieldContainer getGraphFieldContainer(String languageTag) {
		return getGraphFieldContainer(languageTag, NodeGraphFieldContainerImpl.class);
	}

	public NodeGraphFieldContainer getOrCreateGraphFieldContainer(Language language) {
		NodeGraphFieldContainer container = getOrCreateGraphFieldContainer(language, NodeGraphFieldContainerImpl.class);
		// set the initial version number, if no version number set
		if (container.getVersion() == null) {
			container.setVersion(new VersionNumber());
		}
		return container;
	}

	@Override
	public void addTag(Tag tag) {
		setUniqueLinkOutTo(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void removeTag(Tag tag) {
		unlinkOut(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void setSchemaContainer(SchemaContainer schema) {
		setLinkOut(schema.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	/**
	 * @deprecated Load the schema container from the {@link GraphFieldContainer} instance.
	 */
	@Override
	@Deprecated
	public SchemaContainer getSchemaContainer() {
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public Node getParentNode() {
		return out(HAS_PARENT_NODE).has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public void setParentNode(Node parent) {
		setLinkOut(parent.getImpl(), HAS_PARENT_NODE);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void setProject(Project project) {
		setLinkOut(project.getImpl(), ASSIGNED_TO_PROJECT);
	}

	/**
	 * Create a new node and make sure to delegate the creation request to the main node root aggregation node.
	 */
	@Override
	public Node create(User creator, SchemaContainer schemaContainer, Project project) {
		// We need to use the (meshRoot)--(nodeRoot) node instead of the (project)--(nodeRoot) node.
		Node node = BootstrapInitializer.getBoot().nodeRoot().create(creator, schemaContainer, project);
		node.setParentNode(this);
		setCreated(creator);
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
	public Observable<NodeResponse> transformToRestSync(InternalActionContext ac, String... languageTags) {
		try {
			Set<Observable<NodeResponse>> obs = new HashSet<>();
			NodeResponse restNode = new NodeResponse();
			SchemaContainer container = getSchemaContainer();
			if (container == null) {
				throw error(BAD_REQUEST, "The schema container for node {" + getUuid() + "} could not be found.");
			}

			Schema schema = container.getSchema();
			if (schema == null) {
				throw error(BAD_REQUEST, "The schema for node {" + getUuid() + "} could not be found.");
			}
			restNode.setDisplayField(schema.getDisplayField());
			restNode.setPublished(isPublished());

			// Load the children information
			if (schema.isContainer()) {
				for (Node child : getChildren()) {
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
				restNode.setContainer(true);

			}

			// Schema reference
			SchemaContainer schemaContainer = getSchemaContainer();
			if (schemaContainer != null) {
				restNode.setSchema(schemaContainer.transformToReference(ac));
			}

			// Parent node reference
			Node parentNode = getParentNode();
			if (parentNode != null) {
				obs.add(parentNode.transformToReference(ac).map(transformedParentNode -> {
					restNode.setParentNode(transformedParentNode);
					return restNode;
				}));
			}

			// Role permissions
			obs.add(setRolePermissions(ac, restNode));

			// Languages
			restNode.setAvailableLanguages(getAvailableLanguageNames());

			// Fields
			NodeGraphFieldContainer fieldContainer = null;
			List<String> requestedLanguageTags = null;
			if (languageTags != null && languageTags.length > 0) {
				requestedLanguageTags = Arrays.asList(languageTags);
			} else {
				requestedLanguageTags = ac.getSelectedLanguageTags();
			}
			fieldContainer = findNextMatchingFieldContainer(requestedLanguageTags);
			if (fieldContainer == null) {
				String langInfo = getLanguageInfo(requestedLanguageTags);
				if (log.isDebugEnabled()) {
					log.debug("The fields for node {" + getUuid() + "} can't be populated since the node has no matching language for the languages {"
							+ langInfo + "}. Fields will be empty.");
				}

				// TODO The base node has no fields. We need to take care of that edgecase first
				// noTrx.fail(error(ac, NOT_FOUND, "node_no_language_found", langInfo));
				// return;
			} else {
				restNode.setLanguage(fieldContainer.getLanguage().getLanguageTag());

				List<String> fieldsToExpand = ac.getExpandedFieldnames();

				// modify the language fallback list by moving the container's language to the front
				List<String> containerLanguageTags = new ArrayList<>(requestedLanguageTags);
				containerLanguageTags.remove(restNode.getLanguage());
				containerLanguageTags.add(0, restNode.getLanguage());

				for (FieldSchema fieldEntry : schema.getFields()) {
					//					boolean expandField = fieldsToExpand.contains(fieldEntry.getName()) || ac.getExpandAllFlag();
					Observable<NodeResponse> obsFields = fieldContainer
							.getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, containerLanguageTags).map(restField -> {
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
			for (Tag tag : getTags(ac)) {
				TagFamily tagFamily = tag.getTagFamily();
				String tagFamilyName = tagFamily.getName();
				String tagFamilyUuid = tagFamily.getUuid();
				TagReference reference = tag.transformToReference(ac);
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
			if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {

				// Url
				WebRootLinkReplacer linkReplacer = WebRootLinkReplacer.getInstance();
				String url = linkReplacer.resolve(getUuid(), ac.getResolveLinksType(), getProject().getName(), restNode.getLanguage()).toBlocking()
						.single();
				restNode.setPath(url);

				// languagePaths
				Map<String, String> languagePaths = new HashMap<>();
				for (GraphFieldContainer currentFieldContainer : getGraphFieldContainers()) {
					Language currLanguage = currentFieldContainer.getLanguage();
					languagePaths.put(currLanguage.getLanguageTag(),
							linkReplacer.resolve(this, ac.getResolveLinksType(), currLanguage.getLanguageTag()).toBlocking().single());
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
		Node current = this.getParentNode();
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
			current = current.getParentNode();
		}
		restNode.setBreadcrumb(breadcrumb);
		return Observable.just(restNode);
	}

	@Override
	public Observable<NavigationResponse> transformToNavigation(InternalActionContext ac) {
		if (ac.getNavigationRequestParameter().getMaxDepth() < 0) {
			throw error(BAD_REQUEST, "navigation_error_invalid_max_depth");
		}
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrxExperimental(() -> {
			if (!getSchemaContainer().getSchema().isContainer()) {
				throw error(BAD_REQUEST, "navigation_error_no_container");
			}
			NavigationResponse response = new NavigationResponse();
			return buildNavigationResponse(ac, this, ac.getNavigationRequestParameter().getMaxDepth(), 0, response, response.getRoot());
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
	 * @return
	 */
	private Observable<NavigationResponse> buildNavigationResponse(InternalActionContext ac, Node node, int maxDepth, int level,
			NavigationResponse navigation, NavigationElement currentElement) {
		List<? extends Node> nodes = node.getChildren(ac.getUser());
		List<Observable<NavigationResponse>> obsResponses = new ArrayList<>();

		obsResponses.add(node.transformToRest(ac).map(response -> {
			// Set current element data
			currentElement.setUuid(response.getUuid());
			currentElement.setNode(response);
			return navigation;
		}));

		// Abort recursion when we reach the max level or when no more children can be found.
		if (level == maxDepth || nodes.isEmpty()) {
			return Observable.merge(obsResponses).last();
		}

		// Add children
		for (Node child : nodes) {
			if (child.getSchemaContainer().getSchema().isContainer()) {
				NavigationElement childElement = new NavigationElement();
				// We found at least one child so lets create the array
				if (currentElement.getChildren() == null) {
					currentElement.setChildren(new ArrayList<>());
				}
				currentElement.getChildren().add(childElement);
				obsResponses.add(buildNavigationResponse(ac, child, maxDepth, level + 1, navigation, childElement));
			}
		}
		return Observable.merge(obsResponses).last();
	}

	@Override
	public Observable<NodeReferenceImpl> transformToReference(InternalActionContext ac) {
		NodeReferenceImpl nodeReference = new NodeReferenceImpl();
		nodeReference.setUuid(getUuid());
		nodeReference.setDisplayName(getDisplayName(ac));
		nodeReference.setSchema(getSchemaContainer().transformToReference(ac));
		return Observable.just(nodeReference);
	}

	@Override
	public NodeGraphFieldContainer findNextMatchingFieldContainer(List<String> languageTags) {
		NodeGraphFieldContainer fieldContainer = null;

		for (String languageTag : languageTags) {
			fieldContainer = getGraphFieldContainer(languageTag);
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
	public void delete(boolean ignoreChecks) {
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
			child.delete();
		}
		for (NodeGraphFieldContainer container : getGraphFieldContainers()) {
			container.delete();
		}
		getElement().remove();

	}

	@Override
	public void delete() {
		delete(false);
	}

	/**
	 * Get a vertex traversal to find the children of this node, this user has read permission for
	 *
	 * @param requestUser user
	 * @return vertex traversal
	 */
	private VertexTraversal<?, ?, ?> getChildrenTraversal(MeshAuthUser requestUser) {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
	}

	@Override
	public List<? extends Node> getChildren(MeshAuthUser requestUser) {
		return getChildrenTraversal(requestUser).toListExplicit(NodeImpl.class);
	}

	@Override
	public PageImpl<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingParameter pagingInfo)
			throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = getChildrenTraversal(requestUser);
		VertexTraversal<?, ?, ?> countTraversal = getChildrenTraversal(requestUser);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
	}

	@Override
	public PageImpl<? extends Tag> getTags(InternalActionContext ac) throws InvalidArgumentException {
		// TODO add permissions
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, ac.getPagingParameter(), TagImpl.class);
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Node child : getChildren()) {
				child.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public String getDisplayName(InternalActionContext ac) {
		String displayFieldName = null;
		try {
			NodeGraphFieldContainer container = findNextMatchingFieldContainer(ac.getSelectedLanguageTags());
			if (container == null) {
				if (log.isDebugEnabled()) {
					log.debug("Could not find any matching i18n field container for node {" + getUuid() + "}.");
				}
				return null;
			} else {
				// Determine the display field name and load the string value from that field.
				return container.getDisplayFieldValue(getSchemaContainer().getSchema());
			}
		} catch (Exception e) {
			log.error("Could not determine displayName for node {" + getUuid() + "} and fieldName {" + displayFieldName + "}", e);
			throw e;
		}
	}

	@Override
	public void setPublished(boolean published) {
		setProperty(PUBLISHED_PROPERTY_KEY, String.valueOf(published));
	}

	@Override
	public boolean isPublished() {
		String fieldValue = getProperty(PUBLISHED_PROPERTY_KEY);
		return Boolean.valueOf(fieldValue);
	}

	@Override
	public Observable<? extends Node> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		try {
			NodeUpdateRequest requestModel = JsonUtil.readNode(ac.getBodyAsString(), NodeUpdateRequest.class, ServerSchemaStorage.getInstance());
			if (StringUtils.isEmpty(requestModel.getLanguage())) {
				throw error(BAD_REQUEST, "error_language_not_set");
			}
			return db.trx(() -> {
				Language language = BootstrapInitializer.getBoot().languageRoot().findByLanguageTag(requestModel.getLanguage());
				if (language == null) {
					throw error(BAD_REQUEST, "error_language_not_found", requestModel.getLanguage());
				}

				/* TODO handle other fields, etc. */
				setPublished(requestModel.isPublished());
				setEditor(ac.getUser());
				setLastEditedTimestamp(System.currentTimeMillis());
				NodeGraphFieldContainer container = getOrCreateGraphFieldContainer(language);
				Schema schema = getSchemaContainer().getSchema();
				container.updateFieldsFromRest(ac, requestModel.getFields(), schema);
				return addIndexBatch(UPDATE_ACTION);
			}).process().map(i -> this);

		} catch (IOException e1) {
			log.error(e1);
			return Observable.error(error(BAD_REQUEST, e1.getMessage(), e1));
		}
	}

	@Override
	public Observable<Void> moveTo(InternalActionContext ac, Node targetNode) {
		Database db = MeshSpringConfiguration.getInstance().database();

		// TODO should we add a guard that terminates this loop when it runs to long?
		// Check whether the target node is part of the subtree of the source node.
		Node parent = targetNode.getParentNode();
		while (parent != null) {
			if (parent.getUuid().equals(getUuid())) {
				throw error(BAD_REQUEST, "node_move_error_not_allowd_to_move_node_into_one_of_its_children");
			}
			parent = parent.getParentNode();
		}

		if (!targetNode.getSchemaContainer().getSchema().isContainer()) {
			throw error(BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
		}

		if (getUuid().equals(targetNode.getUuid())) {
			throw error(BAD_REQUEST, "node_move_error_same_nodes");
		}

		// TODO check whether there is a node in the target node that has the same name. We do this to prevent issues for the webroot api
		return db.trx(() -> {
			setParentNode(targetNode);
			setEditor(ac.getUser());
			setLastEditedTimestamp(System.currentTimeMillis());
			targetNode.setEditor(ac.getUser());
			targetNode.setLastEditedTimestamp(System.currentTimeMillis());
			// update the webroot path info for every field container.
			getGraphFieldContainers().stream().forEach(container -> container.updateWebrootPathInfo("node_conflicting_segmentfield_move"));
			SearchQueueBatch batch = addIndexBatch(SearchQueueEntryAction.UPDATE_ACTION);
			return batch;
		}).process().map(i -> {
			return null;
		});
	}

	@Override
	public Observable<? extends Node> deleteLanguageContainer(InternalActionContext ac, Language language) {
		return ac.getDatabase().trx(() -> {
			NodeGraphFieldContainer container = getGraphFieldContainer(language);
			if (container == null) {
				throw error(NOT_FOUND, "node_no_language_found", language.getLanguageTag());
			}
			container.delete();
			return addIndexBatch(SearchQueueEntryAction.DELETE_ACTION, language.getLanguageTag());
		}).process().map(i -> this);
	}

	private SearchQueueBatch addIndexBatch(SearchQueueEntryAction action, String languageTag) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		String indexType = NodeIndexHandler.getDocumentType(getSchemaContainer().getSchema());
		batch.addEntry(NodeIndexHandler.composeDocumentId(this, languageTag), getType(), action, indexType);
		addRelatedEntries(batch, action);
		return batch;
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// batch.addEntry(getParentNode(), UPDATE_ACTION);
	}

	@Override
	public SearchQueueBatch addIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		// TODO is this a bug? should we not add the document id (uuid+lang) to the entry?
		for (NodeGraphFieldContainer container : getGraphFieldContainers()) {
			String indexType = NodeIndexHandler.getDocumentType(getSchemaContainer().getSchema());
			batch.addEntry(getUuid(), getType(), action, indexType);
		}
		addRelatedEntries(batch, action);
		return batch;
	}

	@Override
	public PathSegment getSegment(String segment) {
		Schema schema = getSchemaContainer().getSchema();

		// Check the different language versions
		String segmentFieldName = schema.getSegmentField();
		for (GraphFieldContainer container : getGraphFieldContainers()) {
			// First check whether a string field exists for the given name
			StringGraphField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegment(this, field, container.getLanguage().getLanguageTag());
				}
			}

			// No luck yet - lets check whether a binary field matches the segmentField
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
	public Observable<Path> resolvePath(Path path, Stack<String> pathStack) {
		if (pathStack.isEmpty()) {
			return Observable.just(path);
		}
		String segment = pathStack.pop();

		if (log.isDebugEnabled()) {
			log.debug("Resolving for path segment {" + segment + "}");
		}

		// Check all childnodes
		for (Node childNode : getChildren()) {
			PathSegment pathSegment = childNode.getSegment(segment);
			if (pathSegment != null) {
				path.addSegment(pathSegment);
				return childNode.resolvePath(path, pathStack);
			}
		}
		throw error(NOT_FOUND, "node_not_found_for_path", path.getTargetPath());

	}
}
