package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndRespond;
import static com.gentics.mesh.util.VerticleHelper.transformAndRespond;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class TagCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> boot.tagRoot());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> ac.getProject().getTagRoot(), "uuid", "tag_deleted");
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> ac.getProject().getTagRoot());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		Project project = ac.getProject();
		readElementList(ac, () -> project.getTagRoot());
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			loadTransformAndRespond(ac, "uuid", READ_PERM, project.getTagRoot(), OK);
		} , ac.errorHandler());
	}

	/**
	 * Add the handler that returns a node list for a specified tag.
	 * 
	 * @param ac
	 */
	public void handleTaggedNodesList(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
			Project project = ac.getProject();
			project.getTagRoot().loadObject(ac, "uuid", READ_PERM, rh -> {
				if (hasSucceeded(ac, rh)) {
					Tag tag = rh.result();
					Page<? extends Node> page;
					try {
						page = tag.findTaggedNodes(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
						page.transformAndRespond(ac, OK);
					} catch (Exception e) {
						// TODO i18n - exception handling
						ac.fail(BAD_REQUEST, "Could not load nodes");
					}
				}
			});
		} , ac.errorHandler());
	}

}
