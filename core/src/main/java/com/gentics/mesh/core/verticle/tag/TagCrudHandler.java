package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.NonTrx;
import com.gentics.mesh.handler.ActionContext;

@Component
public class TagCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			createObject(ac, boot.tagRoot());
		}
	}

	@Override
	public void handleDelete(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			deleteObject(ac, "uuid", "tag_deleted", ac.getProject().getTagRoot());
		}
	}

	@Override
	public void handleUpdate(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			updateObject(ac, "uuid", ac.getProject().getTagRoot());
		}
	}

	@Override
	public void handleRead(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, "uuid", READ_PERM, project.getTagRoot());
		}
	}

	@Override
	public void handleReadList(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			Project project = ac.getProject();
			loadTransformAndResponde(ac, project.getTagRoot(), new TagListResponse());
		}
	}

	public void handleTaggedNodesList(ActionContext ac) {
		try (NonTrx tx = db.nonTrx()) {
			Project project = ac.getProject();
			loadObject(ac, "uuid", READ_PERM, project.getTagRoot(), rh -> {
				if (hasSucceeded(ac, rh)) {
					Tag tag = rh.result();
					Page<? extends Node> page;
					try {
						page = tag.findTaggedNodes(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingInfo());
						transformAndResponde(ac, page, new NodeListResponse());
					} catch (Exception e) {
						//TODO i18n - exception handling
						ac.fail(BAD_REQUEST, "Could not load nodes");
					}
				}
			});
		}
	}

}
