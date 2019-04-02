package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

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
	public Page<? extends Node> findAll(MeshAuthUser user, List<String> languageTags, PagingParameters pagingInfo)
			throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = user.getPermTraversal(READ_PERM);
		return new DynamicTransformablePageImpl<Node>(user, traversal, pagingInfo, READ_PERM, NodeImpl.class);
	}

	@Override
	public TransformablePage<? extends Node> findAll(InternalActionContext ac, PagingParameters pagingInfo) {

		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		GraphPermission perm = type == ContainerType.PUBLISHED ? READ_PUBLISHED_PERM : READ_PERM;

		Branch branch = ac.getBranch();
		String branchUuid = branch.getUuid();

		return new DynamicTransformablePageImpl<>(ac.getUser(), this, pagingInfo, perm, (item) -> {
			return matchesBranchAndType(item.id(), branchUuid, type.getCode());
		}, true);
	}

	/**
	 * Check whether the node has a field for the branch and given type.
	 * 
	 * @param nodeId     Object id of the node
	 * @param branchUuid
	 * @param code
	 * @return
	 */
	private boolean matchesBranchAndType(Object nodeId, String branchUuid, String code) {
		FramedGraph graph = getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + HAS_FIELD_CONTAINER.toLowerCase() + "_field",
				database().createComposedIndexKey(nodeId, branchUuid, code));
		return edges.iterator().hasNext();
	}

	@Override
	public Stream<? extends Node> findAllStream(InternalActionContext ac, GraphPermission permission) {
		MeshAuthUser user = ac.getUser();
		FramedTransactionalGraph graph = Tx.getActive().getGraph();

		Branch branch = ac.getBranch();
		String branchUuid = branch.getUuid();

		String idx = "e." + getRootLabel().toLowerCase() + "_out";
		Spliterator<Edge> itemEdges = graph.getEdges(idx.toLowerCase(), id()).spliterator();
		return StreamSupport.stream(itemEdges, false)
			.map(edge -> edge.getVertex(Direction.IN))
			.filter(item -> {
				// Check whether the node has at least a draft in the selected branch - Otherwise the node should be skipped
				return matchesBranchAndType(item.getId(), branchUuid, DRAFT.getCode());
			})
			.filter(item -> {
				boolean hasRead = user.hasPermissionForId(item.getId(), READ_PERM);
				if (hasRead) {
					return true;
				} else {
					// Check whether the node is published. In this case we need to check the read publish perm.
					boolean isPublishedForBranch = matchesBranchAndType(item.getId(), branchUuid, PUBLISHED.getCode());
					if (isPublishedForBranch) {
						return user.hasPermissionForId(item.getId(), READ_PUBLISHED_PERM);
					}
				}
				return false;
			})
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	@Override
	public Node loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		Node element = findByUuid(uuid);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		MeshAuthUser requestUser = ac.getUser();
		if (perm == READ_PUBLISHED_PERM) {
			Branch branch = ac.getBranch(element.getProject());

			List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList();
			NodeGraphFieldContainer fieldContainer = element.findVersion(requestedLanguageTags, branch.getUuid(),
					ac.getVersioningParameters().getVersion());

			if (fieldContainer == null) {
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_branch_language", uuid,
						String.join(",", requestedLanguageTags), branch.getUuid());
			}
			// Additionally check whether the read published permission could grant read
			// perm for published nodes
			boolean isPublished = fieldContainer.isPublished(branch.getUuid());
			if (isPublished && requestUser.hasPermission(element, READ_PUBLISHED_PERM)) {
				return element;
				// The container could be a draft. Check whether READ perm is granted.
			} else if (!isPublished && requestUser.hasPermission(element, READ_PERM)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", uuid, READ_PUBLISHED_PERM.getRestPerm().getName());
			}
		} else if (requestUser.hasPermission(element, perm)) {
			return element;
		}
		throw error(FORBIDDEN, "error_missing_perm", uuid, perm.getRestPerm().getName());
	}

	/**
	 * Get the vertex traversal that finds all nodes visible to the user
	 * 
	 * @param requestUser user
	 * @param branch      branch
	 * @param type        type
	 * @param permission  permission to filter by
	 * @return vertex traversal
	 */
	protected VertexTraversal<?, ?, ?> getAllTraversal(MeshAuthUser requestUser, Branch branch, ContainerType type,
			GraphPermission permission) {
		return out(getRootLabel()).filter(vertex -> {
			return requestUser.hasPermissionForId(vertex.getId(), permission);
		}).mark().outE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branch.getUuid())
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

		// TODO is this a duplicate? - Maybe we should only store the project assignment
		// in one way?
		project.getNodeRoot().addNode(node);
		node.setProject(project);
		node.setCreator(creator);
		node.setCreationTimestamp();

		addNode(node);
		return node;
	}

	@Override
	public void delete(BulkActionContext bac) {
		// TODO maybe add a check to prevent deletion of meshRoot.nodeRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting node root {" + getUuid() + "}");
		}
		// Delete all containers of all nodes
		for (Node node : findAll()) {
			// We don't need to handle recursion because we delete the root sequentially
			node.delete(bac, true, false);
			bac.inc();
		}
		// All nodes are gone. Lets remove the node root element.
		getElement().remove();
		bac.inc();
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
	private Node createNode(InternalActionContext ac, SchemaContainerVersion schemaVersion, EventQueueBatch batch,
			String uuid) {
		Project project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		BootstrapInitializer boot = MeshInternal.get().boot();

		NodeCreateRequest requestModel = ac.fromJson(NodeCreateRequest.class);
		if (requestModel.getParentNode() == null || isEmpty(requestModel.getParentNode().getUuid())) {
			throw error(BAD_REQUEST, "node_missing_parentnode_field");
		}
		if (isEmpty(requestModel.getLanguage())) {
			throw error(BAD_REQUEST, "node_no_languagecode_specified");
		}

		// Load the parent node in order to create the node
		Node parentNode = project.getNodeRoot().loadObjectByUuid(ac, requestModel.getParentNode().getUuid(),
				CREATE_PERM);
		Branch branch = ac.getBranch();
		// BUG: Don't use the latest version. Use the version which is linked to the
		// branch!
		Node node = parentNode.create(requestUser, schemaVersion, project, branch, uuid);

		// Add initial permissions to the created node
		requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
		requestUser.addPermissionsOnRole(parentNode, READ_PUBLISHED_PERM, node, READ_PUBLISHED_PERM);
		requestUser.addPermissionsOnRole(parentNode, PUBLISH_PERM, node, PUBLISH_PERM);

		// Create the language specific graph field container for the node
		Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
		if (language == null) {
			throw error(BAD_REQUEST, "language_not_found", requestModel.getLanguage());
		}
		NodeGraphFieldContainer container = node.createGraphFieldContainer(language.getLanguageTag(), branch, requestUser);
		container.updateFieldsFromRest(ac, requestModel.getFields());

		batch.add(node.onCreated());
		batch.add(container.onCreated(branch.getUuid(), DRAFT));

		// Check for webroot input data consistency (PUT on webroot)
		String webrootSegment = ac.get("WEBROOT_SEGMENT_NAME");
		if (webrootSegment != null) {
			String current = container.getSegmentFieldValue();
			if (!webrootSegment.equals(current)) {
				throw error(BAD_REQUEST, "webroot_error_segment_field_mismatch", webrootSegment, current);
			}
		}

		if (requestModel.getTags() != null) {
			node.updateTags(ac, batch, requestModel.getTags());
		}

		return node;
	}

	@Override
	public Node create(InternalActionContext ac, EventQueueBatch batch, String uuid) {

		// Override any given version parameter. Creation is always scoped to drafts
		ac.getVersioningParameters().setVersion("draft");

		Project project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		Branch branch = ac.getBranch();

		String body = ac.getBodyAsString();

		// 1. Extract the schema information from the given JSON
		SchemaReferenceInfo schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
		boolean missingSchemaInfo = schemaInfo.getSchema() == null
				|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid())
						&& StringUtils.isEmpty(schemaInfo.getSchema().getName()));
		if (missingSchemaInfo) {
			throw error(BAD_REQUEST, "error_schema_parameter_missing");
		}

		if (!isEmpty(schemaInfo.getSchema().getUuid())) {
			// 2. Use schema reference by uuid first
			SchemaContainer schemaByUuid = project.getSchemaContainerRoot().loadObjectByUuid(ac,
					schemaInfo.getSchema().getUuid(), READ_PERM);
			SchemaContainerVersion schemaVersion = branch.findLatestSchemaVersion(schemaByUuid);
			if (schemaVersion == null) {
				throw error(BAD_REQUEST, "schema_error_schema_not_linked_to_branch", schemaByUuid.getName(), branch.getName(), project.getName());
			}
			return createNode(ac, schemaVersion, batch, uuid);
		}

		// 3. Or just schema reference by name
		if (!isEmpty(schemaInfo.getSchema().getName())) {
			SchemaContainer schemaByName = project.getSchemaContainerRoot()
					.findByName(schemaInfo.getSchema().getName());
			if (schemaByName != null) {
				String schemaName = schemaByName.getName();
				String schemaUuid = schemaByName.getUuid();
				if (requestUser.hasPermission(schemaByName, READ_PERM)) {
					SchemaContainerVersion schemaVersion = branch.findLatestSchemaVersion(schemaByName);
					if (schemaVersion == null) {
						throw error(BAD_REQUEST, "schema_error_schema_not_linked_to_branch", schemaByName.getName(), branch.getName(), project.getName());
					}
					return createNode(ac, schemaVersion, batch, uuid);
				} else {
					throw error(FORBIDDEN, "error_missing_perm", schemaUuid + "/" + schemaName, READ_PERM.getRestPerm().getName());
				}

			} else {
				throw error(NOT_FOUND, "schema_not_found", schemaInfo.getSchema().getName());
			}
		} else {
			throw error(BAD_REQUEST, "error_schema_parameter_missing");
		}
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive,
			Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (Node node : findAll()) {
				// We don't need to recursively handle the permissions for each node again since
				// this call will already affect all nodes.
				node.applyPermissions(batch, role, false, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

}
