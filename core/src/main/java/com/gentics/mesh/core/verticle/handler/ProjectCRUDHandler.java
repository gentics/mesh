package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.util.BlueprintTransaction;

import io.vertx.ext.web.RoutingContext;

@Component
public class ProjectCRUDHandler extends AbstractCRUDHandler {

	private static final Logger log = LoggerFactory.getLogger(ProjectVerticle.class);

	@Override
	public void handleCreate(RoutingContext rc) {
		// TODO also create a default object schema for the project. Move this into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(, name)
		ProjectCreateRequest requestModel = fromJson(rc, ProjectCreateRequest.class);
		MeshAuthUser requestUser = getUser(rc);

		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "project_missing_name")));
			return;
		}

		if (requestUser.hasPermission(boot.projectRoot(), CREATE_PERM)) {
			if (boot.projectRoot().findByName(requestModel.getName()) != null) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "project_conflicting_name")));
			} else {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					ProjectRoot projectRoot = boot.projectRoot();
					Project project = projectRoot.create(requestModel.getName(), requestUser);
					project.setCreator(requestUser);
					try {
						routerStorage.addProjectRouter(project.getName());
						if (log.isInfoEnabled()) {
							log.info("Registered project {" + project.getName() + "}");
						}
						requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project);
						requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getBaseNode());
						requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getTagFamilyRoot());
						requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getTagRoot());
						requestUser.addCRUDPermissionOnRole(meshRoot, CREATE_PERM, project.getNodeRoot());
						//Inform elasticsearch about the new element
						searchQueue.put(project.getUuid(), Project.TYPE, SearchQueueEntryAction.CREATE_ACTION);
						vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
						tx.success();
						transformAndResponde(rc, project);

					} catch (Exception e) {
						// TODO should we really fail here?
						rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "Error while adding project to router storage"), e));
						tx.failure();
						return;
					}
				}
			}
		} else {
			rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", boot.projectRoot().getUuid())));
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			delete(rc, "uuid", "project_deleted", boot.projectRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		loadObject(rc, "uuid", UPDATE_PERM, boot.projectRoot(), rh -> {
			if (hasSucceeded(rc, rh)) {

				Project project = rh.result();
				ProjectUpdateRequest requestModel = fromJson(rc, ProjectUpdateRequest.class);

				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					project.fillUpdateFromRest(rc, requestModel);
					tx.success();
				}

				searchQueue.put(project.getUuid(), Project.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
				vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
				transformAndResponde(rc, project);

			}
		});
	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.projectRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			loadTransformAndResponde(rc, boot.projectRoot(), new ProjectListResponse());
		}
	}
}
