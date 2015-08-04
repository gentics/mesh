package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.util.BlueprintTransaction;

@Component
public class TagFamiylCRUDHandler extends AbstractCRUDHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		Project project = getProject(rc);
		MeshAuthUser requestUser = getUser(rc);
		TagFamilyCreateRequest requestModel = fromJson(rc, TagFamilyCreateRequest.class);

		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set")));
		} else {
			TagFamilyRoot root = project.getTagFamilyRoot();
			/* TODO check for null */
			if (requestUser.hasPermission(root, CREATE_PERM)) {
				TagFamily tagFamily = null;
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					tagFamily = root.create(requestModel.getName(), requestUser);
					root.addTagFamily(tagFamily);
					requestUser.addCRUDPermissionOnRole(root, CREATE_PERM, tagFamily);
					tx.success();
				}
				transformAndResponde(rc, tagFamily);
			} else {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", root.getUuid())));
			}
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		delete(rc, "uuid", "tagfamily_deleted", getProject(rc).getTagFamilyRoot());
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		Project project = getProject(rc);
		TagFamilyUpdateRequest requestModel = fromJson(rc, TagFamilyUpdateRequest.class);

		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set")));
		} else {
			loadObject(rc, "uuid", UPDATE_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						TagFamily tagFamily = rh.result();
						tagFamily.setName(requestModel.getName());
						tx.success();
						transformAndResponde(rc, tagFamily);
					}
				}
			});
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		loadTransformAndResponde(rc, "uuid", READ_PERM, getProject(rc).getTagFamilyRoot());
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		Project project = getProject(rc);
		loadTransformAndResponde(rc, project.getTagFamilyRoot(), new TagFamilyListResponse());
	}
}
