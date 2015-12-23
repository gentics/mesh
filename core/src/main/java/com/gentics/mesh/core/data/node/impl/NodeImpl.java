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
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.errorObservable;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeBreadcrumbResponse;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.TraversalHelper;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

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
		NodeGraphFieldContainer container = findNextMatchingFieldContainer(ac);
		if (container != null) {
			String fieldName = getSchema().getSegmentField();
			StringGraphField field = container.getString(fieldName);
			if (field != null) {
				return Observable.just(field.getString());
			}
		}
		throw error(BAD_REQUEST, "node_error_could_not_find_path_segment", getUuid());
	}

	@Override
	public Observable<String> getPathSegment(Language language) {
		NodeGraphFieldContainer container = getGraphFieldContainer(language);
		if (container != null) {
			String fieldName = getSchema().getSegmentField();
			StringGraphField field = container.getString(fieldName);
			if (field != null) {
				return Observable.just(field.getString());
			}
		}
		throw error(BAD_REQUEST, "node_error_could_not_find_path_segment", getUuid());
	}

	@Override
	public Observable<String> getPath(Language language) throws UnsupportedEncodingException {
		List<Observable<String>> segments = new ArrayList<>();

		segments.add(getPathSegment(language));
		Node current = this;
		while (current != null) {
			current = current.getParentNode();
			if (current == null || current.getParentNode() == null) {
				break;
			}
			segments.add(current.getPathSegment(language));
		}

		Collections.reverse(segments);
		return RxUtil.concatList(segments).reduce((a, b) -> {
			try {
				String aEnc = URLEncoder.encode(a, "UTF-8");
				String bEnc = URLEncoder.encode(b, "UTF-8");
				return "/" + aEnc + "/" + bEnc;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
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

	public NodeGraphFieldContainer getOrCreateGraphFieldContainer(Language language) {
		return getOrCreateGraphFieldContainer(language, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public void addTag(Tag tag) {
		setLinkOutTo(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void removeTag(Tag tag) {
		unlinkOut(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void createLink(Node to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
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
	public Schema getSchema() {
		return getSchemaContainer().getSchema();
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
	public Observable<NodeBreadcrumbResponse> transformToBreadcrumb(InternalActionContext ac) {
		return Observable.just(new NodeBreadcrumbResponse());
	}

	@Override
	public Observable<NodeResponse> transformToRest(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		return db.asyncNoTrx(() -> {
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
			if (schema.isFolder()) {
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

			NodeGraphFieldContainer fieldContainer = findNextMatchingFieldContainer(ac);
			restNode.setAvailableLanguages(getAvailableLanguageNames());

			// Fields
			if (fieldContainer == null) {
				List<String> languageTags = ac.getSelectedLanguageTags();
				String langInfo = getLanguageInfo(languageTags);
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
				for (FieldSchema fieldEntry : schema.getFields()) {
					boolean expandField = fieldsToExpand.contains(fieldEntry.getName()) || ac.getExpandAllFlag();
					Observable<NodeResponse> obsFields = fieldContainer.getRestFieldFromGraph(ac, fieldEntry.getName(), fieldEntry, expandField)
							.map(restField -> {
						if (fieldEntry.isRequired() && restField == null) {
							/* TODO i18n */
							// TODO no trx fail. Instead let obsRestField fail
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

			// Add webroot url
			if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
				// TODO what about the language?
				restNode.setUrl(WebRootLinkReplacer.getInstance().resolve(getUuid(), null, ac.getResolveLinksType()).toBlocking().first());
			}

			// Merge and complete
			return Observable.merge(obs).toBlocking().first();

		});
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
	public NodeGraphFieldContainer findNextMatchingFieldContainer(InternalActionContext ac) {
		NodeGraphFieldContainer fieldContainer = null;
		List<String> languageTags = ac.getSelectedLanguageTags();
		for (String languageTag : languageTags) {
			Language language = MeshRootImpl.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
			if (language == null) {
				// MeshRootImpl.getInstance().getLanguageRoot().reload();
				// Language lan =MeshRootImpl.getInstance().getLanguageRoot().findByLanguageTag("en");
				// System.out.println(lan);
				throw new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("error_language_not_found", languageTag));
			}
			fieldContainer = getGraphFieldContainer(language);
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
	public void delete() {
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
	public Page<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingParameter pagingInfo)
			throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = in(HAS_PARENT_NODE).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_PARENT_NODE).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
	}

	@Override
	public Page<? extends Tag> getTags(InternalActionContext ac) throws InvalidArgumentException {
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
			NodeGraphFieldContainer container = findNextMatchingFieldContainer(ac);
			if (container == null) {
				if (log.isDebugEnabled()) {
					log.debug("Could not find any matching i18n field container for node {" + getUuid() + "}.");
				}
			} else {
				// Determine the display field name and load the string value from that field.
				return container.getDisplayFieldValue(getSchema());
			}
		} catch (Exception e) {
			log.error("Could not determine displayName for node {" + getUuid() + "} and fieldName {" + displayFieldName + "}", e);
		}
		return null;
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
			NodeUpdateRequest requestModel = JsonUtil.readNode(ac.getBodyAsString(), NodeUpdateRequest.class, ServerSchemaStorage.getSchemaStorage());
			if (StringUtils.isEmpty(requestModel.getLanguage())) {
				return errorObservable(BAD_REQUEST, "error_language_not_set");
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
				try {
					Schema schema = getSchema();
					container.updateFieldsFromRest(ac, requestModel.getFields(), schema);
				} catch (MeshSchemaException e) {
					// TODO i18n
					throw error(BAD_REQUEST, "node_update_failed", e);
				}
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

		try {
			if (!targetNode.getSchema().isFolder()) {
				throw error(BAD_REQUEST, "node_move_error_targetnode_is_no_folder");
			}
		} catch (Exception e) {
			log.error("Could not load schema for target node during move action", e);
			// TODO maybe add better i18n error
			throw error(BAD_REQUEST, "error");
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
		String indexType = getType() + "-" + languageTag;
		batch.addEntry(getUuid(), getType(), action, indexType);
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
		for (NodeGraphFieldContainer container : getGraphFieldContainers()) {
			String indexType = getType() + "-" + container.getLanguage().getLanguageTag();
			batch.addEntry(getUuid(), getType(), action, indexType);
		}
		addRelatedEntries(batch, action);
		return batch;
	}

	@Override
	public PathSegment getSegment(String segment) {
		Schema schema = getSchema();

		// Check the different language versions
		String segmentFieldName = schema.getSegmentField();
		for (GraphFieldContainer container : getGraphFieldContainers()) {
			// First check whether a string field exists for the given name
			StringGraphField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegment(this, field, container.getLanguage());
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
					return new PathSegment(this, binaryField, container.getLanguage());
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
		return errorObservable(NOT_FOUND, "node_not_found_for_path", path.getTargetPath());

	}
}
