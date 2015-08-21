package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.triggerEvent;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

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
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.ext.web.RoutingContext;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler {

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
		try (Trx tx = new Trx(db)) {
			if (requestUser.hasPermission(boot.projectRoot(), CREATE_PERM)) {
				if (boot.projectRoot().findByName(requestModel.getName()) != null) {
					rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "project_conflicting_name")));
				} else {
					ProjectRoot projectRoot = boot.projectRoot();
					Project project = projectRoot.create(rc, requestModel, requestUser);
					transformAndResponde(rc, project);
					//Inform elasticsearch about the new element
					triggerEvent(project.getUuid(), Project.TYPE, SearchQueueEntryAction.CREATE_ACTION);
				}
			} else {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", boot.projectRoot().getUuid())));
			}
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			deleteObject(rc, "uuid", "project_deleted", boot.projectRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			updateObject(rc, "uuid", boot.projectRoot());
		}

	}

	@Override
	public void handleRead(RoutingContext rc) {
		String uuid = rc.request().params().get("uuid");
		if (StringUtils.isEmpty(uuid)) {
			rc.next();
		} else {
			try (Trx tx = new Trx(db)) {
				loadTransformAndResponde(rc, "uuid", READ_PERM, boot.projectRoot());
			}
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, boot.projectRoot(), new ProjectListResponse());
		}
	}
}
