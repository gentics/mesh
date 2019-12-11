package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.tx.Tx;
import com.gentics.madl.type.TypeHandler;
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
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see NodeRoot
 */
public class NodeRootImpl extends AbstractRootVertex<Node> implements NodeRoot {

	private static final Logger log = LoggerFactory.getLogger(NodeRootImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodeRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_NODE).withInOut().withOut());
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
	public TransformablePage<? extends Node> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		ContainerType type = ContainerType.forVersion(ac.getVersioningParameters().getVersion());
		return new DynamicTransformableStreamPageImpl<>(findAllStream(ac, type), pagingInfo);
	}

	@Override
	public TraversalResult<? extends Node> findAll() {
		Project project = getProject();
		return project.findNodes();
	}

	private Project getProject() {
		return in(HAS_NODE_ROOT, ProjectImpl.class).next();
	}

	@Override
	public Stream<? extends Node> findAllStream(InternalActionContext ac, GraphPermission perm) {
		MeshAuthUser user = ac.getUser();
		String branchUuid = ac.getBranch().getUuid();

		return findAll(ac.getProject().getUuid())
			.filter(item -> {
				boolean hasRead = user.hasPermissionForId(item.getId(), READ_PERM);
				if (hasRead) {
					return true;
				} else {
					// Check whether the node is published. In this case we need to check the read publish perm.
					boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
					if (isPublishedForBranch) {
						return user.hasPermissionForId(item.getId(), READ_PUBLISHED_PERM);
					}
				}
				return false;
			})
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	/**
	 * Finds all nodes of a project.
	 * @param projectUuid
	 * @return
	 */
	private Stream<Vertex> findAll(String projectUuid) {
		return toStream(db().getVertices(
			NodeImpl.class,
			new String[]{PROJECT_KEY_PROPERTY},
			new Object[]{projectUuid}
		));
	}

	private Stream<? extends Node> findAllStream(InternalActionContext ac, ContainerType type) {
		MeshAuthUser user = ac.getUser();
		FramedTransactionalGraph graph = Tx.get().getGraph();

		Branch branch = ac.getBranch();
		String branchUuid = branch.getUuid();

		return findAll(ac.getProject().getUuid()).filter(item -> {
			// Check whether the node has at least one content of the type in the selected branch - Otherwise the node should be skipped
			return GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, type);
		}).filter(item -> {
			boolean hasRead = user.hasPermissionForId(item.getId(), READ_PERM);
			if (hasRead) {
				return true;
			} else if (type == PUBLISHED) {
				// Check whether the node is published. In this case we need to check the read publish perm.
				boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
				if (isPublishedForBranch) {
					return user.hasPermissionForId(item.getId(), READ_PUBLISHED_PERM);
				}
			}
			return false;
		})
		.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	@Override
	public Node findByUuid(String uuid) {
		return getProject().findNode(uuid);
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

			List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList(options());
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
		node.setProject(project);
		node.setCreator(creator);
		node.setCreationTimestamp();

		return node;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

	/**
	 * Create a new node using the specified schema container.
	 * 
	 * @param ac
	 * @param schemaVersion
	 * @param batch
	 * @param uuid
	 * @return
	 */
	// TODO use schema container version instead of container
	private Node createNode(InternalActionContext ac, SchemaContainerVersion schemaVersion, EventQueueBatch batch,
			String uuid) {
		Project project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		BootstrapInitializer boot = mesh().boot();

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
		requestUser.inheritRolePermissions(parentNode, node);

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

		applyVertexPermissions(batch, role, permissionsToGrant, permissionsToRevoke);
	}

}
