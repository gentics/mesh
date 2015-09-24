package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ProjectRootImpl extends AbstractRootVertex<Project>implements ProjectRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectRootImpl.class);

	@Override
	protected Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_PROJECT;
	}

	@Override
	public void addProject(Project project) {
		addItem(project);
	}

	@Override
	public void removeProject(Project project) {
		removeItem(project);
	}

	// TODO unique

	@Override
	public Project create(String name, User creator) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getNodeRoot();
		project.createBaseNode(creator);

		project.setCreator(creator);
		project.setCreationTimestamp(System.currentTimeMillis());
		project.setEditor(creator);
		project.setLastEditedTimestamp(System.currentTimeMillis());

		project.getTagRoot();
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		addItem(project);

		return project;
	}

	@Override
	public void resolveToElement(Stack<String> stack, Handler<AsyncResult<? extends MeshVertex>> resultHandler) {
		if (stack.isEmpty()) {
			resultHandler.handle(Future.succeededFuture(this));
		} else {
			String uuidSegment = stack.pop();
			findByUuid(uuidSegment, rh -> {
				if (rh.succeeded()) {
					Project project = rh.result();
					if (stack.isEmpty()) {
						resultHandler.handle(Future.succeededFuture(project));
					} else {
						String nestedRootNode = stack.pop();
						switch (nestedRootNode) {
						case TagFamilyRoot.TYPE:
							TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
							tagFamilyRoot.resolveToElement(stack, resultHandler);
							break;
						case SchemaContainerRoot.TYPE:
							SchemaContainerRoot schemaRoot = project.getSchemaContainerRoot();
							schemaRoot.resolveToElement(stack, resultHandler);
							break;
						case MicroschemaContainerRoot.TYPE:
							// MicroschemaContainerRoot microschemaRoot = project.get
							// project.getMicroschemaRoot();
							throw new NotImplementedException();
							// break;
						case NodeRoot.TYPE:
							NodeRoot nodeRoot = project.getNodeRoot();
							nodeRoot.resolveToElement(stack, resultHandler);
							break;
						case TagRoot.TYPE:
							TagRoot tagRoot = project.getTagRoot();
							tagRoot.resolveToElement(stack, resultHandler);
							return;
						default:
							resultHandler.handle(Future.failedFuture("Unknown project element {" + nestedRootNode + "}"));
							return;
						}
					}
				} else {
					resultHandler.handle(Future.failedFuture(rh.cause()));
					return;
				}
			});
		}
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The project root should never be deleted.");
	}

	@Override
	public void create(InternalActionContext ac, Handler<AsyncResult<Project>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		RouterStorage routerStorage = RouterStorage.getRouterStorage();
		MeshRoot meshRoot = BootstrapInitializer.getBoot().meshRoot();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		// TODO also create a default object schema for the project. Move this into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(, name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		MeshAuthUser requestUser = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("project_missing_name"))));
			return;
		}
		try (Trx tx = db.trx()) {
			if (requestUser.hasPermission(ac, boot.projectRoot(), CREATE_PERM)) {
				if (boot.projectRoot().findByName(requestModel.getName()) != null) {
					handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, ac.i18n("project_conflicting_name"))));
					return;
				} else {
					Project project;
					SearchQueueBatch batch = null;
					try (Trx txCreate = db.trx()) {
						requestUser.reload();
						project = create(requestModel.getName(), requestUser);
						project.setCreator(requestUser);
						try {
							routerStorage.addProjectRouter(project.getName());
							if (log.isInfoEnabled()) {
								log.info("Registered project {" + project.getName() + "}");
							}
							requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project);
							requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getBaseNode());
							requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getTagFamilyRoot());
							requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getSchemaContainerRoot());
							//TODO add microschema root crud perms
							requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getTagRoot());
							requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getNodeRoot());

							batch = project.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
							txCreate.success();
						} catch (Exception e) {
							// TODO should we really fail here?
							txCreate.failure();
							handler.handle(ac.failedFuture(BAD_REQUEST, "Error while adding project to router storage", e));
							return;
						}
					}
					processOrFail(ac, batch, handler, project);
				}
			} else {
				handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", boot.projectRoot().getUuid()))));
				return;
			}
		}

	}

}
