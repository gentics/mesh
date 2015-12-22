package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

@Component
public class TagCrudHandler extends AbstractCrudHandler<Tag, TagResponse> {

	@Override
	public RootVertex<Tag> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getTagRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> ac.getProject().getTagRoot(), "uuid", "tag_deleted");
	}

	/**
	 * Add the handler that returns a node list for a specified tag.
	 * 
	 * @param ac
	 */
	public void handleTaggedNodesList(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			return project.getTagRoot().loadObject(ac, "uuid", READ_PERM).flatMap(tag -> {
				try {
					Page<? extends Node> page = tag.findTaggedNodes(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
					return page.transformToRest(ac);
				} catch (Exception e) {
					return Observable.error(e);
				}
			}).toBlocking().last();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
