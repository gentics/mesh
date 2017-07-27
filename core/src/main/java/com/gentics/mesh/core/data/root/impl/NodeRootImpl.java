package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.page.impl.TransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see NodeRoot
 */
public class NodeRootImpl extends AbstractRootVertex<Node> implements NodeRoot {

	private static final Logger log = LoggerFactory.getLogger(NodeRootImpl.class);

	public static void init(Database database) {
		database.addVertexType(NodeRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_NODE, true, false, true);
	}

	@Override
	public Class<? extends Node> getPersistanceClass() {
		return NodeImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_NODE;
	}

	@Override
	public void addNode(Node node) {
		addItem(node);
	}

	@Override
	public void removeNode(Node node) {
		removeItem(node);
	}

	@Override
	public Page<? extends Node> findAll(MeshAuthUser requestUser, List<String> languageTags, PagingParameters pagingInfo)
			throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = requestUser.getPermTraversal(READ_PERM);
		Page<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	@Override
	public Page<? extends NodeGraphFieldContainer> findAllContents(InternalActionContext ac, PagingParameters pagingInfo) {

		VertexTraversal<?, ?, ?> traversal = out(getRootLabel()).filter(vertex -> {
			return ac.getUser().hasPermissionForId(vertex.getId(), READ_PERM);
		}).outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, ac.getRelease().getUuid()).filter(edge -> {
			// We only want to return published and draft container. No other versions or initial containers
			ContainerType type = ContainerType.get(edge.getProperty(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY));
			return (type.equals(DRAFT) || type.equals(PUBLISHED));
		}).inV();

		Page<? extends NodeGraphFieldContainer> containerPage = TraversalHelper.getPagedResult2(traversal, pagingInfo,
				NodeGraphFieldContainerImpl.class);
		return containerPage;
	}

	@Override
	public TransformablePage<? extends Node> findAll(InternalActionContext ac, PagingParameters pagingInfo) {

		int page = pagingInfo.getPage();
		int perPage = pagingInfo.getPerPage();

		if (page < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(page));
		}
		if (perPage < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(perPage));
		}

		MeshAuthUser requestUser = ac.getUser();
		Release release = ac.getRelease();
		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		GraphPermission perm = type == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;
		String releaseUuid = release.getUuid();
		FramedGraph graph = getGraph();

		// Internally we start with page 0 in order to comply with the tinkerpop range traversal values which start with 0.
		// External (for the enduser) all pages start with 1.
		page = page - 1;

		long low = page * perPage - 1;
		long upper = low + perPage;

		if (perPage == 0) {
			low = 0;
			upper = 0;
		}

		long count = 0;
		// Iterate over all found elements and frame them
		List<Node> elementsOfPage = new ArrayList<>();

		// NOT WORKING CURRENTLY:
		// Locate all in-bound vertex id's for item edges for this root vertex.
		// We use this method rather regular traversal in order to speedup the edge iteration.
		// Otherwise each edge would need to be loaded.
		// List<Object> inIds = db.edgeLookup(getRootLabel(), "inout", getId());
		Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_out", getId());
		for (Edge edge : edges) {
			Object id = edge.getVertex(Direction.IN).getId();
			// Check the permission
			if (!requestUser.hasPermissionForId(id, perm)) {
				continue;
			}
			// Check whether the node has content for our type and release otherwise exclude it
			if (!matchesReleaseAndType(id, releaseUuid, type.getCode())) {
				continue;
			}
			// Only add those vertices to the list which are within the bounds of the requested page
			if (count > low && count <= upper) {
				Vertex itemVertex = graph.getVertex(id);
				Node item = graph.frameElementExplicit(itemVertex, getPersistanceClass());
				// System.out.println("item" + item.getUuid());
				elementsOfPage.add(item);
			}
			count++;
		}

		// The totalPages of the list response must be zero if the perPage parameter is also zero.
		long totalPages = 0;
		if (perPage != 0) {
			totalPages = (long) Math.ceil(count / (double) (perPage));
		}
		// Internally the page size was reduced. We need to increment it now that we are finished.
		PageImpl<Node> resultPage = new PageImpl<Node>(elementsOfPage, count, ++page, totalPages, perPage);
		return new TransformablePageImpl<Node>(resultPage);

	}

	/**
	 * Check whether the node has a field for the release and given type.
	 * 
	 * @param nodeId
	 *            Object id of the node
	 * @param releaseUuid
	 * @param code
	 * @return
	 */
	private boolean matchesReleaseAndType(Object nodeId, String releaseUuid, String code) {
		FramedGraph graph = getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_field",
				database().createComposedIndexKey(nodeId, releaseUuid, code));
		return edges.iterator().hasNext();
	}

	@Override
	public Node loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		reload();
		Node element = findByUuid(uuid);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		MeshAuthUser requestUser = ac.getUser();
		if (perm == READ_PUBLISHED_PERM) {
			Release release = ac.getRelease(element.getProject());
			List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList();
			NodeGraphFieldContainer fieldContainer = element.findNextMatchingFieldContainer(requestedLanguageTags, release.getUuid(),
					ac.getVersioningParameters().getVersion());

			if (fieldContainer == null) {
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_release_language", uuid, String.join(",", requestedLanguageTags),
						release.getUuid());
			}
			// Additionally check whether the read published permission could grant read perm for published nodes
			if (fieldContainer.isPublished(release.getUuid()) && requestUser.hasPermission(element, READ_PUBLISHED_PERM)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", uuid);
			}
		} else if (requestUser.hasPermission(element, perm)) {
			return element;
		}
		throw error(FORBIDDEN, "error_missing_perm", uuid);
	}

	/**
	 * Get the vertex traversal that finds all nodes visible to the user
	 * 
	 * @param requestUser
	 *            user
	 * @param release
	 *            release
	 * @param type
	 *            type
	 * @param permission
	 *            permission to filter by
	 * @return vertex traversal
	 */
	protected VertexTraversal<?, ?, ?> getAllTraversal(MeshAuthUser requestUser, Release release, ContainerType type, GraphPermission permission) {
		return out(getRootLabel()).filter(vertex -> {
			return requestUser.hasPermissionForId(vertex.getId(), permission);
		}).mark().outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, release.getUuid())
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).outV().back();
	}

	@Override
	public Node create(User creator, SchemaContainerVersion version, Project project, String uuid) {
		// TODO check whether the mesh node is in fact a folder node.
		NodeImpl node = getGraph().addFramedVertex(NodeImpl.class);
		if (uuid != null) {
			node.setUuid(uuid);
		}
		node.setSchemaContainer(version.getSchemaContainer());

		// TODO is this a duplicate? - Maybe we should only store the project assignment in one way?
		project.getNodeRoot().addNode(node);
		node.setProject(project);
		node.setCreator(creator);
		node.setCreationTimestamp();

		addNode(node);
		return node;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO maybe add a check to prevent deletion of meshRoot.nodeRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting node root {" + getUuid() + "}");
		}
		// Delete all containers of all nodes
		for (Node node : findAll()) {
			for (NodeGraphFieldContainer container : node.getAllInitialGraphFieldContainers()) {
				container.delete(batch);
			}
			// Finally remove the node element itself
			node.getElement().remove();
		}
		// All nodes are gone. Lets remove the node root element.
		getElement().remove();
	}

	/**
	 * Create a new node using the specified schema container.
	 * 
	 * @param ac
	 * @param schemaContainer
	 * @param batch
	 * @param uuid
	 * @return
	 */
	// TODO use schema container version instead of container
	private Node createNode(InternalActionContext ac, SchemaContainer schemaContainer, SearchQueueBatch batch, String uuid) {
		Project project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		BootstrapInitializer boot = MeshInternal.get().boot();

		String body = ac.getBodyAsString();
		NodeCreateRequest requestModel = JsonUtil.readValue(body, NodeCreateRequest.class);
		if (requestModel.getParentNode() == null || isEmpty(requestModel.getParentNode().getUuid())) {
			throw error(BAD_REQUEST, "node_missing_parentnode_field");
		}
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "node_no_languagecode_specified");
		}

		// Load the parent node in order to create the node
		Node parentNode = project.getNodeRoot().loadObjectByUuid(ac, requestModel.getParentNode().getUuid(), CREATE_PERM);
		Release release = ac.getRelease();
		Node node = parentNode.create(requestUser, schemaContainer.getLatestVersion(), project, release, uuid);

		// Add initial permissions to the created node
		requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
		requestUser.addPermissionsOnRole(parentNode, READ_PUBLISHED_PERM, node, READ_PUBLISHED_PERM);
		requestUser.addPermissionsOnRole(parentNode, PUBLISH_PERM, node, PUBLISH_PERM);

		// Create the language specific graph field container for the node
		Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
		if (language == null) {
			throw error(BAD_REQUEST, "language_not_found", requestModel.getLanguage());
		}
		NodeGraphFieldContainer container = node.createGraphFieldContainer(language, release, requestUser);
		container.updateFieldsFromRest(ac, requestModel.getFields());
		batch.store(node, release.getUuid(), ContainerType.DRAFT, true);
		return node;
	}

	@Override
	public Node create(InternalActionContext ac, SearchQueueBatch batch, String uuid) {

		// Override any given version parameter. Creation is always scoped to drafts
		ac.getVersioningParameters().setVersion("draft");

		Project project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();

		String body = ac.getBodyAsString();

		// 1. Extract the schema information from the given JSON
		SchemaReferenceInfo schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
		boolean missingSchemaInfo = schemaInfo.getSchema() == null
				|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid()) && StringUtils.isEmpty(schemaInfo.getSchema().getName()));
		if (missingSchemaInfo) {
			throw error(BAD_REQUEST, "error_schema_parameter_missing");
		}

		// TODO use fromReference call to load the schema container

		if (!isEmpty(schemaInfo.getSchema().getUuid())) {
			// 2. Use schema reference by uuid first
			SchemaContainer schemaContainer = project.getSchemaContainerRoot().loadObjectByUuid(ac, schemaInfo.getSchema().getUuid(), READ_PERM);
			return createNode(ac, schemaContainer, batch, uuid);
		}

		// TODO handle schema version as well? Decide whether it should be possible to create a node and specify the schema version.
		// 3. Or just schema reference by name
		if (!isEmpty(schemaInfo.getSchema().getName())) {
			SchemaContainer containerByName = project.getSchemaContainerRoot().findByName(schemaInfo.getSchema().getName());
			if (containerByName != null) {
				String schemaName = containerByName.getName();
				String schemaUuid = containerByName.getUuid();
				if (requestUser.hasPermission(containerByName, GraphPermission.READ_PERM)) {
					return createNode(ac, containerByName, batch, uuid);
				} else {
					throw error(FORBIDDEN, "error_missing_perm", schemaUuid + "/" + schemaName);
				}

			} else {
				throw error(NOT_FOUND, "schema_not_found", schemaInfo.getSchema().getName());
			}
		} else {
			throw error(BAD_REQUEST, "error_schema_parameter_missing");
		}
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Node node : findAll()) {
				// We don't need to recursively handle the permissions for each node again since this call will already affect all nodes.
				node.applyPermissions(role, false, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

}
