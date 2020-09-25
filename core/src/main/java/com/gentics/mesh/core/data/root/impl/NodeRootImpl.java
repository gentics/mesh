package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.PROJECT_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
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
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformableStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
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
	public Result<? extends Node> findAll() {
		Project project = getProject();
		return project.findNodes();
	}

	private Project getProject() {
		return in(HAS_NODE_ROOT, ProjectImpl.class).next();
	}

	@Override
	public long globalCount() {
		return db().count(NodeImpl.class);
	}

	@Override
	public Stream<? extends Node> findAllStream(InternalActionContext ac, InternalPermission perm) {
		MeshAuthUser user = ac.getUser();
		String branchUuid = ac.getBranch().getUuid();
		UserDaoWrapper userDao = mesh().boot().userDao();

		return findAll(ac.getProject().getUuid())
			.filter(item -> {
				boolean hasRead = userDao.hasPermissionForId(user, item.getId(), READ_PERM);
				if (hasRead) {
					return true;
				} else {
					// Check whether the node is published. In this case we need to check the read publish perm.
					boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
					if (isPublishedForBranch) {
						return userDao.hasPermissionForId(user, item.getId(), READ_PUBLISHED_PERM);
					}
				}
				return false;
			})
			.map(vertex -> graph.frameElementExplicit(vertex, getPersistanceClass()));
	}

	/**
	 * Finds all nodes of a project.
	 * 
	 * @param projectUuid
	 * @return
	 */
	private Stream<Vertex> findAll(String projectUuid) {
		return toStream(db().getVertices(
			NodeImpl.class,
			new String[] { PROJECT_KEY_PROPERTY },
			new Object[] { projectUuid }));
	}

	private Stream<? extends Node> findAllStream(InternalActionContext ac, ContainerType type) {
		MeshAuthUser user = ac.getUser();
		FramedTransactionalGraph graph = Tx.get().getGraph();

		HibBranch branch = ac.getBranch();
		String branchUuid = branch.getUuid();
		UserDaoWrapper userDao = mesh().boot().userDao();

		return findAll(ac.getProject().getUuid()).filter(item -> {
			// Check whether the node has at least one content of the type in the selected branch - Otherwise the node should be skipped
			return GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, type);
		}).filter(item -> {
			boolean hasRead = userDao.hasPermissionForId(user, item.getId(), READ_PERM);
			if (hasRead) {
				return true;
			} else if (type == PUBLISHED) {
				// Check whether the node is published. In this case we need to check the read publish perm.
				boolean isPublishedForBranch = GraphFieldContainerEdgeImpl.matchesBranchAndType(item.getId(), branchUuid, PUBLISHED);
				if (isPublishedForBranch) {
					return userDao.hasPermissionForId(user, item.getId(), READ_PUBLISHED_PERM);
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
	public Node loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		Node element = findByUuid(uuid);
		if (!errorIfNotFound && element == null) {
			return null;
		}
		UserDaoWrapper userDao = mesh().boot().userDao();
		ContentDaoWrapper contentDao = mesh().boot().contentDao();
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		MeshAuthUser requestUser = ac.getUser();
		if (perm == READ_PUBLISHED_PERM) {
			HibBranch branch = ac.getBranch(element.getProject());

			List<String> requestedLanguageTags = ac.getNodeParameters().getLanguageList(options());
			NodeGraphFieldContainer fieldContainer = contentDao.findVersion(element, requestedLanguageTags, branch.getUuid(), ac.getVersioningParameters().getVersion());

			if (fieldContainer == null) {
				throw error(NOT_FOUND, "node_error_published_not_found_for_uuid_branch_language", uuid,
					String.join(",", requestedLanguageTags), branch.getUuid());
			}
			// Additionally check whether the read published permission could grant read
			// perm for published nodes
			boolean isPublished = fieldContainer.isPublished(branch.getUuid());
			if (isPublished && userDao.hasPermission(requestUser, element, READ_PUBLISHED_PERM)) {
				return element;
				// The container could be a draft. Check whether READ perm is granted.
			} else if (!isPublished && userDao.hasPermission(requestUser, element, READ_PERM)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", uuid, READ_PUBLISHED_PERM.getRestPerm().getName());
			}
		} else if (userDao.hasPermission(requestUser, element, perm)) {
			return element;
		}
		throw error(FORBIDDEN, "error_missing_perm", uuid, perm.getRestPerm().getName());
	}

	@Override
	public Node create(HibUser creator, HibSchemaVersion version, HibProject project, String uuid) {
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
		node.generateBucketId();

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
	private Node createNode(InternalActionContext ac, HibSchemaVersion schemaVersion, EventQueueBatch batch,
		String uuid) {
		HibProject project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		BootstrapInitializer boot = mesh().boot();
		UserDaoWrapper userRoot = boot.userDao();
		NodeDaoWrapper nodeDao = boot.nodeDao();

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
		HibBranch branch = ac.getBranch();
		// BUG: Don't use the latest version. Use the version which is linked to the
		// branch!
		HibNode node = nodeDao.create(parentNode, requestUser, schemaVersion, project, branch, uuid);

		// Add initial permissions to the created node
		userRoot.inheritRolePermissions(requestUser, parentNode, node);

		// Create the language specific graph field container for the node
		HibLanguage language = Tx.get().data().languageDao().findByLanguageTag(requestModel.getLanguage());
		if (language == null) {
			throw error(BAD_REQUEST, "language_not_found", requestModel.getLanguage());
		}
		NodeGraphFieldContainer container = toGraph(node).createGraphFieldContainer(language.getLanguageTag(), branch, requestUser);
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

		return toGraph(node);
	}

	@Override
	public Node create(InternalActionContext ac, EventQueueBatch batch, String uuid) {

		// Override any given version parameter. Creation is always scoped to drafts
		ac.getVersioningParameters().setVersion("draft");

		HibProject project = ac.getProject();
		MeshAuthUser requestUser = ac.getUser();
		HibBranch branch = ac.getBranch();
		UserDaoWrapper userDao = mesh().boot().userDao();

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
			HibSchema schemaByUuid = project.getSchemaContainerRoot().loadObjectByUuid(ac,
				schemaInfo.getSchema().getUuid(), READ_PERM);
			HibSchemaVersion schemaVersion = branch.findLatestSchemaVersion(schemaByUuid);
			if (schemaVersion == null) {
				throw error(BAD_REQUEST, "schema_error_schema_not_linked_to_branch", schemaByUuid.getName(), branch.getName(), project.getName());
			}
			return createNode(ac, schemaVersion, batch, uuid);
		}

		// 3. Or just schema reference by name
		if (!isEmpty(schemaInfo.getSchema().getName())) {
			Schema schemaByName = project.getSchemaContainerRoot()
				.findByName(schemaInfo.getSchema().getName());
			if (schemaByName != null) {
				String schemaName = schemaByName.getName();
				String schemaUuid = schemaByName.getUuid();
				if (userDao.hasPermission(requestUser, schemaByName, READ_PERM)) {
					HibSchemaVersion schemaVersion = branch.findLatestSchemaVersion(schemaByName);
					if (schemaVersion == null) {
						throw error(BAD_REQUEST, "schema_error_schema_not_linked_to_branch", schemaByName.getName(), branch.getName(),
							project.getName());
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
		Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
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
