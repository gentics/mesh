package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

@Component
public class TagCrudHandler extends AbstractCrudHandler<Tag, TagResponse> {

	private static final Logger log = LoggerFactory.getLogger(TagCrudHandler.class);

	@Override
	public RootVertex<Tag> getRootVertex(InternalActionContext ac) {

		Object obj = ac.get(TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY);
		if (obj != null && obj instanceof TagFamily) {
			return ((TagFamily) obj).getTagRoot();
		} else {
			log.error("Could not locate root vertex while handling request.", ac.query());
			throw error(INTERNAL_SERVER_ERROR, "The tag family of the tag could not be located");
		}
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "tag_deleted");
	}

	/**
	 * Add the handler that returns a node list for a specified tag.
	 * 
	 * @param ac
	 */
	public void handleTaggedNodesList(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObject(ac, "uuid", READ_PERM).flatMap(tag -> {
				try {
					PageImpl<? extends Node> page = tag.findTaggedNodes(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
					return page.transformToRest(ac);
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	public void handleReadTagList(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
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
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

}
