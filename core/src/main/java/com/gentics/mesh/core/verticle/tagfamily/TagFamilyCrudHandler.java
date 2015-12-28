package com.gentics.mesh.core.verticle.tagfamily;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;

import rx.Observable;

@Component
public class TagFamilyCrudHandler extends AbstractCrudHandler<TagFamily, TagFamilyResponse> {

	@Override
	public RootVertex<TagFamily> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getTagFamilyRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "tagfamily_deleted");
	}

	public void handleReadTagList(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			Project project = ac.getProject();
			MeshAuthUser requestUser = ac.getUser();
			PagingParameter pagingInfo = ac.getPagingParameter();

			// TODO this is not checking for the project name and project relationship. We _need_ to fix this!
			return project.getTagFamilyRoot().loadObject(ac, "tagFamilyUuid", READ_PERM).flatMap(tagFamily -> {
				try {
					PageImpl<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
					return tagPage.transformToRest(ac);
				} catch (Exception e) {
					return Observable.error(e);
				}
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}
}
