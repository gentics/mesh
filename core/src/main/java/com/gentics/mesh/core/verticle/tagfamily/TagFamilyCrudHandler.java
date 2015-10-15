package com.gentics.mesh.core.verticle.tagfamily;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

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
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class TagFamilyCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			createObject(ac, ac.getProject().getTagFamilyRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			deleteObject(ac, "uuid", "tagfamily_deleted", ac.getProject().getTagFamilyRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			updateObject(ac, "uuid", ac.getProject().getTagFamilyRoot());
		} , ac.errorHandler());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			loadTransformAndResponde(ac, "uuid", READ_PERM, ac.getProject().getTagFamilyRoot(), OK);
		} , ac.errorHandler());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, project.getTagFamilyRoot(), new TagFamilyListResponse(), OK);
		} , ac.errorHandler());
	}

	public void handleReadTagList(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			Project project = ac.getProject();
			MeshAuthUser requestUser = ac.getUser();
			PagingInfo pagingInfo = ac.getPagingInfo();

			// TODO this is not checking for the project name and project relationship. We _need_ to fix this!
			loadObject(ac, "tagFamilyUuid", READ_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					TagFamily tagFamily = rh.result();
					try {
						Page<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
						transformAndResponde(ac, tagPage, new TagListResponse(), OK);
					} catch (Exception e) {
						ac.fail(e);
					}
				}
			});
		} , ac.errorHandler());
	}
}
