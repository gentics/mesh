package com.gentics.mesh.core.verticle.tagfamily;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;

@Component
public class TagFamilyCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			createObject(ac, ac.getProject().getTagFamilyRoot());
		}
	}

	@Override
	public void handleDelete(ActionContext ac) {
		try (Trx tx = db.trx()) {
			deleteObject(ac, "uuid", "tagfamily_deleted", ac.getProject().getTagFamilyRoot());
		}
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		try (Trx tx = db.trx()) {
			updateObject(ac, "uuid", ac.getProject().getTagFamilyRoot());
		}
	}

	@Override
	public void handleRead(ActionContext ac) {
		try (Trx tx = db.trx()) {
			loadTransformAndResponde(ac, "uuid", READ_PERM, ac.getProject().getTagFamilyRoot());
		}
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, project.getTagFamilyRoot(), new TagFamilyListResponse());
		}
	}

	public void handleReadTagList(ActionContext ac) {
		try (Trx tx = db.trx()) {
			Project project = ac.getProject();
			MeshAuthUser requestUser = ac.getUser();
			PagingInfo pagingInfo = ac.getPagingInfo();

			// TODO this is not checking for the project name and project relationship. We _need_ to fix this!
			loadObject(ac, "tagFamilyUuid", READ_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					TagFamily tagFamily = rh.result();
					try {
						Page<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
						transformAndResponde(ac, tagPage, new TagListResponse());
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		}
	}
}
