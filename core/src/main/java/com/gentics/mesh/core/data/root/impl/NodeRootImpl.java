package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.util.VerticleHelper.getProject;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.*;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReferenceInfo;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class NodeRootImpl extends AbstractRootVertex<Node>implements NodeRoot {

	private static final Logger log = LoggerFactory.getLogger(NodeRootImpl.class);

	@Override
	protected Class<? extends Node> getPersistanceClass() {
		return NodeImpl.class;
	}

	@Override
	protected String getRootLabel() {
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
	public Page<? extends Node> findAll(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(NodeImpl.class);
		Page<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	@Override
	public Node create(User creator, SchemaContainer container, Project project) {
		// TODO check whether the mesh node is in fact a folder node.
		NodeImpl node = getGraph().addFramedVertex(NodeImpl.class);
		node.setSchemaContainer(container);

		// TODO is this a duplicate? - Maybe we should only store the project assignment in one way?
		project.getNodeRoot().addNode(node);
		node.setProject(project);
		node.setCreator(creator);
		node.setCreationTimestamp(System.currentTimeMillis());
		node.setEditor(creator);
		node.setLastEditedTimestamp(System.currentTimeMillis());

		addNode(node);
		return node;
	}

	@Override
	public void delete() {
		// TODO maybe add a check to prevent deletion of meshRoot.nodeRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting node root {" + getUuid() + "}");
		}
		for (Node node : findAll()) {
			node.delete();
		}
		getElement().remove();
	}

	@Override
	public void create(RoutingContext rc, Handler<AsyncResult<Node>> handler) {

		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		I18NService i18n = I18NService.getI18n();
		ServerSchemaStorage schemaStorage = ServerSchemaStorage.getSchemaStorage();

		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);

			String body = rc.getBodyAsString();

			// 1. Extract the schema information from the given json
			SchemaReferenceInfo schemaInfo;
			try {
				schemaInfo = JsonUtil.readValue(body, SchemaReferenceInfo.class);
			} catch (Exception e) {
				handler.handle(Future.failedFuture(e));
				return;
			}
			boolean missingSchemaInfo = schemaInfo.getSchema() == null
					|| (StringUtils.isEmpty(schemaInfo.getSchema().getUuid()) && StringUtils.isEmpty(schemaInfo.getSchema().getName()));
			if (missingSchemaInfo) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_schema_parameter_missing"))));
				return;
			}

			Handler<AsyncResult<SchemaContainer>> containerFoundHandler = rh -> {
				SchemaContainer schemaContainer = rh.result();
				try {
					Schema schema = schemaContainer.getSchema();
					NodeCreateRequest requestModel = JsonUtil.readNode(body, NodeCreateRequest.class, schemaStorage);
					if (StringUtils.isEmpty(requestModel.getParentNodeUuid())) {
						handler.handle(
								Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_missing_parentnode_field"))));
						return;
					}
					if (StringUtils.isEmpty(requestModel.getLanguage())) {
						handler.handle(
								Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "node_no_languagecode_specified"))));
						return;
					}

					Node node;
					try (Trx txCreate = new Trx(db)) {
						requestUser.reload();
						project.reload();
						// Load the parent node in order to create the node
						Node parentNode = loadObjectByUuidBlocking(rc, requestModel.getParentNodeUuid(), CREATE_PERM, project.getNodeRoot());
						node = parentNode.create(requestUser, schemaContainer, project);
						requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
						node.setPublished(requestModel.isPublished());
						Language language = boot.languageRoot().findByLanguageTag(requestModel.getLanguage());
						if (language == null) {
							handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST,
									i18n.get(rc, "node_no_language_found", requestModel.getLanguage()))));
							txCreate.failure();
							return;
						}
						try {
							NodeFieldContainer container = node.getOrCreateFieldContainer(language);
							container.setFieldFromRest(rc, requestModel.getFields(), schema);
						} catch (Exception e) {
							handler.handle(Future.failedFuture(e));
							txCreate.failure();
							return;
						}
						txCreate.success();
					}
					handler.handle(Future.succeededFuture(node));
				} catch (Exception e) {
					handler.handle(Future.failedFuture(e));
					return;
				}
			};

			if (!StringUtils.isEmpty(schemaInfo.getSchema().getName())) {
				SchemaContainer containerByName = project.getSchemaContainerRoot().findByName(schemaInfo.getSchema().getName());
				if (containerByName != null) {
					if (requestUser.hasPermission(containerByName, READ_PERM)) {
						containerFoundHandler.handle(Future.succeededFuture(containerByName));
					} else {
						handler.handle(
								Future.failedFuture(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", containerByName.getUuid()))));
						return;
					}
				} else {
					handler.handle(
							Future.failedFuture(new EntityNotFoundException(i18n.get(rc, "schema_not_found", schemaInfo.getSchema().getName()))));
					return;
				}
			} else {
				loadObjectByUuid(rc, schemaInfo.getSchema().getUuid(), READ_PERM, project.getSchemaContainerRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						SchemaContainer schemaContainer = rh.result();
						containerFoundHandler.handle(Future.succeededFuture(schemaContainer));
						return;
					}
				});
			}
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
